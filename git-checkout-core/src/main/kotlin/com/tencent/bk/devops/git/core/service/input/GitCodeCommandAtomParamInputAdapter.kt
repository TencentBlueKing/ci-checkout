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
import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_EXCLUDE_PATH
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_INCLUDE_PATH
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_REF
import com.tencent.bk.devops.git.core.enums.AuthType
import com.tencent.bk.devops.git.core.enums.PullStrategy
import com.tencent.bk.devops.git.core.enums.PullType
import com.tencent.bk.devops.git.core.exception.ParamInvalidException
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.pojo.input.GitCodeCommandAtomParamInput
import com.tencent.bk.devops.git.core.service.auth.AuthUserTokenGitAuthProvider
import com.tencent.bk.devops.git.core.service.auth.CredentialGitAuthProvider
import com.tencent.bk.devops.git.core.service.auth.EmptyGitAuthProvider
import com.tencent.bk.devops.git.core.service.auth.OauthGitAuthProvider
import com.tencent.bk.devops.git.core.service.auth.PrivateGitAuthProvider
import com.tencent.bk.devops.git.core.service.auth.UserNamePasswordGitAuthProvider
import com.tencent.bk.devops.git.core.service.auth.UserTokenGitAuthProvider
import com.tencent.bk.devops.git.core.service.helper.IInputAdapter
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.GitUtil
import java.io.File

class GitCodeCommandAtomParamInputAdapter(
    private val input: GitCodeCommandAtomParamInput
) : IInputAdapter {

    companion object {
        private val devopsApi = DevopsApi()
    }

    @Suppress("ALL")
    override fun getInputs(): GitSourceSettings {
        with(input) {
            if (repositoryUrl.isBlank()) {
                throw ParamInvalidException(errorMsg = "代码库url不能为空")
            }

            // 获取鉴权信息,post action阶段不需要查询凭证
            val authProvider = if (postEntryParam == "True") {
                EmptyGitAuthProvider()
            } else {
                when (authType) {
                    AuthType.ACCESS_TOKEN -> OauthGitAuthProvider(token = accessToken)
                    AuthType.USERNAME_PASSWORD -> UserNamePasswordGitAuthProvider(
                        username = username,
                        password = password
                    )
                    AuthType.TICKET -> CredentialGitAuthProvider(
                        credentialId = ticketId,
                        devopsApi = devopsApi
                    )
                    AuthType.START_USER_TOKEN -> UserTokenGitAuthProvider(
                        userId = pipelineStartUserName,
                        devopsApi = devopsApi
                    )
                    AuthType.PERSONAL_ACCESS_TOKEN -> PrivateGitAuthProvider(
                        token = personalAccessToken
                    )
                    AuthType.AUTH_USER_TOKEN ->
                        AuthUserTokenGitAuthProvider(
                            pipelineStartUserName = pipelineStartUserName,
                            userId = authUserId,
                            repositoryUrl = repositoryUrl,
                            devopsApi = devopsApi
                        )
                    else -> EmptyGitAuthProvider()
                }
            }
            val authInfo = authProvider.getAuthInfo()

            var ref: String = refName
            val preMerge = GitUtil.isEnablePreMerge(
                enableVirtualMergeBranch = enableVirtualMergeBranch,
                repositoryUrl = repositoryUrl,
                hookEventType = hookEventType,
                hookTargetUrl = hookTargetUrl,
                compatibleHostList = hostNameList
            )
            if (preMerge) {
                ref = hookTargetBranch!!
                pullType = PullType.BRANCH.name
            }

            EnvHelper.addEnvVariable(GitConstants.BK_CI_GIT_REPO_CODE_PATH, localPath ?: "")
            EnvHelper.addEnvVariable(
                key = GitConstants.BK_CI_GIT_REPO_ALIAS_NAME,
                value = GitUtil.getServerInfo(repositoryUrl).repositoryName
            )
            EnvHelper.addEnvVariable(BK_CI_GIT_REPO_REF, refName)
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

            return GitSourceSettings(
                bkWorkspace = bkWorkspace,
                pipelineId = pipelineId,
                pipelineTaskId = pipelineTaskId,
                pipelineBuildId = pipelineBuildId,
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
                fetchDepth = fetchDepth ?: 0,
                enableFetchRefSpec = enableFetchRefSpec,
                fetchRefSpec = fetchRefSpec,
                lfs = enableGitLfs,
                lfsConcurrentTransfers = lfsConcurrentTransfers,
                submodules = enableSubmodule,
                nestedSubmodules = enableSubmoduleRecursive ?: true,
                submoduleRemote = enableSubmoduleRemote,
                submodulesPath = submodulePath ?: "",
                includeSubPath = includePath,
                excludeSubPath = excludePath,
                username = authInfo.username,
                password = authInfo.password,
                privateKey = authInfo.privateKey,
                passPhrase = authInfo.passPhrase,
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
                cachePath = cachePath
            )
        }
    }

    private fun getAuthType(authType: AuthType?, repositoryUrl: String): String {
        return when {
            authType == AuthType.ACCESS_TOKEN -> "OAUTH"
            authType == AuthType.USERNAME_PASSWORD -> "HTTP"
            repositoryUrl.toUpperCase().startsWith("HTTP") -> "HTTP"
            else ->
                "SSH"
        }
    }
}
