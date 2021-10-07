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
import com.tencent.bk.devops.git.core.enums.AuthType
import com.tencent.bk.devops.git.core.enums.CodeEventType
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

            /**
             * 开启pre-merge需要满足以下条件
             * 1. 插件启用pre-merge功能
             * 2. 触发方式是webhook触发
             * 3. 触发的url与插件配置的url要是同一个仓库
             * 4. 触发的事件类型必须是mr/pr
             */
            var ref: String = refName
            val preMerge = enableVirtualMergeBranch &&
                GitUtil.isSameRepository(
                    repositoryUrl = repositoryUrl,
                    otherRepositoryUrl = hookTargetUrl,
                    hostNameList = hostNameList
                ) &&
                (hookEventType == CodeEventType.PULL_REQUEST.name || hookEventType == CodeEventType.MERGE_REQUEST.name)
            if (preMerge) {
                ref = hookTargetBranch!!
                pullType = PullType.BRANCH.name
            }

            EnvHelper.addEnvVariable(GitConstants.BK_CI_GIT_REPO_CODE_PATH, localPath ?: "")

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
}
