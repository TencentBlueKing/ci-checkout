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

package com.tencent.bk.devops.git.core.service.helper.auth

import com.tencent.bk.devops.git.core.constant.ContextConstants
import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_BUILD_JOB_ID
import com.tencent.bk.devops.git.core.constant.GitConstants.CREDENTIAL_COMPATIBLE_HOST
import com.tencent.bk.devops.git.core.constant.GitConstants.CREDENTIAL_JAR_PATH
import com.tencent.bk.devops.git.core.constant.GitConstants.CREDENTIAL_JAVA_PATH
import com.tencent.bk.devops.git.core.constant.GitConstants.GIT_CREDENTIAL_HELPER
import com.tencent.bk.devops.git.core.constant.GitConstants.GIT_REPO_PATH
import com.tencent.bk.devops.git.core.enums.AuthHelperType
import com.tencent.bk.devops.git.core.enums.GitConfigScope
import com.tencent.bk.devops.git.core.enums.GitProtocolEnum
import com.tencent.bk.devops.git.core.enums.OSType
import com.tencent.bk.devops.git.core.pojo.CredentialArguments
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.pojo.ServerInfo
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.service.helper.VersionHelper
import com.tencent.bk.devops.git.core.util.AgentEnv
import com.tencent.bk.devops.git.core.util.CommandUtil
import com.tencent.bk.devops.git.core.util.EnvHelper
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.nio.file.Paths

/**
 * 使用自定义git-checkout-credential凭证
 *
 * 用于第三方构建机或公共构建机没有配置全局凭证的
 */
