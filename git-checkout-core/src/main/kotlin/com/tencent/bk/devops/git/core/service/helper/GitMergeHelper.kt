package com.tencent.bk.devops.git.core.service.helper

import com.tencent.bk.devops.git.core.constant.ContextConstants
import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.pojo.CheckoutInfo
import com.tencent.bk.devops.git.core.pojo.CommitLogInfo
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.util.EnvHelper
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
            // 如果服务端预合并开关关闭，则执行merge操作
            if (settings.serverPreMerge != true) {
                git.merge(mergeRef)
            }
            logger.groupEnd("")
        }
    }

    private fun getBranchHeadLog(branchName: String): CommitLogInfo? {
        return git.log(branchName = branchName).firstOrNull()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GitMergeHelper::class.java)
    }
}
