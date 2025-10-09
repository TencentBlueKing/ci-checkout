package com.tencent.bk.devops.git.core.service.helper

import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.enums.PreMergeStrategy
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.util.DateUtil
import com.tencent.bk.devops.git.core.util.GitUtil
import org.slf4j.LoggerFactory

class GitFetchHelper constructor(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) {

    private val refHelper = RefHelper(settings = settings, git = git)
    private val mergeHelper = GitMergeHelper(settings = settings, git = git)

    fun doPrune() {
        // 清理本地已经删除的分支,git fetch --prune也能清理,但是如果git fetch指定分支,就只能清理指定的分支,无法清理所有的
        git.tryPrune(GitConstants.ORIGIN_REMOTE_NAME)
    }

    fun doFetch() {
        with(settings) {
            val preMergeInfo = mergeHelper.getPreMergeInfo()
            settings.preMergeInfo = preMergeInfo
            when (preMergeInfo.first) {
                PreMergeStrategy.SERVER -> {
                    fetchBaseCommit(preMergeInfo.second!!)
                }

                else -> {
                    val shallowSince = calculateShallowSince()
                    fetchTargetRepository(shallowSince = shallowSince)
                    fetchSourceRepository(shallowSince = shallowSince)
                    fetchPrePushBranch(shallowSince = shallowSince)
                    testMerge()
                }
            }
        }
    }

    /**
     * 如果preMerge和fetchDepth同时启用，在执行merge命令时会出现fatal: refusing to merge unrelated histories错误，
     * 所以浅克隆按照深度拉取改成--shallow-since拉取
     * 1. 先fetch共同祖先，共同祖先commitId在webhook body中会传递过来
     * 2. 计算共同祖先提交时间点
     * 3. 按照提交时间点获取代码
     */
    @SuppressWarnings("MagicNumber")
    private fun GitSourceSettings.calculateShallowSince(): String? {
        val baseCommitId = System.getenv(GitConstants.BK_REPO_GIT_WEBHOOK_MR_BASE_COMMIT)
        var shallowSince: String? = null
        if (preMerge && fetchDepth > 0 && !git.isAtLeastVersion(GitConstants.SUPPORT_SHALLOW_SINCE_GIT_VERSION)) {
            logger.warn("开启preMerge，并且指定depth,git版本需要大于2.18才会生效，否则使用的是全量拉取")
        }
        if (canShallowSince(baseCommitId)) {
            git.fetch(
                refSpec = listOf(baseCommitId),
                fetchDepth = 1,
                remoteName = GitConstants.ORIGIN_REMOTE_NAME
            )
            val baseCommitTime = git.log(maxCount = 1, revisionRange = baseCommitId)
                    .firstOrNull()?.commitTime ?: return null
            shallowSince = DateUtil.addDay(baseCommitTime * 1000, -1)
        }
        return shallowSince
    }

    private fun GitSourceSettings.canShallowSince(baseCommitId: String?) =
        preMerge && fetchDepth > 0 && !baseCommitId.isNullOrBlank() &&
                git.isAtLeastVersion(GitConstants.SUPPORT_SHALLOW_SINCE_GIT_VERSION)

    /**
     * 测试是否能够merge成功
     *
     * 如果preMerge和fetchDepth同时启用，并且使用--shallow-since拉取,
     * 在执行merge命令时也可能会出现fatal: refusing to merge unrelated histories错误,
     * 这是因为当develop分支合并master分支，如果develop分支先merge master，然后再向master发起mr请求，
     * ---m1---m2---master
     *      \
     *       \
     *        \
     * ---d1---d2---d3---develop
     * develop和master的公共祖先节点是m1，当通过--shallow-since拉取时，develop分支只拉取了d2，d3，d2有两个父节点，导致无法merge,
     * 只需把d1拉下来就能合并
     *
     * 解决策略:
     * 1. 先通过--shallow-since拉取
     * 2. 计算m1到d3之间的提交数，提交数+1就能得到d1的深度
     * 3. 再次通过depth拉取源分支就能得到d1节点
     */
    private fun GitSourceSettings.testMerge() {
        // preMerge和fetchDepth同时启用,并且不能merge
        if (preMerge && fetchDepth > 0) {
            val remoteName = if (sourceRepoUrlEqualsRepoUrl) {
                GitConstants.ORIGIN_REMOTE_NAME
            } else {
                GitConstants.DEVOPS_VIRTUAL_REMOTE_NAME
            }
            if (!git.canMerge(
                        sourceBranch = "$remoteName/$sourceBranchName",
                        targetBranch = "${GitConstants.ORIGIN_REMOTE_NAME}/$ref"
                    )
            ) {
                val baseCommitId = System.getenv(GitConstants.BK_REPO_GIT_WEBHOOK_MR_BASE_COMMIT)
                val sourceCommitId = System.getenv(GitConstants.BK_REPO_GIT_WEBHOOK_MR_SOURCE_COMMIT)
                val sourceCommitNum = if (!baseCommitId.isNullOrBlank() && !sourceCommitId.isNullOrBlank()) {
                    git.countCommits(baseCommitId = baseCommitId, commitId = sourceCommitId)
                } else {
                    0
                }
                if (sourceCommitNum > 0) {
                    git.fetch(
                        refSpec = listOf("+refs/heads/$sourceBranchName:refs/remotes/$remoteName/$sourceBranchName"),
                        fetchDepth = sourceCommitNum + 1,
                        remoteName = remoteName,
                        shallowSince = null,
                        enablePartialClone = enablePartialClone
                    )
                }
            }
        }
    }

