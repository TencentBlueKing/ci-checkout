package com.tencent.devops.scm

import com.tencent.devops.enums.CodeEventType
import com.tencent.devops.enums.GitPullModeType
import com.tencent.devops.enums.StartType
import com.tencent.devops.pojo.GitCodeAtomParam
import com.tencent.devops.utils.GitUtil
import com.tencent.devops.utils.shell.CommonShellUtils
import java.io.File
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.net.URL
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory

open class GitUpdateTask constructor(
    protected open val atomParam: GitCodeAtomParam,
    protected open val credentialSetter: GitCredentialSetter
) {

    protected var workspace = File("")

    fun perform(): Map<String, String>? {
        initWorkspace()
        preUpdate()
        GitUtil.deleteLock(workspace)
        checkLocalGitRepo()
        gitInit()
        setConfig()
        doCheckout()
        doUpdateSubmodule()
        return mapOf()
    }

    open fun preUpdate() {
    }

    private fun gitInit() {
        if (!File(workspace, ".git").exists()) {
            val url = atomParam.repositoryUrl
            println("Init the repo in $url")
            CommonShellUtils.execute("git init", workspace)
            CommonShellUtils.execute("git remote add origin ${credentialSetter.getCredentialUrl(url)}", workspace)
        }
        initSparseFile()
    }

    private fun initSparseFile() {
        // clear sparseFile file
        val content = StringBuilder()
        val sparseFile = File(workspace, ".git/info/sparse-checkout")

        if (!atomParam.excludePath.isNullOrBlank()) {
            content.append("/*").append(System.lineSeparator())
            atomParam.excludePath!!.split(",").forEach {
                content.append("!").append(it.trim()).append(System.lineSeparator())
            }
        }
        if (!atomParam.includePath.isNullOrBlank()) {
            atomParam.includePath!!.split(",").forEach {
                content.append("/").append(it.trim().removePrefix("/")).append(System.lineSeparator())
            }
        }
        println(".git/info/sparse-checkout content: $content")

        // 不填包含或者排除路径就不开启sparse checkout
        if (content.toString().isBlank()) {
            if (sparseFile.exists()) sparseFile.delete()
            CommonShellUtils.execute("git config core.sparsecheckout false", workspace)
        } else {
            if (!sparseFile.parentFile.exists()) sparseFile.parentFile.mkdirs()
            if (!sparseFile.exists()) sparseFile.createNewFile()
            sparseFile.writeText(content.toString())
            CommonShellUtils.execute("git config core.sparsecheckout true", workspace)
        }
    }

    private fun doCheckout() {
        gitFetch()
        with(atomParam) {
            if (enableVirtualMergeBranch && isSameProject(atomParam.repositoryUrl, hookTargetUrl) &&
                pipelineStartType == StartType.WEB_HOOK.name &&
                (hookEventType == CodeEventType.PULL_REQUEST.name || hookEventType == CodeEventType.MERGE_REQUEST.name)
            ) {
                logger.warn("The mode enable virtual merge branch")
                checkoutVirtualBranch()
            } else {
                checkout()
            }
        }
    }

    protected fun gitFetch() {
        val fetchOption = if (atomParam.fetchDepth != null) {
            "--depth=${atomParam.fetchDepth}"
        } else {
            ""
        }

        CommonShellUtils.execute("git fetch $fetchOption", workspace, failExit = true)
    }

    private fun isSameProject(url: String, targetUrl: String?): Boolean {
        return !targetUrl.isNullOrBlank() && GitUtil.getProjectName(url) == GitUtil.getProjectName(targetUrl!!)
    }

    private fun checkout() {
        when (atomParam.pullType) {
            GitPullModeType.BRANCH -> {
                checkoutBranch(atomParam.refName)
            }

            GitPullModeType.TAG -> {
                checkoutTag(atomParam.refName)
            }

            GitPullModeType.COMMIT_ID -> {
                checkoutCommitId(atomParam.refName)
            }
        }
    }

    private fun checkoutVirtualBranch() {
        CommonShellUtils.execute("git checkout ${atomParam.hookTargetBranch}", workspace)
        CommonShellUtils.execute("git pull", workspace)
        rmDevopsBranch()
        if (atomParam.hookSourceUrl != atomParam.hookTargetUrl) {
            mergeVirtualRemote()
        } else {
            mergeOriginRemote()
        }
        println("Merge branch ${atomParam.hookSourceBranch} succeed")
    }

    private fun mergeOriginRemote() {
        CommonShellUtils.execute("git remote -v", workspace)
        CommonShellUtils.execute("git branch devops-virtual-branch", workspace)
        CommonShellUtils.execute("git checkout devops-virtual-branch", workspace)
        CommonShellUtils.execute("git merge origin/${atomParam.hookSourceBranch}", workspace)
    }

    private fun mergeVirtualRemote() {
        try {
            rmDevopsOrigin()
            CommonShellUtils.execute("git remote add devops-virtual-origin ${getHookSourceUrl()}", workspace)
            CommonShellUtils.execute("git remote -v", workspace)
            CommonShellUtils.execute("git fetch devops-virtual-origin", workspace)
            CommonShellUtils.execute("git branch devops-virtual-branch", workspace)
            CommonShellUtils.execute("git checkout devops-virtual-branch", workspace)
            CommonShellUtils.execute("git merge devops-virtual-origin/${atomParam.hookSourceBranch}", workspace)
        } finally {
            rmDevopsOrigin()
        }
    }

    private fun getHookSourceUrl(): String {
        return credentialSetter.getCredentialUrl(atomParam.hookSourceUrl!!)
    }

    private fun rmDevopsBranch() {
        try {
            CommonShellUtils.execute("git branch -D devops-virtual-branch", workspace)
        } catch (e: Exception) {
        }
    }

    private fun rmDevopsOrigin() {
        try {
            CommonShellUtils.execute("git remote rm devops-virtual-origin", workspace)
        } catch (e: Exception) {
        }
    }

    private fun checkoutBranch(branch: String) {
        println("Checkout to branch $branch")
        CommonShellUtils.execute("git checkout $branch", workspace)
        CommonShellUtils.execute("git pull", workspace)
    }

    private fun checkoutTag(tag: String) {
        println("Checkout to tag $tag")
        CommonShellUtils.execute("git tag -d $tag", workspace)
        CommonShellUtils.execute("git fetch origin $tag", workspace)
        CommonShellUtils.execute("git checkout $tag", workspace)
    }

    private fun checkoutCommitId(commitId: String) {
        println("Checkout to revision $commitId")
        CommonShellUtils.execute("git checkout $commitId", workspace)
    }

    private fun doUpdateSubmodule() {
        if (atomParam.enableSubmodule) {
            println("Enable pull git submodule")
            updateSubmodule(workspace)
        } else {
            println("Disable pull git submodule")
        }
    }

    private fun updateSubmodule(workspace: File) {
        val submoduleConfigFile = File(workspace, ".gitmodules")
        if (submoduleConfigFile.exists()) {
            /**
             * The following logic is to replace the url in the .gitmodules which start with "http" with "ssh"
             * This is because we only support the ssh submodule
             */
            val modules = listSubmodules(workspace)
            var submoduleConfig = submoduleConfigFile.readText()

            modules.forEach { m ->
                println("load sub module ${m.path} - ${m.url}")
                submoduleConfig = submoduleConfig.replace(m.url, m.credentialUrl)
            }
            submoduleConfigFile.writeText(submoduleConfig)
            println("Updating the submodule")
            CommonShellUtils.execute("git submodule init", workspace)
            CommonShellUtils.execute(getSubmoduleCommand(), workspace)

            // update submodule' submodule iteratively
            modules.forEach { m ->
                val submoduleWorkspace = File(workspace, m.path)
                updateSubmodule(submoduleWorkspace)
            }
        }
    }

    private fun getSubmoduleCommand(): String {
        return if (atomParam.enableSubmoduleRemote) {
            "git submodule update --init --remote"
        } else {
            "git submodule update --init"
        }
    }

    private fun listSubmodules(workspace: File): List<Submodule> {
        val result = mutableListOf<Submodule>()
        val lines = File(workspace, ".gitmodules").readLines().filter { it.isNotBlank() }.map { it.trim() }

        var path = ""
        var url = ""
        var beginIndex = 0
        var endIndex = 0
        while (endIndex < lines.size) {
            while (beginIndex < lines.size && !lines[beginIndex].startsWith("[submodule ")) beginIndex++
            endIndex = beginIndex + 1
            while (endIndex < lines.size && !lines[endIndex].startsWith("[submodule ")) endIndex++
            println("begin=$beginIndex, end=$endIndex")
            for (i in beginIndex until endIndex) {
                val line = lines[i]
                if (line.startsWith("path =")) {
                    path = line.removePrefix("path = ")
                }
                if (line.startsWith("url =")) {
                    url = line.removePrefix("url = ")
                }
            }
            val submodule = getSubmodule(url, path)
            if (submodule != null) result.add(submodule)
            beginIndex = endIndex
            endIndex = beginIndex + 1
        }

        return result
    }

    private fun getSubmodule(url: String, path: String): Submodule? {
        val rootHost = getUrlHost(atomParam.repositoryUrl)
        if (url.isEmpty()) {
            logger.warn("The url is empty of submodule($url)")
            return null
        } else {

            val subHost = getUrlHost(url)
            if (rootHost != null &&
                subHost != null &&
                rootHost != subHost
            ) {
                return null
            }

            // convert submodule url
            if (!atomParam.repositoryUrl.startsWith("http")) {
                return if (url.startsWith("http")) {
                    try {
                        val u = URL(url)
                        val convert = "git@${u.host}:${u.path.removePrefix("/")}"
                        logger.warn("Convert the git submodule url from ($url) to ($convert)")
                        Submodule(path, url, convert)
                    } catch (e: Exception) {
                        logger.error("外链($url)不是一个正确的URL地址")
                        throw e
                    }
                } else {
                    Submodule(path, url, credentialSetter.getCredentialUrl(url))
                }
            } else {
                return if (url.startsWith("git@")) {
                    try {
                        val gitUrlHostAndPath = getGitUrlHostAndPath(url)
                        val convert = "http://${gitUrlHostAndPath.first}/${gitUrlHostAndPath.second}.git"
                        logger.warn("Convert the git submodule url from ($url) to ($convert)")
                        Submodule(path, url, credentialSetter.getCredentialUrl(convert))
                    } catch (e: Exception) {
                        logger.error("外链($url)不是一个正确的URL地址")
                        throw e
                    }
                } else {
                    Submodule(path, url, credentialSetter.getCredentialUrl(url))
                }
            }
        }
    }

    private fun getUrlHost(url: String): String? {
        try {
            val actualUrl = url.trim()
            if (actualUrl.startsWith("http") || actualUrl.startsWith("https")) return URL(actualUrl).host
            return actualUrl.substring("git@".length, actualUrl.indexOf(":"))
        } catch (e: Exception) {
            logger.warn("获取git代码库主机信息失败: $url")
        }
        return url
    }

    private fun getUrlHostAndPath(urlStr: String): Pair<String?, String?> {
        return when {
            urlStr.contains("http") || urlStr.contains("https") -> {
                val url = URL(urlStr)
                url.host to url.path.removeSuffix(".git")
            }
            urlStr.contains("git@") -> {
                getGitUrlHostAndPath(urlStr)
            }
            else -> throw IllegalArgumentException("urlStr:$urlStr parse error")
        }
    }

    private fun getGitUrlHostAndPath(urlStr: String): Pair<String?, String?> {
        val matches = Regex("""git@(.*):(.*).git""").find(urlStr)
        return matches!!.groupValues[1] to matches.groupValues[2]
    }

    fun checkLocalGitRepo() {
        if (File(workspace, ".git").exists()) {
            val localUrl = CommonShellUtils.execute("git config --get remote.origin.url", workspace, failExit = false).trim()
            if (localUrl.isBlank()) {
                println("Local url is blank")
                return
            }
            val localUrlObj = getUrlHostAndPath(localUrl)
            val url = getUrlHostAndPath(atomParam.repositoryUrl)
            if (localUrlObj.first != url.first && localUrlObj.second != url.second) {
                logger.warn("Git repo url 从($localUrl)变为($url), 全量拉取代码仓库")
                cleanupWorkspace()
                return
            }

            // 每次都重新设置凭证
            val credentialSetterUrl = credentialSetter.getCredentialUrl(atomParam.repositoryUrl)
            println("set url in workspace: ${workspace.canonicalPath} (${workspace.exists()})")
            CommonShellUtils.execute("git remote set-url origin $credentialSetterUrl", workspace, failExit = false)
        } else {
            println("The .git file is not exist: ${File(workspace, ".git").canonicalPath}")
        }
    }

    protected fun cleanupWorkspace() {
        println("Clean up the workspace(${workspace.path})")
        workspace.listFiles()?.forEach {
            println("delete the file: ${it.canonicalPath}")
            FileUtils.forceDelete(it)
        }
    }

    private fun initWorkspace(): File {
        workspace = if (atomParam.localPath.isNullOrBlank()) File(atomParam.bkWorkspace)
        else File(atomParam.bkWorkspace, atomParam.localPath!!)

        if (!workspace.exists()) {
            println("${workspace.canonicalPath} is not exist, creating...")
            workspace.mkdirs()
        }
        println("pull code into workspace ${workspace.canonicalPath}")
        return workspace
    }

    private fun setConfig() {
        CommonShellUtils.execute("git config user.name ${atomParam.pipelineStartUserName}", workspace)
        CommonShellUtils.execute("git config user.email ${atomParam.pipelineStartUserName}@tencent.com", workspace)
        CommonShellUtils.execute("git config core.autocrlf ${atomParam.enableAutoCrlf}", workspace)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GitUpdateTask::class.java)
    }

    data class Submodule(
        val path: String,
        val url: String,
        val credentialUrl: String
    )
}
