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
import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_REPOSITORY_ALIAS_NAME
import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_REPOSITORY_HASH_ID
import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_REPOSITORY_TYPE
import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_BUILD_JOB_ID
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_BRANCHES
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_CODE_PATHS
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_ALIAS_NAME
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_BRANCH
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_CODE_PATH
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_EXCLUDE_PATH
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_INCLUDE_PATH
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_TAG
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_URLS
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_GIT_BRANCHES
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_GIT_CODE_PATHS
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_GIT_REPO_ALIAS_NAME
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_GIT_REPO_BRANCH
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_GIT_REPO_CODE_PATH
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_GIT_REPO_NAME
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_GIT_REPO_URL
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_GIT_URLS
import com.tencent.bk.devops.git.core.constant.GitConstants.PARAM_SEPARATOR
import com.tencent.bk.devops.git.core.enums.PullStrategy
import com.tencent.bk.devops.git.core.enums.PullType
import com.tencent.bk.devops.git.core.enums.ScmType
import com.tencent.bk.devops.git.core.exception.ParamInvalidException
import com.tencent.bk.devops.git.core.pojo.AuthInfo
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.pojo.api.Repository
import com.tencent.bk.devops.git.core.pojo.api.RepositoryType
import com.tencent.bk.devops.git.core.pojo.input.GitCodeAtomParamInput
import com.tencent.bk.devops.git.core.service.auth.EmptyGitAuthProvider
import com.tencent.bk.devops.git.core.service.auth.RepositoryGitAuthProvider
import com.tencent.bk.devops.git.core.service.auth.UserTokenGitAuthProvider
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
                RepositoryType.ID, RepositoryType.SELF -> {
                    repositoryHashId ?: throw ParamInvalidException(errorMsg = "Repository ID cannot be empty")
                    EnvHelper.addEnvVariable(GitConstants.BK_CI_GIT_REPO_ID, repositoryHashId!!)
                    repositoryHashId
                }
                RepositoryType.NAME -> {
                    repositoryName ?: throw ParamInvalidException(errorMsg = "Repository name cannot be empty")
                    EnvHelper.addEnvVariable(GitConstants.BK_CI_GIT_REPO_NAME, repositoryName!!)
                    repositoryName
                }
                else ->
                    throw ParamInvalidException(errorMsg = "repository type not found")
            }
            val repositoryConfig = RepositoryUtils.buildConfig(repositoryId!!, repositoryType)
            val repository = devopsApi.getRepository(repositoryConfig).data
                ?: throw ParamInvalidException(
                    errorMsg = "repository ${repositoryConfig.getRepositoryId()} is not found"
                )
            EnvHelper.putContext(CONTEXT_REPOSITORY_HASH_ID, repository.repoHashId ?: "")
            logger.info("get the repo:$repository")
            // 2. 确定分支和commit
            var ref: String = getRef()
            EnvHelper.addEnvVariable(GitConstants.BK_CI_GIT_REPO_REF, ref)

            val scmType = RepositoryUtils.getScmType(repository)
            // 3. 确定是否开启pre-merge功能
            val preMerge = GitUtil.isEnablePreMerge(
                enableVirtualMergeBranch = enableVirtualMergeBranch,
                repositoryUrl = repository.url,
                hookEventType = hookEventType,
                hookTargetUrl = hookTargetUrl,
                compatibleHostList = compatibleHostList,
                scmType = scmType
            )
            // fork库凭证信息
            var forkRepoAuthInfo: AuthInfo? = null
            if (preMerge) {
                ref = hookTargetBranch!!
                pullType = PullType.BRANCH.name
                forkRepoAuthInfo = getForkRepoAuthInfo(scmType = scmType, repositoryUrl = repository.url)
            }

            // 4. 得到授权信息,post action阶段不需要查询凭证
            val authProvider = getAuthProvider(repository)
            // 主库凭证信息
            val authInfo = authProvider.getAuthInfo()
            // 保存代码库相关信息
            val gitProjectId = RepositoryUtils.getGitProjectId(repository, authInfo)
            EnvHelper.addEnvVariable(GitConstants.BK_CI_GIT_PROJECT_ID, "$gitProjectId")
            // 5. 导入输入的参数到环境变量
            EnvHelper.addEnvVariable(BK_CI_GIT_REPO_ALIAS_NAME, repository.aliasName)
            EnvHelper.putContext(CONTEXT_REPOSITORY_ALIAS_NAME, repository.aliasName)
            // 兼容其他插件使用【BK_CI_GIT_REPO_TYPE】变量，统一设置为ID
            EnvHelper.addEnvVariable(GitConstants.BK_CI_GIT_REPO_TYPE, repositoryType.let {
                if (it == RepositoryType.SELF) RepositoryType.ID else it
            }.name)
            EnvHelper.putContext(CONTEXT_REPOSITORY_TYPE, repositoryType.let {
                if (it == RepositoryType.SELF) RepositoryType.ID else it
            }.name)
            EnvHelper.addEnvVariable(BK_CI_GIT_REPO_CODE_PATH, localPath ?: "")
            EnvHelper.addEnvVariable(BK_CI_GIT_REPO_BRANCH, branchName)
            EnvHelper.addEnvVariable(BK_CI_GIT_REPO_TAG, tagName ?: "")

            EnvHelper.addEnvVariable(DEVOPS_GIT_REPO_URL, repository.url)
            EnvHelper.addEnvVariable(DEVOPS_GIT_REPO_NAME, repository.projectName)
            EnvHelper.addEnvVariable(DEVOPS_GIT_REPO_ALIAS_NAME, repository.aliasName)
            EnvHelper.addEnvVariable(DEVOPS_GIT_REPO_CODE_PATH, localPath ?: "")
            EnvHelper.addEnvVariable(DEVOPS_GIT_REPO_BRANCH, branchName)
            EnvHelper.addEnvVariable(BK_CI_GIT_REPO_INCLUDE_PATH, input.includePath ?: "")
            EnvHelper.addEnvVariable(BK_CI_GIT_REPO_EXCLUDE_PATH, input.excludePath ?: "")

            // 添加代码库信息支持codecc扫描
            EnvHelper.addEnvVariable("bk_repo_taskId_${input.pipelineTaskId}", input.pipelineTaskId)
            EnvHelper.addEnvVariable("bk_repo_hashId_${input.pipelineTaskId}", input.repositoryHashId ?: "")
            EnvHelper.addEnvVariable("bk_repo_name_${input.pipelineTaskId}", input.repositoryName ?: "")
            // codecc没有SELF模式，此处做兼容
            EnvHelper.addEnvVariable("bk_repo_config_type_${input.pipelineTaskId}", repositoryType.let {
                if (it == RepositoryType.SELF) RepositoryType.ID else it
            }.name)
            EnvHelper.addEnvVariable("bk_repo_type_${input.pipelineTaskId}", "GIT")
            EnvHelper.addEnvVariable("bk_repo_local_path_${input.pipelineTaskId}", input.localPath ?: "")
            EnvHelper.addEnvVariable("bk_repo_container_id_${input.pipelineTaskId}", System.getenv(BK_CI_BUILD_JOB_ID))
            EnvHelper.addEnvVariable("bk_repo_include_path_${input.pipelineTaskId}", input.includePath ?: "")
            EnvHelper.addEnvVariable("bk_repo_exclude_path_${input.pipelineTaskId}", input.excludePath ?: "")
            EnvHelper.addEnvVariable("bk_repo_git_project_id_${input.pipelineTaskId}", "$gitProjectId")

            val devopsGitUrls = System.getenv(BK_CI_GIT_URLS)
            val gitUrls = if (devopsGitUrls.isNullOrBlank()) {
                repository.url
            } else {
                devopsGitUrls + PARAM_SEPARATOR + repository.url
            }
            EnvHelper.addEnvVariable(
                key = BK_CI_GIT_URLS,
                value = gitUrls
            )
            EnvHelper.addEnvVariable(
                key = DEVOPS_GIT_URLS,
                value = gitUrls
            )

            val devopsGitBranches = System.getenv(BK_CI_GIT_BRANCHES)
            val gitBranches = if (devopsGitBranches.isNullOrBlank()) {
                branchName
            } else {
                devopsGitBranches + PARAM_SEPARATOR + branchName
            }
            EnvHelper.addEnvVariable(
                key = DEVOPS_GIT_BRANCHES,
                value = gitBranches
            )
            EnvHelper.addEnvVariable(
                key = BK_CI_GIT_BRANCHES,
                value = gitBranches
            )

            val devopsGitCodePaths = System.getenv(DEVOPS_GIT_CODE_PATHS)
            val gitCodePaths = if (devopsGitCodePaths.isNullOrBlank()) {
                localPath ?: ""
            } else {
                devopsGitCodePaths + PARAM_SEPARATOR + (localPath ?: "")
            }
            EnvHelper.addEnvVariable(
                key = DEVOPS_GIT_CODE_PATHS,
                value = gitCodePaths
            )
            EnvHelper.addEnvVariable(
                key = BK_CI_GIT_CODE_PATHS,
                value = gitCodePaths
            )

            return GitSourceSettings(
                bkWorkspace = bkWorkspace,
                pipelineId = pipelineId,
                pipelineTaskId = pipelineTaskId,
                pipelineBuildId = pipelineBuildId,
                pipelineStartUserName = pipelineStartUserName,
                postEntryParam = postEntryParam,
                scmType = scmType,
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
                enableGitCleanNested = enableGitCleanNested,
                fetchDepth = fetchDepth ?: 0,
                enableFetchRefSpec = enableFetchRefSpec,
                fetchRefSpec = fetchRefSpec,
                lfs = enableGitLfs,
                lfsConcurrentTransfers = lfsConcurrentTransfers,
                enableGitLfsClean = enableGitLfsClean,
                submodules = enableSubmodule,
                nestedSubmodules = enableSubmoduleRecursive ?: true,
                submodulesPath = submodulePath ?: "",
                submoduleRemote = enableSubmoduleRemote,
                includeSubPath = includePath,
                excludeSubPath = excludePath,
                submoduleDepth = submoduleDepth,
                submoduleJobs = submoduleJobs,
                authInfo = authInfo,
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

    /**
     * 获取代码库授权提供者
     */

    private fun getAuthProvider(repository: Repository) = with(input) {
        if (postEntryParam == "True") {
            EmptyGitAuthProvider()
        } else {
            RepositoryGitAuthProvider(
                repository = repository,
                devopsApi = devopsApi
            )
        }
    }

    /**
     * 获取fork仓库授权信息
     * 1.post阶段不获取
     * 2.非git、github代码库不获取，后续支持tgit
     * 3.不是fork仓库的，源仓库和目标仓库是同一个仓库
     */
    private fun getForkRepoAuthInfo(
        scmType: ScmType,
        repositoryUrl: String
    ) = with(input) {
        if (postEntryParam == "True" ||
            !listOf(ScmType.CODE_GIT, ScmType.GITHUB).contains(scmType) ||
            GitUtil.isSameRepository(
                repositoryUrl = repositoryUrl,
                otherRepositoryUrl = hookTargetUrl,
                hostNameList = compatibleHostList
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

    /**
     * 获取引用，确定分支和commit
     */
    private fun getRef() = with(input) {
        when (pullType) {
            PullType.BRANCH.name ->
                branchName

            PullType.TAG.name -> {
                if (tagName.isNullOrBlank()) {
                    throw ParamInvalidException(
                        errorMsg = "The pull type is TAG, and the tag name cannot be empty"
                    )
                }
                tagName!!
            }
            PullType.COMMIT_ID.name -> {
                if (commitId.isNullOrBlank()) {
                    throw ParamInvalidException(
                        errorMsg = "The pull type is COMMIT, and the commit_id name cannot be empty"
                    )
                }
                commitId!!
            }
            else ->
                throw ParamInvalidException(errorMsg = "The pull method can only be BRANCH/TAG/COMMIT_ID")
        }
    }
}