    private fun GitSourceSettings.fetchSourceRepository(shallowSince: String?) {
        if (preMerge && !sourceRepoUrlEqualsRepoUrl) {
            val refSpec = refHelper.getSourceRefSpec()
            git.fetch(
                refSpec = refSpec,
                fetchDepth = fetchDepth,
                remoteName = GitConstants.DEVOPS_VIRTUAL_REMOTE_NAME,
                shallowSince = shallowSince,
                enablePartialClone = enablePartialClone
            )
        }
    }

    private fun GitSourceSettings.fetchTargetRepository(shallowSince: String?) {
        val refSpec = if (isUseFetchRefSpec(shallowSince)) {
            refHelper.getRefSpec()
        } else {
            refHelper.getRefSpecForAllHistory()
        }
        git.fetch(
            refSpec = refSpec,
            fetchDepth = fetchDepth,
            remoteName = GitConstants.ORIGIN_REMOTE_NAME,
            shallowSince = shallowSince,
            enablePartialClone = enablePartialClone
        )
        // 如果是浅克隆,则判断要拉取的commitId是否已存在
        if (fetchDepth > 0) {
            val (exists, commitId) = refHelper.testRef()
            // 如果不存在,则单独再拉取一次
            if (!exists) {
                git.fetch(
                    refSpec = listOf(commitId!!),
                    remoteName = GitConstants.ORIGIN_REMOTE_NAME,
                    fetchDepth = fetchDepth,
                    prune = false
                )
            }
        }
    }

    /**
     * 判断是否只拉取指定的分支，不拉取全部分支，满足以下条件只拉取指定分支
     * 1. 用户开启拉取指定分支
     * 2. preMerge+浅克隆场景
     */
    private fun GitSourceSettings.isUseFetchRefSpec(shallowSince: String?): Boolean {
        return enableFetchRefSpec == true || !shallowSince.isNullOrBlank()
    }

    /**
     * 如果开启pre-push，需要将虚拟分支拉取到FETCH_HEAD
     */
    private fun GitSourceSettings.fetchPrePushBranch(shallowSince: String?) {
        if (GitUtil.isPrePushBranch(ref)) {
            git.fetch(
                refSpec = listOf(ref),
                fetchDepth = fetchDepth,
                remoteName = GitConstants.ORIGIN_REMOTE_NAME,
                shallowSince = shallowSince,
                enablePartialClone = enablePartialClone,
                prune = false
            )
        }
        if (preMerge && GitUtil.isPrePushBranch(sourceBranchName)) {
            git.fetch(
                refSpec = listOf(sourceBranchName),
                fetchDepth = fetchDepth,
                remoteName = GitConstants.ORIGIN_REMOTE_NAME,
                shallowSince = shallowSince,
                enablePartialClone = enablePartialClone,
                prune = false
            )
        }
    }

    private fun GitSourceSettings.fetchBaseCommit(commitId: String) {
        git.fetch(
            refSpec = listOf(commitId),
            fetchDepth = fetchDepth,
            remoteName = GitConstants.ORIGIN_REMOTE_NAME,
            enablePartialClone = enablePartialClone
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GitFetchHelper::class.java)
    }
}