@Suppress("ALL")
class CredentialCheckoutAuthHelper(
    private val git: GitCommandManager,
    private val settings: GitSourceSettings
) : HttpGitAuthHelper(git = git, settings = settings) {

    companion object {
        private val logger = LoggerFactory.getLogger(CredentialCheckoutAuthHelper::class.java)
    }

    private val credentialVersion = VersionHelper.getCredentialVersion()
    private val credentialJarFileName = if (credentialVersion.isNotBlank()) {
        "git-checkout-credential-$credentialVersion.jar"
    } else {
        "git-checkout-credential.jar"
    }
    private val credentialShellFileName = "git-checkout.sh"
    private val credentialHome = File(System.getProperty("user.home"), ".checkout").absolutePath
    private val credentialJarPath = File(credentialHome, credentialJarFileName).absolutePath
    private val credentialShellPath = File(credentialHome, credentialShellFileName).absolutePath

    override fun configureAuth() {
        logger.info("using custom credential helper to set credentials ${authInfo.username}/******")
        EnvHelper.putContext(ContextConstants.CONTEXT_GIT_PROTOCOL, GitProtocolEnum.HTTP.name)

        // 设置自定义凭证管理需要的环境变量
        val jobId = System.getenv(BK_CI_BUILD_JOB_ID)
        EnvHelper.addEnvVariable("${CREDENTIAL_JAVA_PATH}_$jobId", getJavaFilePath())
        EnvHelper.addEnvVariable("${CREDENTIAL_JAR_PATH}_$jobId", credentialJarFileName)

        git.setEnvironmentVariable("${CREDENTIAL_JAVA_PATH}_$jobId", getJavaFilePath())
        git.setEnvironmentVariable("${CREDENTIAL_JAR_PATH}_$jobId", credentialJarFileName)

        // 仓库凭证配置
        git.config(
            configKey = GitConstants.GIT_CREDENTIAL_AUTH_HELPER,
            configValue = AuthHelperType.CUSTOM_CREDENTIAL.name
        )
        EnvHelper.putContext(GitConstants.GIT_CREDENTIAL_AUTH_HELPER, AuthHelperType.CUSTOM_CREDENTIAL.name)

        git.config(configKey = GitConstants.GIT_CREDENTIAL_TASKID, configValue = settings.pipelineTaskId)
        // 卸载子模块insteadOf时使用
        git.config(
            configKey = GitConstants.GIT_CREDENTIAL_INSTEADOF_KEY,
            configValue = "url.${serverInfo.origin}/.insteadOf"
        )
        // 插件执行过程中,把构建机上所有的凭证管理都禁用了,导致构建机上的错误的凭证没有被覆盖,可能使编译过程报错，所以提前存储一次
        storeGlobalCredential(writeCompatibleHost = false)
        if (git.isAtLeastVersion(GitConstants.SUPPORT_EMPTY_CRED_HELPER_GIT_VERSION)) {
            git.tryDisableOtherGitHelpers(configScope = GitConfigScope.LOCAL)
        } else {
            // 如果低于git 2.9版本以下,则通过设置一个不存在的username,禁用其他凭证管理
            git.config(GitConstants.GIT_CREDENTIAL_USERNAME, settings.pipelineTaskId)
        }
        git.configAdd(
            configKey = repoCredentialHelperKey(),
            configValue = "!bash '$credentialShellPath' ${settings.pipelineTaskId}"
        )

        install()
        store()
        // 是否保存fork凭证
        if (settings.storeForkRepoCredential) {
            try {
                forkInstall()
                forkStore()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun install() {
        val credentialJarParentFile = File(credentialHome)
        if (!credentialJarParentFile.exists()) {
            credentialJarParentFile.mkdirs()
        }
        copyCredentialFile(
            sourceFilePath = "script/$credentialJarFileName",
            targetFile = File(credentialJarPath)
        )

        replaceCredentialFile(
            sourceFilePath = "script/$credentialShellFileName",
            targetFile = File(credentialShellPath)
        )
        // TODO 卸载历史的全局的git-checkout-credential凭证,后续需要删除
        git.tryConfigUnset(
            configKey = repoCredentialHelperKey(),
            configValueRegex = GitConstants.GIT_CHECKOUT_CREDENTIAL_VALUE_REGEX,
            configScope = GitConfigScope.GLOBAL
        )
        try {
            // 凭证管理必须安装在全局,否则无法传递给其他插件
            if (!git.configExists(
                    configKey = repoCredentialHelperKey(),
                    configValueRegex = GitConstants.GIT_CREDENTIAL_HELPER_VALUE_REGEX,
                    configScope = GitConfigScope.GLOBAL
                )
            ) {
                git.configAdd(
                    configKey = repoCredentialHelperKey(),
                    configValue = "!bash '$credentialShellPath'",
                    configScope = GitConfigScope.GLOBAL
                )
            }
        } catch (ignore: Exception) {
        }
    }

    private fun store() {
        with(URL(settings.repositoryUrl).toURI()) {
            CommandUtil.execute(
                executable = getJavaFilePath(),
                args = listOf(
                    "-Dfile.encoding=utf-8",
                    "-Ddebug=${settings.enableTrace}",
                    "-jar",
                    credentialJarPath,
                    settings.pipelineTaskId,
                    "devopsStore"
                ),
                runtimeEnv = mapOf(
                    GIT_REPO_PATH to settings.repositoryPath,
                    CREDENTIAL_COMPATIBLE_HOST to (settings.compatibleHostList?.joinToString(",") ?: "")
                ),
                inputStream = CredentialArguments(
                    protocol = scheme,
                    host = host,
                    path = path.removePrefix("/"),
                    username = authInfo.username,
                    password = authInfo.password
                ).convertInputStream()
            )
        }
    }

    private fun copyCredentialFile(sourceFilePath: String, targetFile: File) {
        if (!targetFile.exists()) {
            javaClass.classLoader.getResourceAsStream(sourceFilePath)?.use { sourceInputStream ->
                FileUtils.copyToFile(sourceInputStream, targetFile)
            }
        }
    }

    private fun replaceCredentialFile(sourceFilePath: String, targetFile: File) {
        if (!targetFile.exists()) {
            javaClass.classLoader.getResourceAsStream(sourceFilePath)?.use { sourceInputStream ->
                FileUtils.copyToFile(sourceInputStream, targetFile)
            }
        } else {
            val newFileMd5 = javaClass.classLoader.getResourceAsStream(sourceFilePath)?.use { DigestUtils.md5Hex(it) }
            val oldFileMd5 = targetFile.inputStream().use { DigestUtils.md5Hex(it) }
            if (newFileMd5 != oldFileMd5) {
                targetFile.delete()
                javaClass.classLoader.getResourceAsStream(sourceFilePath)?.use { sourceInputStream ->
                    FileUtils.copyToFile(sourceInputStream, targetFile)
                }
            }
        }
    }

    private fun getJavaFilePath() = File(System.getProperty("java.home"), "/bin/java").absolutePath

    override fun removeAuth() {
        if (!serverInfo.httpProtocol) {
            return
        }
        val taskId = git.tryConfigGet(configKey = GitConstants.GIT_CREDENTIAL_TASKID)
        // 清理构建机上凭证
        if (File(credentialJarPath).exists()) {
            with(URL(settings.repositoryUrl).toURI()) {
                CommandUtil.execute(
                    executable = getJavaFilePath(),
                    args = listOf(
                        "-Dfile.encoding=utf-8",
                        "-Ddebug=${settings.enableTrace}",
                        "-jar",
                        credentialJarPath,
                        taskId,
                        "devopsErase"
                    ),
                    runtimeEnv = mapOf(
                        GIT_REPO_PATH to settings.repositoryPath,
                        CREDENTIAL_COMPATIBLE_HOST to (settings.compatibleHostList?.joinToString(",") ?: "")
                    ),
                    inputStream = CredentialArguments(
                        protocol = scheme,
                        host = host,
                        path = path.removePrefix("/")
                    ).convertInputStream()
                )
            }
        }
        git.tryConfigUnset(configKey = GIT_CREDENTIAL_HELPER)
        git.tryConfigUnset(configKey = GitConstants.GIT_CREDENTIAL_INSTEADOF_KEY)
        if (!git.isAtLeastVersion(GitConstants.SUPPORT_EMPTY_CRED_HELPER_GIT_VERSION)) {
            git.tryConfigUnset(configKey = GitConstants.GIT_CREDENTIAL_USERNAME)
        }
        git.tryConfigGetAll(configKey = GIT_CREDENTIAL_HELPER)
    }

    override fun configXdgAuthCommand() {
        val configFile = Paths.get(
            git.getEnvironmentVariable(GitConstants.HOME).toString(),
            ".gitconfig"
        ).toFile()
        appendCredential(configFile)
    }

    private fun appendCredential(configFile: File) {
        val credentialValues = StringBuilder()
        if (!configFile.exists()) {
            return
        }
        val escapeCredentialShellPath = if (AgentEnv.getOS() == OSType.WINDOWS) {
            credentialShellPath.replace("\\", "\\\\")
        } else {
            credentialShellPath
        }
        combinableHost { protocol, host ->
            credentialValues.append("[credential \"$protocol://$host/\"]\n")
            /* mac和windows构建机版本肯定都大于2.9，
            linux机器如果低于git 2.9，因为xdg配置优先于~/.gitconfig,所以低于git 2.9版本不需要禁用其他凭证
             */
            if (git.isAtLeastVersion(GitConstants.SUPPORT_EMPTY_CRED_HELPER_GIT_VERSION)) {
                credentialValues.append("        helper =\n")
            }
            credentialValues.append("        helper = !bash '$escapeCredentialShellPath'\n")
        }
        logger.info("write checkout credential config to global config\n$credentialValues")
        if (configFile.exists()) {
            configFile.appendText(credentialValues.toString())
        }
    }

    override fun configSubmoduleAuthCommand(
        moduleServerInfo: ServerInfo,
        commands: MutableList<String>
    ) {
        if (git.isAtLeastVersion(GitConstants.SUPPORT_EMPTY_CRED_HELPER_GIT_VERSION)) {
            commands.add("git config --add credential.helper \"\" ")
        } else {
            commands.add("git config credential.username ${settings.pipelineTaskId}")
        }
        commands.add(
            "git config --add credential.helper " +
                    "\"!bash '${File(credentialShellPath).canonicalPath}' ${settings.pipelineTaskId}\""
        )
    }

    /**
     * 安装fork库凭证
     */
    private fun forkInstall() {
        git.tryConfigUnset(configKey = forkRepoCredentialHelperKey())
        git.configAdd(
            configKey = "credential.${settings.sourceRepositoryUrl}.helper",
            configValue = "!bash '$credentialShellPath' ${settings.pipelineTaskId}-fork",
            configScope = GitConfigScope.LOCAL
        )
    }

    /**
     * 保存fork库凭证
     */
    private fun forkStore() {
        with(URL(settings.sourceRepositoryUrl).toURI()) {
            CommandUtil.execute(
                executable = getJavaFilePath(),
                args = listOf(
                    "-Dfile.encoding=utf-8",
                    "-Ddebug=${settings.enableTrace}",
                    "-jar",
                    credentialJarPath,
                    "${settings.pipelineTaskId}-fork",
                    "devopsStore"
                ),
                runtimeEnv = mapOf(
                    GIT_REPO_PATH to settings.repositoryPath
                ),
                inputStream = CredentialArguments(
                    protocol = scheme,
                    host = host,
                    path = path.removePrefix("/"),
                    username = settings.forkRepoAuthInfo!!.username,
                    password = settings.forkRepoAuthInfo!!.password
                ).convertInputStream()
            )
        }
    }
}
