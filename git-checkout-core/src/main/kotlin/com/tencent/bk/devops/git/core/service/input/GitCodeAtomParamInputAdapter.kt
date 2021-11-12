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
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_BUILD_JOB_ID
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_ALIAS_NAME
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_BRANCH
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_CODE_PATH
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_TAG
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_GIT_REPO_ALIAS_NAME
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_GIT_REPO_BRANCH
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_GIT_REPO_CODE_PATH
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_GIT_REPO_NAME
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_GIT_REPO_URL
import com.tencent.bk.devops.git.core.constant.GitConstants.PIPELINE_MATERIAL_ALIASNAME
import com.tencent.bk.devops.git.core.constant.GitConstants.PIPELINE_MATERIAL_BRANCHNAME
import com.tencent.bk.devops.git.core.constant.GitConstants.PIPELINE_MATERIAL_URL
import com.tencent.bk.devops.git.core.enums.PullStrategy
import com.tencent.bk.devops.git.core.enums.PullType
import com.tencent.bk.devops.git.core.exception.ParamInvalidException
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.pojo.api.RepositoryType
import com.tencent.bk.devops.git.core.pojo.input.GitCodeAtomParamInput
import com.tencent.bk.devops.git.core.service.auth.EmptyGitAuthProvider
import com.tencent.bk.devops.git.core.service.auth.RepositoryGitAuthProvider
import com.tencent.bk.devops.git.core.service.helper.IInputAdapter
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.GitUtil
import com.tencent.bk.devops.git.core.util.RepositoryUtils
import org.slf4j.LoggerFactory
import java.io.File

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
            val repository = devopsApi.getRepository(repositoryConfig).data
                ?: throw ParamInvalidException(
                    errorMsg = "repository ${repositoryConfig.getRepositoryId()} is not found"
                )
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
            val preMerge = GitUtil.isEnablePreMerge(
                enableVirtualMergeBranch = enableVirtualMergeBranch,
                repositoryUrl = repository.url,
                hookEventType = hookEventType,
                hookTargetUrl = hookTargetUrl,
                compatibleHostList = compatibleHostList
            )
            if (preMerge) {
                ref = hookTargetBranch!!
                pullType = PullType.BRANCH.name
            }

            // 4. 得到授权信息,post action阶段不需要查询凭证
            val authProvider = if (postEntryParam == "True") {
                EmptyGitAuthProvider()
            } else {
                RepositoryGitAuthProvider(
                    repository = repository,
                    devopsApi = devopsApi
                )
            }
            val authInfo = authProvider.getAuthInfo()

            // 5. 导入输入的参数到环境变量
            EnvHelper.addEnvVariable(BK_CI_GIT_REPO_ALIAS_NAME, repository.aliasName)
            EnvHelper.addEnvVariable(BK_CI_GIT_REPO_CODE_PATH, localPath ?: "")
            EnvHelper.addEnvVariable(BK_CI_GIT_REPO_BRANCH, branchName)
            EnvHelper.addEnvVariable(BK_CI_GIT_REPO_TAG, tagName ?: "")

            EnvHelper.addEnvVariable(DEVOPS_GIT_REPO_URL, repository.url)
            EnvHelper.addEnvVariable(DEVOPS_GIT_REPO_NAME, repository.projectName)
            EnvHelper.addEnvVariable(DEVOPS_GIT_REPO_ALIAS_NAME, repository.aliasName)
            EnvHelper.addEnvVariable(DEVOPS_GIT_REPO_CODE_PATH, localPath ?: "")
            EnvHelper.addEnvVariable(DEVOPS_GIT_REPO_BRANCH, branchName)

            EnvHelper.addEnvVariable("$PIPELINE_MATERIAL_URL.${repositoryConfig.getRepositoryId()}", repository.url)
            EnvHelper.addEnvVariable("$PIPELINE_MATERIAL_BRANCHNAME.${repositoryConfig.getRepositoryId()}", ref)
            EnvHelper.addEnvVariable(
                key = "$PIPELINE_MATERIAL_ALIASNAME.${repositoryConfig.getRepositoryId()}",
                value = repository.aliasName
            )

            // 添加代码库信息支持codecc扫描
            EnvHelper.addEnvVariable("bk_repo_taskId_${input.pipelineTaskId}", input.pipelineTaskId)
            EnvHelper.addEnvVariable("bk_repo_hashId_${input.pipelineTaskId}", input.repositoryHashId ?: "")
            EnvHelper.addEnvVariable("bk_repo_name_${input.pipelineTaskId}", input.repositoryName ?: "")
            EnvHelper.addEnvVariable("bk_repo_config_type_${input.pipelineTaskId}", input.repositoryType)
            EnvHelper.addEnvVariable("bk_repo_type_${input.pipelineTaskId}", "GIT")
            EnvHelper.addEnvVariable("bk_repo_local_path_${input.pipelineTaskId}", input.localPath ?: "")
            EnvHelper.addEnvVariable("bk_repo_container_id_${input.pipelineTaskId}", System.getenv(BK_CI_BUILD_JOB_ID))

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
                enableGitClean = enableGitClean,
                enableGitCleanIgnore = enableGitCleanIgnore,
                fetchDepth = fetchDepth ?: 0,
                enableFetchRefSpec = enableFetchRefSpec,
                fetchRefSpec = fetchRefSpec,
                lfs = enableGitLfs,
                submodules = enableSubmodule,
                nestedSubmodules = enableSubmoduleRecursive ?: true,
                submodulesPath = submodulePath ?: "",
                submoduleRemote = enableSubmoduleRemote,
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
                compatibleHostList = compatibleHostList,
                enableTrace = enableTrace,
                enablePartialClone = enablePartialClone,
                cachePath = cachePath
            )
        }
    }
}
