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

package com.tencent.bk.devops.git.core.service.input

import com.tencent.bk.devops.git.core.api.DevopsApi
import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_REPOSITORY_TYPE
import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_EXCLUDE_PATH
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_INCLUDE_PATH
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_REF
import com.tencent.bk.devops.git.core.enums.AuthType
import com.tencent.bk.devops.git.core.enums.PullStrategy
import com.tencent.bk.devops.git.core.enums.PullType
import com.tencent.bk.devops.git.core.enums.ScmType
import com.tencent.bk.devops.git.core.exception.ParamInvalidException
import com.tencent.bk.devops.git.core.pojo.AuthInfo
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.pojo.api.RepositoryType
import com.tencent.bk.devops.git.core.pojo.input.GitCodeCommandAtomParamInput
import com.tencent.bk.devops.git.core.service.auth.AuthUserTokenGitAuthProvider
import com.tencent.bk.devops.git.core.service.auth.CredentialGitAuthProvider
import com.tencent.bk.devops.git.core.service.auth.EmptyGitAuthProvider
import com.tencent.bk.devops.git.core.service.auth.OauthGitAuthProvider
import com.tencent.bk.devops.git.core.service.auth.PrivateGitAuthProvider
import com.tencent.bk.devops.git.core.service.auth.UserNamePasswordGitAuthProvider
import com.tencent.bk.devops.git.core.service.auth.UserTokenGitAuthProvider
import com.tencent.bk.devops.git.core.service.helper.IInputAdapter
import com.tencent.bk.devops.git.core.service.repository.GitScmService
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.GitUtil
import com.tencent.bk.devops.git.core.util.RegexUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Locale

