/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.devops.git.core.service

import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_GIT_VERSION
import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_REPOSITORY_URL
import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_BUILD_ID
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_PIPELINE_ID
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_PROJECT_NAME
import com.tencent.bk.devops.git.core.constant.GitConstants.GCM_INTERACTIVE
import com.tencent.bk.devops.git.core.constant.GitConstants.GIT_CREDENTIAL_HELPER
import com.tencent.bk.devops.git.core.constant.GitConstants.GIT_LFS_FORCE_PROGRESS
import com.tencent.bk.devops.git.core.constant.GitConstants.GIT_TERMINAL_PROMPT
import com.tencent.bk.devops.git.core.constant.GitConstants.HOME
import com.tencent.bk.devops.git.core.constant.GitConstants.SUPPORT_CHECKOUT_B_GIT_VERSION
import com.tencent.bk.devops.git.core.constant.GitConstants.SUPPORT_MERGE_NO_VERIFY_GIT_VERSION
import com.tencent.bk.devops.git.core.constant.GitConstants.SUPPORT_PARTIAL_CLONE_GIT_VERSION
import com.tencent.bk.devops.git.core.constant.GitConstants.SUPPORT_RECURSE_SUBMODULES_VERSION
import com.tencent.bk.devops.git.core.constant.GitConstants.SUPPORT_SET_UPSTREAM_TO_GIT_VERSION
import com.tencent.bk.devops.git.core.constant.GitConstants.SUPPORT_SUBMODULE_SYNC_RECURSIVE_GIT_VERSION
import com.tencent.bk.devops.git.core.constant.GitConstants.SUPPORT_SUBMODULE_UPDATE_FORCE_GIT_VERSION
import com.tencent.bk.devops.git.core.enums.CredentialActionEnum
import com.tencent.bk.devops.git.core.enums.FilterValueEnum
import com.tencent.bk.devops.git.core.enums.GitConfigScope
import com.tencent.bk.devops.git.core.enums.GitErrors
import com.tencent.bk.devops.git.core.enums.OSType
import com.tencent.bk.devops.git.core.exception.GitExecuteException
import com.tencent.bk.devops.git.core.pojo.CommitLogInfo
import com.tencent.bk.devops.git.core.pojo.CredentialArguments
import com.tencent.bk.devops.git.core.pojo.GitOutput
import com.tencent.bk.devops.git.core.service.helper.VersionHelper
import com.tencent.bk.devops.git.core.util.AgentEnv
import com.tencent.bk.devops.git.core.util.CommandUtil
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.FileUtils
import com.tencent.bk.devops.git.core.util.GitUtil
import com.tencent.bk.devops.git.core.util.RegexUtil
import com.tencent.devops.git.log.LogType
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.net.URI
import java.nio.file.Files

