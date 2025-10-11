package com.tencent.bk.devops.git.core.service.helper

import com.tencent.bk.devops.git.core.constant.ContextConstants
import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_PRE_MERGE_COMMIT_ID
import com.tencent.bk.devops.git.core.enums.GitErrors
import com.tencent.bk.devops.git.core.enums.PreMergeStrategy
import com.tencent.bk.devops.git.core.exception.GitExecuteException
import com.tencent.bk.devops.git.core.pojo.CheckoutInfo
import com.tencent.bk.devops.git.core.pojo.CommitLogInfo
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.service.repository.GitScmService
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.PlaceholderResolver
import com.tencent.bk.devops.plugin.pojo.ErrorType
import org.slf4j.LoggerFactory

class GitMergeHelper constructor(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) {

    private val refHelper = RefHelper(settings = settings, git = git)

    fun doMerge(afterCheckoutLog: CommitLogInfo?, checkoutInfo: CheckoutInfo) {
        // 启用服务端预合并，则不执行merge操作，直接checkout合并点即可
        if (settings.preMerge) {
            val mergeRef = refHelper.getMergeInfo()
            logger.groupStart("merge $mergeRef into ${checkoutInfo.ref}")
            EnvHelper.addEnvVariable(
                key = GitConstants.BK_CI_GIT_REPO_MR_TARGET_HEAD_COMMIT_ID,
                value = afterCheckoutLog?.commitId ?: ""
            )
            val sourceBranchLog = getBranchHeadLog(mergeRef)
            EnvHelper.addEnvVariable(
                key = GitConstants.BK_CI_GIT_REPO_MR_SOURCE_HEAD_COMMIT_ID,
                value = sourceBranchLog?.commitId ?: ""
            )
            EnvHelper.addEnvVariable(
                key = GitConstants.BK_CI_GIT_REPO_MR_SOURCE_HEAD_COMMIT_COMMENT,
                value = sourceBranchLog?.commitMessage ?: ""
            )
            EnvHelper.putContext(ContextConstants.CONTEXT_MERGE_SOURCE_REF, mergeRef)
            EnvHelper.putContext(ContextConstants.CONTEXT_MERGE_TARGET_REF, settings.ref)
            git.merge(mergeRef)
            logger.groupEnd("")
        }
    }

    private fun getBranchHeadLog(branchName: String): CommitLogInfo? {
        return git.log(branchName = branchName).firstOrNull()
    }

    /**
     * 获取Pre-Merge 策略
     */
    @SuppressWarnings("NestedBlockDepth", "CyclomaticComplexMethod")
    fun getPreMergeInfo() = with(settings) {
        when {
            preMerge && enableServerPreMerge == true -> {
                // 明文拉取时，preMergeInfo已存在直接使用
                // 预合并commit为游离态，服务端可能会清理掉，使用前需校验commit是否存在，不存在则创建
                val tryCreatePreMerge = preMergeInfo?.second
                        ?.takeIf { it.isNotBlank() }
                        ?.let { !git.shaExists(it) } ?: true
                if (tryCreatePreMerge) {
                    System.getenv(GitConstants.BK_REPO_GIT_WEBHOOK_MR_NUMBER)?.toIntOrNull()?.let {
                        logger.info("Creating pre-merge commit for MR#$it")
                        GitScmService(
                            scmType = settings.scmType,
                            repositoryUrl = settings.repositoryUrl,
                            authInfo = settings.authInfo
                        ).createPreMerge(it)
                    }?.let {
                        EnvHelper.putContext(ContextConstants.CONTEXT_MERGE_SOURCE_REF, sourceBranchName)
                        EnvHelper.putContext(ContextConstants.CONTEXT_MERGE_TARGET_REF, ref)
                        // 合并冲突
                        if (it.conflict) {
                            throw GitExecuteException(
                                errorType = ErrorType.USER,
                                errorCode = GitErrors.MergeConflicts.errorCode,
                                errorMsg = GitErrors.MergeConflicts.title ?: "",
                                reason = GitErrors.MergeConflicts.cause?.let {
                                    PlaceholderResolver.defaultResolver.resolveByMap(it, EnvHelper.getContextMap())
                                } ?: "",
                                solution = GitErrors.MergeConflicts.solution ?: "",
                                wiki = GitErrors.MergeConflicts.wiki ?: ""
                            )
                        }
                        logger.debug("created pre-merge commit point [${it.id}]")
                        // 保存预合并commit id，后续重试时校验commit存在后直接使用，无需重复调接口
                        EnvHelper.addEnvVariable(
                            key = BK_CI_GIT_REPO_PRE_MERGE_COMMIT_ID + "_" + settings.pipelineTaskId,
                            value = it.id
                        )
                        PreMergeStrategy.SERVER to it.id
                    } ?: (PreMergeStrategy.DEFAULT to null)
                } else {
                    preMergeInfo!!
                }
            }

            else -> PreMergeStrategy.DEFAULT to null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GitMergeHelper::class.java)
    }
}
