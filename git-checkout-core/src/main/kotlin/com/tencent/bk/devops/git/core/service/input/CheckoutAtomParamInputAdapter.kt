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

import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_HOOK_BRANCH
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_HOOK_REVISION
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_HOOK_TARGET_BRANCH
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_REPO_GITHUB_WEBHOOK_CREATE_REF_NAME
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_REPO_GITHUB_WEBHOOK_CREATE_REF_TYPE
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_REPO_GIT_WEBHOOK_TAG_NAME
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_REPO_WEB_HOOK_HASHID
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_START_TYPE
import com.tencent.bk.devops.git.core.enums.CodeEventType
import com.tencent.bk.devops.git.core.enums.PullType
import com.tencent.bk.devops.git.core.enums.StartType
import com.tencent.bk.devops.git.core.exception.ParamInvalidException
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.pojo.api.RepositoryType
import com.tencent.bk.devops.git.core.pojo.input.CheckoutAtomParamInput
import com.tencent.bk.devops.git.core.pojo.input.GitCodeAtomParamInput
import com.tencent.bk.devops.git.core.pojo.input.GitCodeCommandAtomParamInput
import com.tencent.bk.devops.git.core.service.helper.IInputAdapter
import com.tencent.bk.devops.git.core.util.EnvHelper

