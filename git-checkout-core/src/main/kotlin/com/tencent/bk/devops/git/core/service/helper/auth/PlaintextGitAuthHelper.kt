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
import com.tencent.bk.devops.git.core.enums.AuthHelperType
import com.tencent.bk.devops.git.core.enums.CommandLogLevel
import com.tencent.bk.devops.git.core.enums.GitProtocolEnum
import com.tencent.bk.devops.git.core.pojo.AuthInfo
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.pojo.GitSubmodule
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.service.helper.IGitAuthHelper
import com.tencent.bk.devops.git.core.util.CommandUtil
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.GitUtil
import com.tencent.bk.devops.git.core.util.SubmoduleUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI

class PlaintextGitAuthHelper(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) : IGitAuthHelper {

    companion object {
        private val logger = LoggerFactory.getLogger(PlaintextGitAuthHelper::class.java)
    }

    private val serverInfo = GitUtil.getServerInfo(settings.repositoryUrl)
    private val authInfo = settings.authInfo

    override fun removePreviousAuth() = Unit

    override fun configureAuth() {
        logger.info("using plaintext auth to set credentials ${authInfo.username}/******")
        EnvHelper.putContext(ContextConstants.CONTEXT_GIT_PROTOCOL, GitProtocolEnum.HTTP.name)
        with(settings) {
            replaceUrl(
                url = repositoryUrl,
                remoteName = GitConstants.ORIGIN_REMOTE_NAME,
                authInfo = authInfo
            )
            if (preMerge && !sourceRepoUrlEqualsRepoUrl) {
                replaceUrl(
                    url = sourceRepositoryUrl,
                    remoteName = GitConstants.DEVOPS_VIRTUAL_REMOTE_NAME,
                    authInfo = forkRepoAuthInfo ?: authInfo
                )
            }
        }
        EnvHelper.putContext(GitConstants.GIT_CREDENTIAL_AUTH_HELPER, AuthHelperType.PLAINTEXT.name)
    }

    private fun replaceUrl(url: String, remoteName: String, authInfo: AuthInfo) {
        val uri = URI(url)
        val authUrl = "${uri.scheme}://" +
            "${authInfo.username}:${GitUtil.urlEncode(authInfo.password!!)}@" +
            "${uri.host}${uri.path}"
        git.remoteSetUrl(remoteName = remoteName, remoteUrl = authUrl)
    }

    override fun removeAuth() {
        with(settings) {
            git.remoteSetUrl(remoteName = GitConstants.ORIGIN_REMOTE_NAME, remoteUrl = repositoryUrl)
            if (preMerge && !sourceRepoUrlEqualsRepoUrl) {
                git.remoteSetUrl(remoteName = GitConstants.DEVOPS_VIRTUAL_REMOTE_NAME, remoteUrl = sourceRepositoryUrl)
            }
        }
    }

    fun configureSubmoduleAuth(repoDir: File, modules: List<GitSubmodule>) {
        val submoduleConfigFile = File(repoDir, ".gitmodules")
        var submoduleConfig = submoduleConfigFile.readText()
        modules.forEach { module ->
            submoduleConfig = submoduleConfig.replace(module.url, getModuleAuthUrl(module))
        }
        submoduleConfigFile.writeText(submoduleConfig)
    }

    override fun removeSubmoduleAuth() {
        SubmoduleUtil.submoduleForeach(
            repositoryDir = File(settings.repositoryPath),
            recursive = settings.nestedSubmodules
        ) { submodule ->
            val commands = mutableListOf<String>()
            commands.add("git checkout .gitmodules")
            commands.add("git submodule sync")
            if (File(submodule.absolutePath).exists()) {
                CommandUtil.execute(
                    command = commands.joinToString("\n"),
                    workingDirectory = File(submodule.absolutePath),
                    printLogger = true,
                    logLevel = CommandLogLevel.DEBUG,
                    allowAllExitCodes = true
                )
            }
        }
    }

    private fun getModuleAuthUrl(module: GitSubmodule): String {
        return try {
            val moduleServerInfo = GitUtil.getServerInfo(module.url)
            val sameGitServer = GitUtil.isSameHostName(
                targetHostName = serverInfo.hostName,
                sourceHostName = moduleServerInfo.hostName,
                hostNameList = settings.compatibleHostList
            )
            if (sameGitServer) {
                val mainRepoUri = URI(settings.repositoryUrl)
                if (moduleServerInfo.httpProtocol) {
                    val uri = URI(module.url)
                    "${uri.scheme}://" +
                        "${authInfo.username}:${GitUtil.urlEncode(authInfo.password!!)}@" +
                        "${mainRepoUri.host}${uri.path}"
                } else {
                    "${mainRepoUri.scheme}://" +
                        "${authInfo.username}:${GitUtil.urlEncode(authInfo.password!!)}@" +
                        "${mainRepoUri.host}/${moduleServerInfo.repositoryName}.git"
                }
            } else {
                module.url
            }
        } catch (ignore: Exception) {
            module.url
        }
    }

    override fun configGlobalAuth() = Unit

    override fun removeGlobalAuth() = Unit

    override fun configureSubmoduleAuth() = Unit
}