class GitCodeCommandAtomParamInputAdapter(
    private val input: GitCodeCommandAtomParamInput
) : IInputAdapter {

    companion object {
        private val devopsApi = DevopsApi()
        private val logger = LoggerFactory.getLogger(GitCodeCommandAtomParamInputAdapter::class.java)
    }

    @Suppress("ALL")
    override fun getInputs(): GitSourceSettings {
        with(input) {
            if (repositoryUrl.isBlank()) {
                throw ParamInvalidException(errorMsg = "Repository url cannot be empty")
            }

            // 获取鉴权信息,post action阶段不需要查询凭证
            val authProvider = getAuthProvider(repositoryUrl)
            // 主库凭证信息
            val authInfo = authProvider.getAuthInfo()
            // fork库凭证信息
            var forkRepoAuthInfo: AuthInfo? = null
            // 代码库ID
            val gitProjectId = if (!RegexUtil.isIPAddress(GitUtil.getServerInfo(repositoryUrl).hostName)) {
                GitScmService(
                    scmType = scmType,
                    repositoryUrl = repositoryUrl,
                    authInfo = authInfo
                ).getGitProjectId()
            } else {
                null
            } ?: ""
            // 保存代码库相关信息
            EnvHelper.addEnvVariable(GitConstants.BK_CI_GIT_PROJECT_ID, "$gitProjectId")
            var ref: String = refName
            val preMerge = GitUtil.isEnablePreMerge(
                enableVirtualMergeBranch = enableVirtualMergeBranch,
                repositoryUrl = repositoryUrl,
                hookEventType = hookEventType,
                hookTargetUrl = hookTargetUrl,
                compatibleHostList = hostNameList,
                scmType = scmType
            )
            if (preMerge) {
                ref = hookTargetBranch!!
                pullType = PullType.BRANCH.name
                forkRepoAuthInfo = getForkRepoAuthInfo()
            }

            EnvHelper.addEnvVariable(GitConstants.BK_CI_GIT_REPO_CODE_PATH, localPath ?: "")
            EnvHelper.addEnvVariable(
                key = GitConstants.BK_CI_GIT_REPO_ALIAS_NAME,
                value = GitUtil.getServerInfo(repositoryUrl).repositoryName
            )
            EnvHelper.addEnvVariable(BK_CI_GIT_REPO_REF, refName)
            EnvHelper.addEnvVariable(GitConstants.BK_CI_GIT_REPO_BRANCH, refName)
            EnvHelper.addEnvVariable(GitConstants.DEVOPS_GIT_REPO_BRANCH, refName)
            EnvHelper.addEnvVariable(BK_CI_GIT_REPO_INCLUDE_PATH, input.includePath ?: "")
            EnvHelper.addEnvVariable(BK_CI_GIT_REPO_EXCLUDE_PATH, input.excludePath ?: "")

            // 添加代码库信息支持codecc扫描
            EnvHelper.addEnvVariable("bk_repo_taskId_$pipelineTaskId", pipelineTaskId)
            EnvHelper.addEnvVariable("bk_repo_type_$pipelineTaskId", "GIT")
            EnvHelper.addEnvVariable("bk_repo_local_path_$pipelineTaskId", localPath ?: "")
            EnvHelper.addEnvVariable("bk_repo_code_url_$pipelineTaskId", repositoryUrl)
            EnvHelper.addEnvVariable(
                key = "bk_repo_auth_type_$pipelineTaskId",
                value = getAuthType(authType = authType, repositoryUrl = repositoryUrl)
            )
            EnvHelper.addEnvVariable(
                key = "bk_repo_container_id_$pipelineTaskId",
                value = System.getenv(GitConstants.BK_CI_BUILD_JOB_ID)
            )
            EnvHelper.addEnvVariable("bk_repo_include_path_${input.pipelineTaskId}", input.includePath ?: "")
            EnvHelper.addEnvVariable("bk_repo_exclude_path_${input.pipelineTaskId}", input.excludePath ?: "")
            EnvHelper.putContext(CONTEXT_REPOSITORY_TYPE, RepositoryType.URL.name)
            EnvHelper.addEnvVariable("bk_repo_git_project_id_${input.pipelineTaskId}", "$gitProjectId")
            if (authType == AuthType.TICKET) {
                EnvHelper.addEnvVariable("bk_repo_ticket_id_${input.pipelineTaskId}", ticketId ?: "")
            }

            return GitSourceSettings(
                bkWorkspace = bkWorkspace,
                pipelineId = pipelineId,
                pipelineTaskId = pipelineTaskId,
                pipelineBuildId = pipelineBuildId,
                pipelineStartUserName = pipelineStartUserName,
                postEntryParam = postEntryParam,
                scmType = scmType,
                repositoryUrl = repositoryUrl,
                repositoryPath = if (localPath.isNullOrBlank()) {
                    File(bkWorkspace)
                } else {
                    File(bkWorkspace, localPath!!)
                }.absolutePath,
                ref = ref,
                pullType = PullType.valueOf(pullType),
                commit = retryStartPoint ?: "",
                pullStrategy = PullStrategy.valueOf(strategy),
                enableGitClean = enableGitClean,
                enableGitCleanIgnore = enableGitCleanIgnore,
                enableGitCleanNested = enableGitCleanNested,
                fetchDepth = fetchDepth ?: 0,
                enableFetchRefSpec = enableFetchRefSpec,
                fetchRefSpec = fetchRefSpec,
                lfs = enableGitLfs,
                lfsConcurrentTransfers = lfsConcurrentTransfers,
                enableGitLfsClean = enableGitLfsClean,
                submodules = enableSubmodule,
                nestedSubmodules = enableSubmoduleRecursive ?: true,
                submoduleRemote = enableSubmoduleRemote,
                submodulesPath = submodulePath ?: "",
                submoduleDepth = submoduleDepth,
                submoduleJobs = submoduleJobs,
                includeSubPath = includePath,
                excludeSubPath = excludePath,
                authInfo = authInfo,
                persistCredentials = persistCredentials,
                preMerge = preMerge,
                sourceRepositoryUrl = hookSourceUrl ?: "",
                sourceBranchName = hookSourceBranch ?: "",
                autoCrlf = autoCrlf,
                usernameConfig = usernameConfig,
                userEmailConfig = userEmailConfig,
                compatibleHostList = hostNameList,
                enableTrace = enableTrace,
                enablePartialClone = enablePartialClone,
                cachePath = cachePath,
                enableGlobalInsteadOf = enableGlobalInsteadOf,
                useCustomCredential = useCustomCredential,
                forkRepoAuthInfo = forkRepoAuthInfo,
                enableTGitCache = (enableTGitCache ?: false),
                tGitCacheUrl = tGitCacheUrl,
                tGitCacheProxyUrl = tGitCacheProxyUrl,
                setSafeDirectory = setSafeDirectory,
                mainRepo = mainRepo,
                tGitCacheGrayProject = tGitCacheGrayProject,
                tGitCacheGrayWeight = tGitCacheGrayWeight,
                tGitCacheGrayWhiteProject = tGitCacheGrayWhiteProject
            )
        }
    }

    private fun getAuthType(authType: AuthType?, repositoryUrl: String): String {
        return when {
            authType == AuthType.ACCESS_TOKEN -> "OAUTH"
            authType == AuthType.USERNAME_PASSWORD -> "HTTP"
            repositoryUrl.uppercase(Locale.getDefault()).startsWith("HTTP") -> "HTTP"
            else ->
                "SSH"
        }
    }

    /**
     * 获取代码库授权提供者
     */

    private fun getAuthProvider(newRepositoryUrl: String) = with(input) {
        // 获取鉴权信息,post action阶段不需要查询凭证
        if (postEntryParam == "True") {
            EmptyGitAuthProvider()
        } else {
            when (authType) {
                AuthType.ACCESS_TOKEN -> OauthGitAuthProvider(token = accessToken, userId = "")
                AuthType.USERNAME_PASSWORD -> UserNamePasswordGitAuthProvider(
                    username = username,
                    password = password
                )
                AuthType.TICKET -> CredentialGitAuthProvider(
                    credentialId = ticketId,
                    devopsApi = devopsApi,
                    defaultGitAuthProvider = if (scmType == ScmType.CODE_GIT) {
                        UserTokenGitAuthProvider(
                            userId = pipelineStartUserName,
                            devopsApi = devopsApi,
                            scmType = scmType
                        )
                    } else {
                        EmptyGitAuthProvider()
                    }
                )
                AuthType.START_USER_TOKEN -> UserTokenGitAuthProvider(
                    userId = pipelineStartUserName,
                    devopsApi = devopsApi,
                    scmType = scmType
                )
                AuthType.PERSONAL_ACCESS_TOKEN -> PrivateGitAuthProvider(
                    token = personalAccessToken
                )
                AuthType.AUTH_USER_TOKEN ->
                    AuthUserTokenGitAuthProvider(
                        pipelineStartUserName = pipelineStartUserName,
                        userId = authUserId,
                        repositoryUrl = repositoryUrl,
                        devopsApi = devopsApi,
                        scmType = scmType
                    )
                else -> EmptyGitAuthProvider()
            }
        }
    }

    /**
     * 获取fork仓库授权信息
     * 1.post阶段不获取
     * 2.非git、github代码库不获取，后续支持tgit
     * 3.不是fork仓库的，源仓库和目标仓库是同一个仓库
     */
    private fun getForkRepoAuthInfo() = with(input) {
        if (postEntryParam == "True" ||
            !listOf(ScmType.CODE_GIT, ScmType.GITHUB).contains(scmType) ||
            GitUtil.isSameRepository(
                repositoryUrl = repositoryUrl,
                otherRepositoryUrl = hookSourceUrl,
                hostNameList = hostNameList
            )
        ) {
            null
        } else {
            try {
                UserTokenGitAuthProvider(
                    userId = pipelineStartUserName,
                    devopsApi = devopsApi,
                    scmType = scmType
                ).getAuthInfo()
            } catch (ignored: Exception) {
                logger.warn("can't get fork repository auth info,${ignored.message}")
                null
            }
        }
    }
}
