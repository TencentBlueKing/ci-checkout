package com.tencent.devops.git

import com.tencent.bk.devops.atom.AtomContext
import com.tencent.bk.devops.atom.spi.AtomService
import com.tencent.bk.devops.atom.spi.TaskAtom
import com.tencent.bk.devops.git.core.GitCheckoutRunner
import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.enums.ScmType
import com.tencent.bk.devops.git.core.pojo.api.RepositoryType
import com.tencent.bk.devops.git.core.pojo.input.CheckoutAtomParamInput
import com.tencent.bk.devops.git.core.service.input.CheckoutAtomParamInputAdapter
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.GitUtil
import com.tencent.devops.git.pojo.CheckoutAtomParam

@AtomService(paramClass = CheckoutAtomParam::class)
class CheckoutAtom : TaskAtom<CheckoutAtomParam> {
    override fun execute(context: AtomContext<CheckoutAtomParam>) {
        val inputAdapter = getInputAdapter(context)
        GitCheckoutRunner().run(inputAdapter = inputAdapter, atomContext = context)
    }

    @SuppressWarnings("LongMethod")
    private fun getInputAdapter(context: AtomContext<CheckoutAtomParam>) =
        CheckoutAtomParamInputAdapter(input = with(context.param) {
            // 配置环境变量
            EnvHelper.addEnvVariable(GitConstants.BK_CI_GIT_REPO_REF, refName)
            CheckoutAtomParamInput(
                bkWorkspace = bkWorkspace,
                pipelineId = pipelineId,
                pipelineTaskId = pipelineTaskId,
                pipelineBuildId = pipelineBuildId,
                pipelineStartUserName = pipelineStartUserName,
                postEntryParam = postEntryParam,
                repositoryType = repositoryType,
                scmType = getScmTypeByRepoUrl(repositoryUrl, repositoryType),
                repositoryHashId = repositoryHashId,
                repositoryName = repositoryName,
                repositoryUrl = repositoryUrl,
                accessToken = accessToken,
                authType = authType,
                ticketId = ticketId,
                username = username,
                password = password,
                personalAccessToken = personalAccessToken,
                authUserId = authUserId,
                localPath = localPath,
                strategy = strategy,
                pullType = pullType,
                refName = refName,
                fetchDepth = fetchDepth,
                enableFetchRefSpec = enableFetchRefSpec,
                fetchRefSpec = fetchRefSpec,
                enableGitLfs = enableGitLfs,
                lfsConcurrentTransfers = lfsConcurrentTransfers,
                enableSubmodule = enableSubmodule,
                enableGitLfsClean = enableGitLfsClean,
                submodulePath = submodulePath,
                enableVirtualMergeBranch = enableVirtualMergeBranch,
                enableSubmoduleRemote = enableSubmoduleRemote,
                enableSubmoduleRecursive = enableSubmoduleRecursive,
                submoduleJobs = submoduleJobs,
                submoduleDepth = submoduleDepth,
                autoCrlf = autoCrlf,
                enableGitClean = enableGitClean,
                enableGitCleanIgnore = enableGitCleanIgnore,
                includePath = includePath,
                excludePath = excludePath,

                pipelineStartType = pipelineStartType,
                hookEventType = hookEventType,
                hookSourceBranch = hookSourceBranch,
                hookTargetBranch = hookTargetBranch,
                hookSourceUrl = hookSourceUrl,
                hookTargetUrl = hookTargetUrl,
                persistCredentials = persistCredentials ?: true,
                compatibleHostList = null,
                enableTrace = enableTrace,
                usernameConfig = if (usernameConfig.isNullOrBlank()) {
                    pipelineStartUserName
                } else {
                    usernameConfig
                },
                userEmailConfig = if (userEmailConfig.isNullOrBlank()) {
                    "$pipelineStartUserName@xxx.com"
                } else {
                    userEmailConfig
                },
                enablePartialClone = enablePartialClone,
                cachePath = cachePath,
                enableGlobalInsteadOf = true,
                useCustomCredential = true,
                enableTGitCache = enableTGitCache,
                tGitCacheUrl = null,
                tGitCacheProxyUrl = null,
                setSafeDirectory = setSafeDirectory,
                mainRepo = mainRepo,
                tGitCacheGrayProject = this.bkSensitiveConfInfo[GitConstants.TGIT_CACHE_GRAY_PROJECT],
                tGitCacheGrayWhiteProject = this.bkSensitiveConfInfo[GitConstants.TGIT_CACHE_GRAY_WHITE_PROJECT],
                tGitCacheGrayWeight = this.bkSensitiveConfInfo[GitConstants.TGIT_CACHE_GRAY_WEIGHT],
                enableServerPreMerge = enableServerPreMerge,
                enableSparseCone = enableSparseCone
            )
        })

    private fun getScmTypeByRepoUrl(repositoryUrl: String, repositoryType: String): ScmType {
        return if (repositoryUrl.isNotEmpty() && RepositoryType.valueOf(repositoryType) == RepositoryType.URL) {
            GitUtil.getScmType(GitUtil.getServerInfo(repositoryUrl).hostName)
        } else {
            ScmType.CODE_TGIT
        }
    }
}
