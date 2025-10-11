package com.tencent.devops.git

import com.tencent.bk.devops.atom.AtomContext
import com.tencent.bk.devops.atom.spi.AtomService
import com.tencent.bk.devops.atom.spi.TaskAtom
import com.tencent.bk.devops.git.core.GitCheckoutRunner
import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.pojo.input.CheckoutAtomParamInput
import com.tencent.bk.devops.git.core.service.input.CheckoutAtomParamInputAdapter
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.devops.git.pojo.CheckoutAtomParam

@AtomService(paramClass = CheckoutAtomParam::class)
class CheckoutAtom : TaskAtom<CheckoutAtomParam> {
    @Suppress("LongMethod")
    override fun execute(context: AtomContext<CheckoutAtomParam>) {
        val inputAdapter = CheckoutAtomParamInputAdapter(input = with(context.param) {
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
                enableGitLfsClean = enableGitLfsClean,
                enableSubmodule = enableSubmodule,
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

                retryStartPoint = context.allParameters[
                    GitConstants.BK_CI_GIT_REPO_HEAD_COMMIT_ID + "_" + pipelineTaskId]?.toString(),
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
                mainRepo = mainRepo
            )
        })
        GitCheckoutRunner().run(inputAdapter = inputAdapter, atomContext = context)
    }
}