@Suppress("ALL")
class GitCommandManager(
    val workingDirectory: File,
    val lfs: Boolean = false
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GitCommandManager::class.java)
        private const val LONG_RETRY_PERIOD_MILLS = 10000L
    }

    private val gitEnv = mutableMapOf(
        GIT_TERMINAL_PROMPT to "0",
        GCM_INTERACTIVE to "Never"
    )
    private var gitVersion = 0L

    init {
        if (lfs) {
            gitEnv[GIT_LFS_FORCE_PROGRESS] = "1"
        }
    }

    fun getGitVersion(): String {
        val version = execGit(args = listOf("--version")).stdOut
        gitVersion = VersionHelper.computeGitVersion(version)
        val projectId = System.getenv(BK_CI_PROJECT_NAME)
        val pipelineId = System.getenv(BK_CI_PIPELINE_ID)
        val buildId = System.getenv(BK_CI_BUILD_ID)
        setEnvironmentVariable(
            GitConstants.GIT_HTTP_USER_AGENT,
            "git/$gitVersion (devops-$projectId-$pipelineId-$buildId)"
        )
        EnvHelper.putContext(CONTEXT_GIT_VERSION, "$gitVersion")
        return version
    }

    fun tryGetFetchUrl(): String {
        val output = execGit(
            args = listOf("config", "--get", "remote.origin.url"),
            allowAllExitCodes = true
        )
        if (output.exitCode != 0) {
            return ""
        }
        val stdout = output.stdOut.trim()
        if (stdout.contains("\n")) {
            return ""
        }
        return stdout
    }

    fun tryClean(enableGitCleanIgnore: Boolean?, enableGitCleanNested: Boolean?): Boolean {
        val args = mutableListOf("clean", "-fd")
        if (enableGitCleanIgnore == true) {
            args.add("-x")
        }
        if (enableGitCleanNested == true) {
            args.add("-f")
        }
        val output = execGit(args = args, allowAllExitCodes = true)
        return output.exitCode == 0
    }

    fun tryReset(commit: String? = null): Boolean {
        return tryReset(mode = "--hard", commit = commit)
    }

    fun tryReset(mode: String, commit: String? = null): Boolean {
        val args = mutableListOf("reset", mode)
        if (!commit.isNullOrBlank()) {
            args.add(commit)
        }
        val output = execGit(args = args, allowAllExitCodes = true, logType = LogType.PROGRESS)
        return output.exitCode == 0
    }

    fun init() {
        execGit(args = listOf("init", workingDirectory.canonicalPath))
    }

    fun remoteAdd(remoteName: String, remoteUrl: String) {
        execGit(args = listOf("remote", "add", remoteName, remoteUrl))
    }

    fun remoteSetUrl(remoteName: String, remoteUrl: String) {
        execGit(args = listOf("remote", "set-url", remoteName, remoteUrl))
    }

    fun remoteRemove(remoteName: String) {
        execGit(args = listOf("remote", "remove", remoteName), allowAllExitCodes = true)
    }

    fun remoteList() {
        execGit(args = listOf("remote", "-v"))
    }

    fun configExists(
        configKey: String,
        configValueRegex: String? = null,
        configScope: GitConfigScope = GitConfigScope.LOCAL,
        configFile: String? = null
    ): Boolean {
        val output = execGit(
            args = configArgs(
                configKey = configKey,
                configValue = configValueRegex,
                configScope = configScope,
                configFile = configFile,
                action = "--get"
            ),
            allowAllExitCodes = true
        )
        return output.exitCode == 0
    }

    fun tryConfigGet(
        configKey: String,
        configValueRegex: String? = null,
        configScope: GitConfigScope = GitConfigScope.LOCAL,
        configFile: String? = null
    ): String {
        val output = execGit(
            args = configArgs(
                configKey = configKey,
                configValue = configValueRegex,
                configScope = configScope,
                configFile = configFile,
                action = "--get"
            ),
            allowAllExitCodes = true
        )
        if (output.exitCode != 0) {
            return ""
        }
        return output.stdOut
    }

    fun tryConfigGetAll(
        configKey: String,
        configValueRegex: String? = null,
        configScope: GitConfigScope = GitConfigScope.LOCAL,
        configFile: String? = null
    ): List<String> {
        val output = execGit(
            args = configArgs(
                configKey = configKey,
                configValue = configValueRegex,
                configScope = configScope,
                configFile = configFile,
                action = "--get-all"
            ),
            allowAllExitCodes = true
        )
        if (output.exitCode != 0) {
            return emptyList()
        }
        return output.stdOuts
    }

    fun tryConfigGetRegexp(
        configKeyRegex: String,
        configValueRegex: String? = null,
        configScope: GitConfigScope = GitConfigScope.LOCAL
    ): List<String> {
        val output = execGit(
            args = configArgs(
                configKey = configKeyRegex,
                configValue = configValueRegex,
                configScope = configScope,
                action = "--get-regexp"
            ),
            allowAllExitCodes = true
        )
        if (output.exitCode != 0) {
            return emptyList()
        }
        return output.stdOuts
    }

    fun tryConfigUnset(
        configKey: String,
        configValueRegex: String? = null,
        configScope: GitConfigScope = GitConfigScope.LOCAL,
        configFile: String? = null
    ): Boolean {
        val output = execGit(
            args = configArgs(
                configKey = configKey,
                configValue = configValueRegex,
                configScope = configScope,
                configFile = configFile,
                action = "--unset-all"
            ),
            allowAllExitCodes = true
        )
        return output.exitCode == 0
    }

    fun config(
        configKey: String,
        configValue: String,
        configScope: GitConfigScope = GitConfigScope.LOCAL,
        configFile: String? = null
    ) {
        execGit(
            args = configArgs(
                configKey = configKey,
                configValue = configValue,
                configScope = configScope,
                configFile = configFile
            )
        )
    }

    fun configAdd(
        configKey: String,
        configValue: String,
        configScope: GitConfigScope = GitConfigScope.LOCAL,
        configFile: String? = null
    ) {
        execGit(
            args = configArgs(
                configKey = configKey,
                configValue = configValue,
                configScope = configScope,
                configFile = configFile,
                action = "--add"
            )
        )
    }

    private fun configArgs(
        configKey: String,
        configValue: String? = null,
        configScope: GitConfigScope = GitConfigScope.LOCAL,
        configFile: String? = null,
        action: String? = null
    ): List<String> {
        if (configScope == GitConfigScope.FILE && configFile.isNullOrBlank()) {
            throw IllegalArgumentException("config file can't be empty when config scope is file")
        }
        val args = mutableListOf("config")
        var scope = configScope
        /*
            #2 当构建机重启后，worker-agent自启动会导致HOME环境变量丢失,在执行全局配置时会报fatal: $HOME not set
            将全局环境变量变成本地,此时凭证无法全局传递，只能在当前仓库传递
         */
        if (isNotHomeEnv() && configScope == GitConfigScope.GLOBAL) {
            scope = GitConfigScope.LOCAL
        }
        // 低于git 1.9以下的版本没有--local参数,所以--local直接去掉
        if (scope != GitConfigScope.LOCAL) {
            args.add(configScope.arg)
        }
        if (!configFile.isNullOrBlank()) {
            args.add(configFile)
        }
        if (!action.isNullOrBlank()) {
            args.add(action)
        }
        args.add(configKey)
        if (!configValue.isNullOrBlank()) {
            args.add(configValue)
        }
        return args
    }

    private fun isNotHomeEnv(): Boolean {
        return AgentEnv.getOS() != OSType.WINDOWS && System.getenv(HOME) == null && gitEnv[HOME] == null
    }

    fun tryDisableOtherGitHelpers(
        configScope: GitConfigScope = GitConfigScope.LOCAL,
        configKey: String = GIT_CREDENTIAL_HELPER
    ) {
        CommandUtil.execute(
            command = "git config ${configScope.arg} $configKey \"\"",
            workingDirectory = workingDirectory,
            printLogger = true,
            allowAllExitCodes = true
        )
    }

    fun setEnvironmentVariable(name: String, value: String) {
        gitEnv[name] = value
    }

    fun removeEnvironmentVariable(name: String): String? {
        return gitEnv.remove(name)
    }

    fun getEnvironmentVariable(name: String): String? {
        return gitEnv[name]
    }

    fun submoduleSync(repoDir: File? = null, recursive: Boolean, path: String) {
        val args = mutableListOf("submodule", "sync")
        // git submodule sync --recursive在1.8.1才支持
        if (recursive && isAtLeastVersion(SUPPORT_SUBMODULE_SYNC_RECURSIVE_GIT_VERSION)) {
            args.add("--recursive")
        }
        if (path.isNotBlank()) {
            args.addAll(path.split(","))
        }
        execGit(repoDir = repoDir, args = args)
    }

    fun submoduleForeach(repoDir: File? = null, command: String, recursive: Boolean) {
        val args = mutableListOf("submodule", "foreach")
        if (recursive) {
            args.add("--recursive")
        }
        args.add(command)
        execGit(repoDir = repoDir, args = args, allowAllExitCodes = true, logType = LogType.PROGRESS)
    }

    fun submoduleUpdate(
        repoDir: File? = null,
        recursive: Boolean,
        path: String,
        submoduleRemote: Boolean,
        submoduleJobs: Int?,
        submoduleDepth: Int?
    ) {
        val args = mutableListOf("submodule", "update", "--init")
        if (isAtLeastVersion(SUPPORT_SUBMODULE_UPDATE_FORCE_GIT_VERSION)) {
            args.add("--force")
        }
        if (recursive) {
            args.add("--recursive")
        }
        if (submoduleJobs != null && submoduleJobs > 0) {
            args.addAll(listOf("--jobs", submoduleJobs.toString()))
        }
        if (submoduleDepth != null && submoduleDepth > 0) {
            args.addAll(listOf("--depth", submoduleDepth.toString()))
        }
        if (submoduleRemote) {
            args.add("--remote")
        }
        if (path.isNotBlank()) {
            args.addAll(path.split(","))
        }
        doRetry(repoDir = repoDir, args = args)
    }

    fun fetch(
        refSpec: List<String>,
        fetchDepth: Int = 0,
        remoteName: String,
        shallowSince: String? = null,
        enablePartialClone: Boolean? = false,
        prune: Boolean = true
    ) {
        val args = mutableListOf("fetch", "--progress")
        if (prune) {
            args.add("--prune")
        }
        if (isAtLeastVersion(SUPPORT_RECURSE_SUBMODULES_VERSION)) {
            args.add("--no-recurse-submodules")
        }
        /**
         * 如果git版本大于2.20.0,并且开启部分克隆，则忽略浅克隆
         */
        when {
            enablePartialClone == true && isAtLeastVersion(SUPPORT_PARTIAL_CLONE_GIT_VERSION) ->
                args.add("--filter=${FilterValueEnum.TREELESS.value}")
            !shallowSince.isNullOrBlank() -> {
                // --shallow-since 参数只有在git protocol.version=2时才生效
                args.add(0, "-c")
                args.add(1, "protocol.version=2")
                args.add("--shallow-since=$shallowSince")
            }
            fetchDepth > 0 ->
                args.add("--depth=$fetchDepth")
            File(File(workingDirectory, ".git"), "shallow").exists() ->
                args.add("--unshallow")
        }

        args.add(remoteName)
        args.addAll(refSpec)
        doRetry(args = args)
    }

    fun lfsPull(
        fetchInclude: String?,
        fetchExclude: String?
    ) {
        val args = mutableListOf("lfs", "pull")
        if (!fetchInclude.isNullOrBlank()) {
            args.addAll(listOf("-I", fetchInclude))
        }
        if (!fetchExclude.isNullOrBlank()) {
            args.addAll(listOf("-X", fetchExclude))
        }
        doRetry(args = args)
    }

    private fun doRetry(
        repoDir: File? = null,
        args: List<String>,
        retryTime: Int = 3
    ) {
        try {
            execGit(repoDir = repoDir, args = args, logType = LogType.PROGRESS)
        } catch (e: GitExecuteException) {
            if (retryTime - 1 < 0) {
                throw e
            }
            // 第一次重试先卸载oauth2凭证,然后再重试
            if (retryTime == 3 && e.errorCode == GitErrors.RepositoryNotFoundFailed.errorCode) {
                eraseOauth2Credential()
            }
            if (needRetry(errorCode = e.errorCode)) {
                gitEnv[GitConstants.GIT_TRACE] = "1"
                if (needSleep(errorCode = e.errorCode)) {
                    // 服务端故障,睡眠后再重试
                    Thread.sleep(LONG_RETRY_PERIOD_MILLS)
                }
                // 后续根据缓存使用情况，在这里补充重试逻辑，先卸载相关代理配置，再做重试
                doRetry(args = args, retryTime = retryTime - 1)
            } else {
                throw e
            }
        }
    }

    private fun needRetry(errorCode: Int): Boolean {
        return listOf(
            GitErrors.RemoteServerFailed.errorCode,
            GitErrors.AuthenticationFailed.errorCode,
            GitErrors.RepositoryNotFoundFailed.errorCode,
            GitErrors.LockFileAlreadyExists.errorCode,
            GitErrors.MergeStashFail.errorCode
        ).contains(errorCode)
    }

    /**
     * 错误码是否需要睡眠再重试
     */
    private fun needSleep(errorCode: Int): Boolean {
        return listOf(
            GitErrors.RemoteServerFailed.errorCode,
            GitErrors.LockFileAlreadyExists.errorCode
        ).contains(errorCode)
    }

    // 工蜂如果oauth2方式授权，如果token有效但是没有仓库的权限,返回状态码是200，但是会抛出repository not found异常,
    // 导致凭证不会自动清理,所以清理构建机上存在的oauth2凭证,然后再清理
    private fun eraseOauth2Credential() {
        logger.debug("removing global credential for `oauth2` username")
        logger.debug("##[command]$ git credential reject")
        // 获取主仓库url
        val repositoryUrl = EnvHelper.getContext(CONTEXT_REPOSITORY_URL) ?: return
        val serverInfo = GitUtil.getServerInfo(repositoryUrl)
        // 清理的时候,当前仓库已经配置了credential.helper='',不会卸载全局凭证,创建一个临时目录,在临时目录执行清理命令
        val workDir = Files.createTempDirectory("git-credential-").toFile()
        try {
            if (serverInfo.httpProtocol) {
                val targetUri = URI(repositoryUrl)
                listOf("http", "https").forEach { protocol ->
                    credential(
                        repoDir = workDir,
                        action = CredentialActionEnum.REJECT,
                        inputStream = CredentialArguments(
                            protocol = protocol,
                            host = targetUri.host,
                            username = GitConstants.OAUTH2
                        ).convertInputStream()
                    )
                }
            }
        } finally {
            FileUtils.deleteDirectory(workDir)
        }
    }

    fun lfsInstall() {
        execGit(args = listOf("lfs", "install", "--local"))
    }

    fun checkout(ref: String, startPoint: String) {
        val args = mutableListOf("checkout", "--force")
        if (startPoint.isBlank()) {
            args.add(ref)
        } else {
            if (isAtLeastVersion(SUPPORT_CHECKOUT_B_GIT_VERSION)) {
                args.addAll(listOf("-B", ref, startPoint))
            } else {
                args.add(startPoint)
            }
        }
        // 最多重试3分钟
        doRetry(args = args, retryTime = 18)
        // git 1.7.3之前的版本，没有-B参数，需要先切换startPoint然后再切换分支
        if (startPoint.isNotBlank() && !isAtLeastVersion(GitConstants.SUPPORT_CHECKOUT_B_GIT_VERSION)) {
            branchDelete(ref)
            execGit(args = listOf("checkout", "-f", "-b", ref))
        }
    }

    fun branchUpstream(upstream: String) {
        val args = if (isAtLeastVersion(SUPPORT_SET_UPSTREAM_TO_GIT_VERSION)) {
            listOf("branch", "--set-upstream-to=$upstream")
        } else {
            listOf("branch", "--track", upstream)
        }
        execGit(args = args)
    }

    fun merge(ref: String) {
        val args = mutableListOf("merge")
        if (isAtLeastVersion(SUPPORT_MERGE_NO_VERIFY_GIT_VERSION)) {
            args.add("--no-verify")
        }
        args.add(ref)
        doRetry(args = args)
    }

    /**
     * 中止merge
     */
    fun mergeAbort() {
        val args = mutableListOf("merge", "--abort")
        execGit(args = args, allowAllExitCodes = true)
    }

    fun log(maxCount: Int = 1, revisionRange: String = "", branchName: String = ""): List<CommitLogInfo> {
        val args = mutableListOf(
            "log",
            "-$maxCount",
            "--pretty=format:${GitConstants.GIT_LOG_FORMAT}"
        )
        if (revisionRange.isNotBlank()) {
            args.add(revisionRange)
        }
        if (branchName.isNotBlank()) {
            args.add(branchName)
        }

        val output = execGit(args = args, allowAllExitCodes = true)
        return output.stdOuts.mapNotNull { log ->
            RegexUtil.parseLog(log)
        }
    }

    fun commitExists(commit: String): Boolean {
        val args = listOf("rev-parse", "-q", "--verify", "$commit^{commit}")
        val output = execGit(args = args, allowAllExitCodes = true, printLogger = false)
        return output.stdOut.trim().isNotBlank()
    }

    fun branchDelete(branch: String): Boolean {
        val output = execGit(
            args = listOf("branch", "-D", branch),
            allowAllExitCodes = true
        )
        return output.exitCode == 0
    }

    fun branchExists(remote: Boolean, branchName: String): Boolean {
        val args = mutableListOf("branch", "-l")
        if (remote) {
            args.add("-a")
        }
        args.add(branchName)
        val output = execGit(args = args)
        return output.stdOut.trim().isNotBlank()
    }

    fun tagExists(tag: String): Boolean {
        val args = mutableListOf("tag", "-l")
        args.add(tag)
        val output = execGit(args = args)
        return output.stdOut.trim().isNotBlank()
    }

    fun tagDelete(tagName: String) {
        execGit(
            args = listOf("tag", "-d", tagName),
            allowAllExitCodes = true
        )
    }

    fun headExists(): Boolean {
        val output = execGit(
            args = listOf("rev-parse", "--symbolic-full-name", "--verify", "--quiet", "HEAD"),
            allowAllExitCodes = true,
            printLogger = false
        )
        return output.stdOut.trim().isNotBlank()
    }

    fun isAtLeastVersion(requestedVersion: Long): Boolean {
        if (gitVersion == 0L) {
            getGitVersion()
        }
        return VersionHelper.isAtLeastVersion(
            gitVersion = gitVersion,
            requestedVersion = requestedVersion
        )
    }

    /**
     * 是否能够合并
     *
     * @param sourceBranch 源分支
     * @param targetBranch 目标分支
     */
    fun canMerge(sourceBranch: String, targetBranch: String): Boolean {
        val output = execGit(
            args = listOf("merge-base", sourceBranch, targetBranch),
            allowAllExitCodes = true
        )
        return output.stdOut.trim().isNotBlank()
    }

    /**
     * 统计两个commit之间的提交数
     */
    fun countCommits(baseCommitId: String, commitId: String): Int {
        val output = execGit(
            args = listOf("rev-list", "--count", "$baseCommitId..$commitId"),
            allowAllExitCodes = true
        )
        return if (output.stdOut.trim().isNotBlank()) {
            output.stdOut.toInt()
        } else {
            0
        }
    }

    fun readTree(
        options: List<String>
    ) {
        val args = mutableListOf("read-tree")
        args.addAll(options)
        execGit(args = args, allowAllExitCodes = true)
    }

    fun credential(
        repoDir: File? = null,
        action: CredentialActionEnum,
        inputStream: InputStream
    ) {
        val args = listOf("credential", action.value)
        CommandUtil.execute(
            workingDirectory = repoDir ?: workingDirectory,
            executable = "git",
            args = args,
            runtimeEnv = gitEnv,
            inputStream = inputStream,
            allowAllExitCodes = true,
            printLogger = false,
            // git 低版本的credential-cache错误流没有关闭,导致程序会挂起,需要不捕获错误流
            handleErrStream = false
        )
    }

    fun shaExists(sha: String): Boolean {
        val output = execGit(
            args = listOf("rev-parse", "--verify", "--quiet", "$sha^{object}"),
            allowAllExitCodes = true,
            printLogger = false
        )
        return output.stdOut.trim().isNotBlank()
    }

    private fun execGit(
        repoDir: File? = null,
        args: List<String>,
        allowAllExitCodes: Boolean = false,
        logType: LogType = LogType.TEXT,
        printLogger: Boolean = true
    ): GitOutput {
        return CommandUtil.execute(
            workingDirectory = repoDir ?: workingDirectory,
            executable = "git",
            args = args,
            runtimeEnv = gitEnv,
            allowAllExitCodes = allowAllExitCodes,
            logType = logType,
            printLogger = printLogger
        )
    }

    fun tryCleanLfs(): Boolean {
        val args = mutableListOf("lfs", "prune")
        val output = execGit(args = args, allowAllExitCodes = true, logType = LogType.PROGRESS)
        return output.exitCode == 0
    }

    fun tryPrune(remoteName: String): Boolean {
        val args = mutableListOf("remote", "prune", remoteName)
        val output = execGit(args = args, allowAllExitCodes = true, logType = LogType.PROGRESS)
        return output.exitCode == 0
    }

    /**
     * 输出当前git状态
     */
    fun currentStatus() {
        execGit(args = listOf("status"))
    }

    /**
     * 输出当前lfs版本
     */
    fun lfsVersion() {
        execGit(args = listOf("lfs", "version"))
    }

    /**
     * sparse checkout 初始化
     */
    fun sparseCheckoutInit(
        cone: Boolean
    ) {
        val args = mutableListOf("sparse-checkout", "init")
        if (cone) {
            args.add("--cone")
        } else {
            args.add("--no-cone")
        }
        execGit(args = args)
    }

    /**
     * sparse checkout 添加拉取路径
     */
    fun sparseCheckoutSet(
        paths: List<String>
    ) {
        execGit(args = listOf("sparse-checkout", "set").plus(paths))
    }

    /**
     * sparse checkout 配置列表
     */
    fun sparseCheckoutList() {
        execGit(args = listOf("sparse-checkout", "list"))
    }
}