class CheckoutAtomParamInputAdapter(
    private val input: CheckoutAtomParamInput
) : IInputAdapter {

    override fun getInputs(): GitSourceSettings {
        EnvHelper.addEnvVariable(GitConstants.BK_CI_GIT_REPO_TYPE, input.repositoryType)
        return when (RepositoryType.valueOf(input.repositoryType)) {
            RepositoryType.SELF -> {
                handleSelfParam()
                input.byRepositoryIdOrName()
            }
            RepositoryType.ID, RepositoryType.NAME -> {
                input.byRepositoryIdOrName()
            }
            RepositoryType.URL -> {
                input.byRepositoryUrl()
            }
        }
    }

    private fun CheckoutAtomParamInput.byRepositoryIdOrName() = GitCodeAtomParamInputAdapter(
        GitCodeAtomParamInput(
            bkWorkspace = bkWorkspace,
            pipelineId = pipelineId,
            pipelineTaskId = pipelineTaskId,
            pipelineBuildId = pipelineBuildId,
            pipelineStartUserName = pipelineStartUserName,
            postEntryParam = postEntryParam,

            repositoryType = repositoryType,
            repositoryHashId = repositoryHashId,
            repositoryName = repositoryName,
            localPath = localPath,
            strategy = strategy,
            enableSubmodule = enableSubmodule,
            submodulePath = submodulePath,
            enableVirtualMergeBranch = enableVirtualMergeBranch,
            enableSubmoduleRemote = enableSubmoduleRemote,
            enableSubmoduleRecursive = enableSubmoduleRecursive,
            submoduleDepth = submoduleDepth,
            submoduleJobs = submoduleJobs,
            autoCrlf = autoCrlf,
            pullType = pullType,
            branchName = refName,
            tagName = refName,
            commitId = refName,
            includePath = includePath,
            excludePath = excludePath,
            fetchDepth = fetchDepth,
            enableFetchRefSpec = enableFetchRefSpec,
            fetchRefSpec = fetchRefSpec,
            enableGitClean = enableGitClean,
            enableGitCleanIgnore = enableGitCleanIgnore,
            enableGitCleanNested = enableGitCleanNested,
            enableGitLfs = enableGitLfs,
            lfsConcurrentTransfers = lfsConcurrentTransfers,
            enableGitLfsClean = enableGitLfsClean,
            pipelineStartType = pipelineStartType,
            hookEventType = hookEventType,
            hookSourceBranch = hookSourceBranch,
            hookTargetBranch = hookTargetBranch,
            hookSourceUrl = hookSourceUrl,
            hookTargetUrl = hookTargetUrl,
            retryStartPoint = retryStartPoint,
            persistCredentials = persistCredentials,
            compatibleHostList = compatibleHostList,
            enableTrace = enableTrace,
            usernameConfig = usernameConfig,
            userEmailConfig = userEmailConfig,
            enablePartialClone = enablePartialClone,
            cachePath = cachePath,
            enableGlobalInsteadOf = true,
            useCustomCredential = true
        )
    ).getInputs()

    @SuppressWarnings("LongMethod")
    private fun CheckoutAtomParamInput.byRepositoryUrl() = GitCodeCommandAtomParamInputAdapter(
        GitCodeCommandAtomParamInput(
            bkWorkspace = bkWorkspace,
            pipelineId = pipelineId,
            pipelineTaskId = pipelineTaskId,
            pipelineBuildId = pipelineBuildId,
            pipelineStartUserName = pipelineStartUserName,
            postEntryParam = postEntryParam,

            repositoryUrl = repositoryUrl,
            scmType = scmType,
            authType = authType,
            ticketId = ticketId,
            accessToken = accessToken,
            username = username,
            password = password,
            personalAccessToken = personalAccessToken,
            authUserId = authUserId,
            localPath = localPath,
            strategy = strategy,
            enableSubmodule = enableSubmodule,
            submodulePath = submodulePath,
            enableVirtualMergeBranch = enableVirtualMergeBranch,
            enableSubmoduleRemote = enableSubmoduleRemote,
            enableSubmoduleRecursive = enableSubmoduleRecursive,
            submoduleDepth = submoduleDepth,
            submoduleJobs = submoduleJobs,
            autoCrlf = autoCrlf,
            pullType = pullType,
            refName = refName,
            includePath = includePath,
            excludePath = excludePath,
            fetchDepth = fetchDepth,
            enableFetchRefSpec = enableFetchRefSpec,
            fetchRefSpec = fetchRefSpec,
            enableGitClean = enableGitClean,
            enableGitCleanIgnore = enableGitCleanIgnore,
            enableGitCleanNested = enableGitCleanNested,
            enableGitLfs = enableGitLfs,
            lfsConcurrentTransfers = lfsConcurrentTransfers,
            enableGitLfsClean = enableGitLfsClean,
            pipelineStartType = pipelineStartType,
            hookEventType = hookEventType,
            hookSourceBranch = hookSourceBranch,
            hookTargetBranch = hookTargetBranch,
            hookSourceUrl = hookSourceUrl,
            hookTargetUrl = hookTargetUrl,
            retryStartPoint = retryStartPoint,
            persistCredentials = persistCredentials,
            hostNameList = compatibleHostList,
            enableTrace = enableTrace,
            usernameConfig = usernameConfig,
            userEmailConfig = userEmailConfig,
            enablePartialClone = enablePartialClone,
            cachePath = cachePath,
            enableGlobalInsteadOf = true,
            useCustomCredential = true
        )
    ).getInputs()

    private fun handleSelfParam() {
        val startType = System.getenv(BK_CI_START_TYPE)
        when (startType) {
            StartType.WEB_HOOK.name -> {
                input.repositoryHashId = System.getenv(BK_CI_REPO_WEB_HOOK_HASHID)
                    ?: throw ParamInvalidException(errorMsg = "repository hash id is empty")
                val gitHookEventType = System.getenv(GitConstants.BK_CI_REPO_GIT_WEBHOOK_EVENT_TYPE) ?: ""
                input.refName = when (gitHookEventType) {
                    CodeEventType.PUSH.name, CodeEventType.ISSUES.name,
                    CodeEventType.NOTE.name, CodeEventType.REVIEW.name -> {
                        input.pullType = PullType.BRANCH.name
                        System.getenv(BK_CI_HOOK_BRANCH) ?: "master"
                    }

                    CodeEventType.MERGE_REQUEST.name, CodeEventType.MERGE_REQUEST_ACCEPT.name,
                    CodeEventType.PULL_REQUEST.name -> {
                        input.pullType = PullType.BRANCH.name
                        System.getenv(BK_CI_HOOK_TARGET_BRANCH) ?: "master"
                    }

                    CodeEventType.TAG_PUSH.name -> {
                        input.pullType = PullType.TAG.name
                        System.getenv(BK_CI_REPO_GIT_WEBHOOK_TAG_NAME) ?: ""
                    }

                    CodeEventType.CREATE.name -> {
                        // 创建要素类型，branch 或 tag
                        val createRefType =
                            System.getenv(BK_CI_REPO_GITHUB_WEBHOOK_CREATE_REF_TYPE) ?: "branch"
                        input.pullType = when (createRefType) {
                            "tag" -> PullType.TAG.name
                            else -> PullType.BRANCH.name
                        }
                        System.getenv(BK_CI_REPO_GITHUB_WEBHOOK_CREATE_REF_NAME) ?: "master"
                    }

                    else -> ""
                }
            }
            // 非webhook触发则拉取当前PAC流水线关联代码库的默认分支
            else -> {
                input.repositoryHashId = System.getenv(BK_CI_REPO_WEB_HOOK_HASHID)
                    ?: throw ParamInvalidException(errorMsg = "repository hash id is empty")
                input.pullType = PullType.BRANCH.name
                input.refName = System.getenv(BK_CI_HOOK_BRANCH) ?: "master"
            }
        }
    }
}
