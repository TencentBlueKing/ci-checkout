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

package com.tencent.devops.git.service.input

import com.tencent.devops.git.api.DevopsApi
import com.tencent.devops.git.constant.GitConstants.BK_CI_GIT_REPO_ALIAS_NAME
import com.tencent.devops.git.constant.GitConstants.BK_CI_GIT_REPO_CODE_PATH
import com.tencent.devops.git.enums.CodeEventType
import com.tencent.devops.git.enums.PullStrategy
import com.tencent.devops.git.enums.PullType
import com.tencent.devops.git.exception.ApiException
import com.tencent.devops.git.exception.ParamInvalidException
import com.tencent.devops.git.pojo.GitSourceSettings
import com.tencent.devops.git.pojo.api.RepositoryType
import com.tencent.devops.git.pojo.input.GitCodeAtomParamInput
import com.tencent.devops.git.service.auth.RepositoryGitAuthProvider
import com.tencent.devops.git.service.helper.IInputAdapter
import com.tencent.devops.git.util.EnvHelper
import com.tencent.devops.git.util.GitUtil
import com.tencent.devops.git.util.RepositoryUtils
import java.io.File
import org.slf4j.LoggerFactory

class GitCodeAtomParamInputAdapter(
    private val input: GitCodeAtomParamInput
) : IInputAdapter {

    companion object {
        private val devopsApi = DevopsApi()
        private val logger = LoggerFactory.getLogger(GitCodeAtomParamInputAdapter::class.java)
    }

    @Suppress("ALL")
    override fun getInputs(): GitSourceSettings {
        with(input) {
            // 1. 获取仓库信息
            val repositoryType = RepositoryType.valueOf(repositoryType)
            val repositoryId = when (repositoryType) {
                RepositoryType.ID -> repositoryHashId
                    ?: throw ParamInvalidException(errorMsg = "代码库ID不能为空")
                RepositoryType.NAME -> repositoryName
                ?: throw ParamInvalidException(errorMsg = "代码库名称不能为空")
                else ->
                    throw ParamInvalidException(errorMsg = "代码库类型错误")
            }
            val repositoryConfig = RepositoryUtils.buildConfig(repositoryId, repositoryType)
            val repository = try {
                devopsApi.getRepository(repositoryConfig).data
            } catch (e: ApiException) {
                if (e.httpStatus == 404) {
                    throw ApiException(
                        errorMsg = "代码库${repositoryConfig.getRepositoryId()}不存在或已删除，" +
                            "请联系当前流水线的管理人员检查代码库信息是否正确"
                    )
                }
                throw e
            } ?: throw ParamInvalidException(errorMsg = "repository ${repositoryConfig.getRepositoryId()} is not found")
            logger.info("get the repo:$repository")

            // 2. 确定分支和commit
            var ref: String = when (pullType) {
                PullType.BRANCH.name ->
                    branchName
                PullType.TAG.name -> {
                    if (tagName.isNullOrBlank()) {
                        throw ParamInvalidException(errorMsg = "拉取方式是TAG,tag名不能为空")
                    }
                    tagName!!
                }
                PullType.COMMIT_ID.name -> {
                    if (commitId.isNullOrBlank()) {
                        throw ParamInvalidException(errorMsg = "拉取方式是commitId,commitId名不能为空")
                    }
                    commitId!!
                }
                else ->
                    throw ParamInvalidException(errorMsg = "拉取方式只能是BRANCH/TAG/COMMIT_ID")
            }

            // 3. 确定是否开启pre-merge功能
            /**
             * 开启pre-merge需要满足以下条件
             * 1. 插件启用pre-merge功能
             * 2. 触发方式是webhook触发
             * 3. 触发的url与插件配置的url要是同一个仓库
             * 4. 触发的事件类型必须是mr/pr
             */
            val preMerge = enableVirtualMergeBranch &&
                GitUtil.isSameRepository(
                    repositoryUrl = repository.url,
                    otherRepositoryUrl = hookTargetUrl,
                    hostNameList = compatibleHostList
                ) &&
                (hookEventType == CodeEventType.PULL_REQUEST.name || hookEventType == CodeEventType.MERGE_REQUEST.name)
            if (preMerge) {
                ref = hookTargetBranch!!
                pullType = PullType.BRANCH.name
            }

            // 4. 得到授权信息
            val authInfo = RepositoryGitAuthProvider(
                repository = repository,
                devopsApi = devopsApi
            ).getAuthInfo()

            // 5. 导入输入的参数到环境变量
            EnvHelper.addEnvVariable(BK_CI_GIT_REPO_ALIAS_NAME, repository.aliasName)
            EnvHelper.addEnvVariable(BK_CI_GIT_REPO_CODE_PATH, localPath ?: "")

            return GitSourceSettings(
                bkWorkspace = bkWorkspace,
                pipelineId = pipelineId,
                pipelineTaskId = pipelineTaskId,
                pipelineBuildId = pipelineBuildId,
                postEntryParam = postEntryParam,
                scmType = RepositoryUtils.getScmType(repository),
                repositoryUrl = repository.url,
                repositoryPath = if (localPath.isNullOrBlank()) {
                    File(bkWorkspace)
                } else {
                    File(bkWorkspace, localPath!!)
                }.absolutePath,
                ref = ref,
                pullType = PullType.valueOf(pullType),
                commit = retryStartPoint ?: "",
                pullStrategy = PullStrategy.valueOf(strategy),
                clean = enableGitClean,
                fetchDepth = fetchDepth ?: 0,
                fetchOnlyCurrentRef = fetchOnlyCurrentRef,
                lfs = enableGitLfs,
                submodules = enableSubmodule,
                nestedSubmodules = enableSubmoduleRecursive ?: true,
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
                usernameConfig = pipelineStartUserName,
                compatibleHostList = compatibleHostList,
                enableTrace = enableTrace
            )
        }
    }
}
