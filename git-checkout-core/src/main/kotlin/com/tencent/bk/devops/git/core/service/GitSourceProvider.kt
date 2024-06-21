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

import com.tencent.bk.devops.git.core.api.GitClientApi
import com.tencent.bk.devops.git.core.api.IDevopsApi
import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_REPOSITORY_HTTP_URL
import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_REPOSITORY_URL
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_NAME
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_URL
import com.tencent.bk.devops.git.core.enums.GitErrors
import com.tencent.bk.devops.git.core.exception.GitExecuteException
import com.tencent.bk.devops.git.core.exception.ParamInvalidException
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.handler.GitAuthHandler
import com.tencent.bk.devops.git.core.service.handler.GitAuthPlaintextHandler
import com.tencent.bk.devops.git.core.service.handler.GitCheckoutAndMergeHandler
import com.tencent.bk.devops.git.core.service.handler.GitFetchHandler
import com.tencent.bk.devops.git.core.service.handler.GitLfsHandler
import com.tencent.bk.devops.git.core.service.handler.GitLogHandler
import com.tencent.bk.devops.git.core.service.handler.GitSubmodulesHandler
import com.tencent.bk.devops.git.core.service.handler.GitSubmodulesPlaintextHandler
import com.tencent.bk.devops.git.core.service.handler.HandlerExecutionChain
import com.tencent.bk.devops.git.core.service.handler.InitRepoHandler
import com.tencent.bk.devops.git.core.service.handler.PrepareWorkspaceHandler
import com.tencent.bk.devops.git.core.service.helper.auth.GitAuthHelperFactory
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.GitUtil
import org.slf4j.LoggerFactory
import java.io.File

class GitSourceProvider(
    private val settings: GitSourceSettings,
    private val devopsApi: IDevopsApi
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GitSourceProvider::class.java)
    }

    fun getSource() {
        with(settings) {
            logger.info("Syncing repository: ${settings.repositoryUrl}")
            EnvHelper.addEnvVariable(BK_CI_GIT_REPO_URL, settings.repositoryUrl)
            val repositoryName = GitUtil.getServerInfo(settings.repositoryUrl).repositoryName
            EnvHelper.addEnvVariable(BK_CI_GIT_REPO_NAME, repositoryName)
            EnvHelper.putContext(CONTEXT_REPOSITORY_URL, repositoryUrl)
            val repositoryHttpUrl = if (repositoryUrl.startsWith("git@")) {
                repositoryUrl.replace(":", "/").replace("git@", "https://")
            } else {
                repositoryUrl
            }
            EnvHelper.putContext(CONTEXT_REPOSITORY_HTTP_URL, repositoryHttpUrl)

            logger.info("Working directory is: $repositoryPath")
            if (ref.isBlank()) {
                throw ParamInvalidException(
                    errorMsg = "The refs cannot be empty",
                    errorCode = GitErrors.EmptyBranch.errorCode,
                    errorType = GitErrors.EmptyBranch.errorType,
                    reason = GitErrors.EmptyBranch.cause!!,
                    solution = GitErrors.EmptyBranch.solution!!,
                    wiki = GitErrors.EmptyBranch.wiki!!
                )
            }
            val workingDirectory = File(repositoryPath)
            val git = GitCommandManager(workingDirectory = workingDirectory, lfs = lfs)
            val handlerChain = HandlerExecutionChain(
                listOf(
                    PrepareWorkspaceHandler(settings, git),
                    InitRepoHandler(settings, git),
                    GitAuthHandler(settings, git),
                    GitFetchHandler(settings, git),
                    GitCheckoutAndMergeHandler(settings, git),
                    GitSubmodulesHandler(settings, git),
                    GitLfsHandler(settings, git),
                    GitLogHandler(settings, git, devopsApi)
                )
            )
            try {
                handlerChain.doHandle()
            } catch (ignore: GitExecuteException) {
                if (!needRetry(ignore.errorCode)) {
                    throw ignore
                }
                // 兜底方案
                plaintextAuthRetry(git = git)
            } finally {
                handlerChain.afterHandle()
            }
        }
    }

    // 先校验凭证是否是正确的,如果是正确的,表示自定义凭证管理有误,使用明文凭证
    private fun needRetry(errorCode: Int): Boolean {
        val gitClientApi = GitClientApi()
        return listOf(
            GitErrors.AuthenticationFailed.errorCode,
            GitErrors.RepositoryNotFoundFailed.errorCode,
            GitErrors.SshAuthenticationFailed.errorCode
        ).contains(errorCode) && gitClientApi.checkCredentials(
            repositoryUrl = settings.repositoryUrl,
            authInfo = settings.authInfo
        )
    }

    /**
     * 如果凭证设置失败,导致拉取失败,使用明文拉取重试
     */
    private fun plaintextAuthRetry(git: GitCommandManager) {
        logger.warn("************************************auth retry**********************************************")
        val handlerChain = HandlerExecutionChain(
            listOf(
                GitAuthPlaintextHandler(settings, git),
                GitFetchHandler(settings, git),
                GitCheckoutAndMergeHandler(settings, git),
                GitSubmodulesPlaintextHandler(settings, git),
                GitLfsHandler(settings, git),
                GitLogHandler(settings, git, devopsApi)
            )
        )
        handlerChain.doHandle()
    }

    fun cleanUp() {
        logger.warn("This plugin is a post-action of the git plugin, " +
            "which is automatically generated by the system to " +
            "clean up the credentials of the git plugin on the build machine"
        )
        with(settings) {
            val workingDirectory = File(repositoryPath)
            if (!workingDirectory.exists()) {
                return
            }
            val git = GitCommandManager(workingDirectory = workingDirectory, lfs = false)
            val authHelper = GitAuthHelperFactory.getCleanUpAuthHelper(git = git, settings = settings)
            if (settings.submodules && settings.persistCredentials) {
                logger.groupStart("removing credentials for submodules")
                authHelper.removeSubmoduleAuth()
                logger.groupEnd("")
            }
            if (settings.persistCredentials) {
                logger.groupStart("removing auth")
                authHelper.removeAuth()
                logger.groupEnd("")
            }
            if (settings.enableTGitCache == true && GitUtil.isHttpProtocol(settings.repositoryUrl)) {
                val serverInfo = GitUtil.getServerInfo(settings.repositoryUrl)
                val origin = serverInfo.origin
                git.tryConfigUnset("http.$origin.proxy")
                git.tryConfigUnset("http.$origin.sslverify")
            }
        }
    }
}
