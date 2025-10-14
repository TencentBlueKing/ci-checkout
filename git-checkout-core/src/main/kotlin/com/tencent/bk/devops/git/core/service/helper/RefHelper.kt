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

package com.tencent.bk.devops.git.core.service.helper

import com.tencent.bk.devops.git.core.constant.ContextConstants
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_HOOK_BRANCH
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_HOOK_REVISION
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_REPO_GIT_WEBHOOK_EVENT_TYPE
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_REPO_WEBHOOK_REPO_URL
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_REPO_GIT_WEBHOOK_MR_MERGE_COMMIT_SHA
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_VIRTUAL_BRANCH
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_VIRTUAL_REMOTE_NAME
import com.tencent.bk.devops.git.core.constant.GitConstants.FETCH_HEAD
import com.tencent.bk.devops.git.core.constant.GitConstants.ORIGIN_REMOTE_NAME
import com.tencent.bk.devops.git.core.enums.CodeEventType
import com.tencent.bk.devops.git.core.enums.PullType
import com.tencent.bk.devops.git.core.pojo.CheckoutInfo
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.GitUtil
import com.tencent.bk.devops.git.core.util.RegexUtil
import org.slf4j.LoggerFactory

class RefHelper(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RefHelper::class.java)
    }

    fun getRefSpecForAllHistory(): List<String> {
        return listOf("+refs/heads/*:refs/remotes/$ORIGIN_REMOTE_NAME/*", "+refs/tags/*:refs/tags/*")
    }

    fun getRefSpec(): List<String> {
        with(settings) {
            return when (pullType) {
                PullType.BRANCH -> {
                    getBranchRefSpec()
                }
                PullType.TAG ->
                    listOf("+refs/tags/$ref:refs/tags/$ref")
                PullType.COMMIT_ID ->
                    listOf(ref)
            }
        }
    }

    private fun GitSourceSettings.getBranchRefSpec(): MutableList<String> {
        val refSpec = mutableListOf(
            "--no-tags"
        )

        // 当push和mr accept事件触发时，需要拉取触发时的commitId,否则拉取指定的分支
        val hookCommitId = getHookCommitId()
        when {
            fetchDepth > 0 && hookCommitId != null -> {
                refSpec.add("+$hookCommitId:refs/remotes/$ORIGIN_REMOTE_NAME/$ref")
            }
            fetchDepth > 0 && commit.isNotBlank() ->
                refSpec.add("+$commit:refs/remotes/$ORIGIN_REMOTE_NAME/$ref")
            else ->
                addBranchRefSpec(branchName = ref, refSpec = refSpec)
        }

        // 只有开启preMerge的时候才添加源分支,否则可能出现源分支合并后就删除,导致找不到源分支
        if (preMerge && sourceRepoUrlEqualsRepoUrl) {
            addBranchRefSpec(branchName = sourceBranchName, refSpec = refSpec)
        }

        fetchRefSpec?.split(",")?.filter {
            it.isNotBlank() && !refSpec.contains(it) && it != ref
        }?.forEach { branch ->
            refSpec.add("+refs/heads/$branch:refs/remotes/$ORIGIN_REMOTE_NAME/$branch")
        }
        return refSpec
    }

    private fun addBranchRefSpec(branchName: String, refSpec: MutableList<String>) {
        // pre-push分支，只能拉取到FETCH_HEAD，不能拉取到分支，所以单独拉取
        if (GitUtil.isPrePushBranch(branchName)) {
            refSpec.add(branchName)
        } else {
            refSpec.add("+refs/heads/$branchName:refs/remotes/$ORIGIN_REMOTE_NAME/$branchName")
        }
    }

    fun getSourceRefSpec(): List<String> {
        return listOf(
            "+refs/heads/${settings.sourceBranchName}:" +
                "refs/remotes/$DEVOPS_VIRTUAL_REMOTE_NAME/${settings.sourceBranchName}"
        )
    }

    /**
     * 判断checkout的ref是否存在
     *
     * 当拉取所有ref并开启depth的时候,如果重试或者拉取commitId时,可能会出现要拉取的commitId没有找到,应该再拉一次
     */
    fun testRef(): Pair<Boolean, String?> {
        with(settings) {
            return when (pullType) {
                PullType.BRANCH -> {
                    val hookCommitId = getHookCommitId()
                    when {
                        hookCommitId != null ->
                            Pair(git.shaExists(hookCommitId), hookCommitId)
                        commit.isNotBlank() ->
                            Pair(git.shaExists(commit), commit)
                        else -> Pair(true, null)
                    }
                }
                PullType.COMMIT_ID -> {
                    Pair(git.shaExists(ref), ref)
                }
                else -> Pair(true, null)
            }
        }
    }

    @SuppressWarnings("LongMethod", "CyclomaticComplexMethod")
    fun getCheckInfo(): CheckoutInfo {
        with(settings) {
            return when (pullType) {
                PullType.BRANCH -> {
                    val hookCommitId = getHookCommitId()
                    val (startPoint, upstream) = when {
                        GitUtil.isPrePushBranch(ref) ->
                            Pair("FETCH_HEAD", "")
                        serverPreMerge?.first == true -> {
                            // 服务端预合并CommitId, 上游分支指定为MR的目标分支
                            Pair(serverPreMerge?.second!!, "$ORIGIN_REMOTE_NAME/$ref")
                        }
                        hookCommitId != null -> {
                            Pair(hookCommitId, "$ORIGIN_REMOTE_NAME/$ref")
                        }
                        commit.isBlank() ->
                            Pair("refs/remotes/$ORIGIN_REMOTE_NAME/$ref", "")
                        else ->
                            Pair(commit, "$ORIGIN_REMOTE_NAME/$ref")
                    }
                    when {
                        // 默认预合并
                        preMerge -> {
                            CheckoutInfo(
                                ref = DEVOPS_VIRTUAL_BRANCH,
                                startPoint = startPoint,
                                upstream = if (serverPreMerge?.first != true) {
                                    ""
                                } else {
                                    // 服务端合并需指定上游分支
                                    upstream
                                }
                            )
                        }

                        else -> {
                            CheckoutInfo(ref = ref, startPoint = startPoint, upstream = upstream)
                        }
                    }
                }
                PullType.TAG -> {
                    if (git.tagExists(ref)) {
                        CheckoutInfo(ref = "refs/tags/$ref", startPoint = "")
                    } else {
                        EnvHelper.putContext(ContextConstants.CONTEXT_INVALID_REF, "1")
                        CheckoutInfo(ref = ref, startPoint = "")
                    }
                }
                PullType.COMMIT_ID -> {
                    if (!RegexUtil.checkSha(ref)) {
                        EnvHelper.putContext(ContextConstants.CONTEXT_INVALID_REF, "1")
                        // origin/master -> master
                        val targetRef = ref.removePrefix("$ORIGIN_REMOTE_NAME/")
                        // ref参数为分支
                        CheckoutInfo(
                            ref = targetRef,
                            startPoint = "refs/remotes/$ORIGIN_REMOTE_NAME/$targetRef",
                            upstream = ""
                        )
                    } else {
                        CheckoutInfo(ref = ref, startPoint = "")
                    }
                }
            }
        }
    }

    fun getMergeInfo(): String {
        with(settings) {
            return if (sourceRepoUrlEqualsRepoUrl) {
                // 工蜂开启pre-push，预合并需要执行 `git merge FETCH_HEAD`
                if (GitUtil.isPrePushBranch(sourceBranchName)) {
                    FETCH_HEAD
                } else {
                    "$ORIGIN_REMOTE_NAME/$sourceBranchName"
                }
            } else {
                "$DEVOPS_VIRTUAL_REMOTE_NAME/$sourceBranchName"
            }
        }
    }

    /**
     * 如果是push和mr accept事件触发，push拉取指定事件触发的commitId，mr accept拉取合并后的commitId
     */
    private fun getHookCommitId(): String? {
        val hookBranch = System.getenv(BK_CI_HOOK_BRANCH)
        val gitHookEventType = System.getenv(BK_CI_REPO_GIT_WEBHOOK_EVENT_TYPE)
        val hookRepoUrl = System.getenv(BK_CI_REPO_WEBHOOK_REPO_URL)
        val hookRevision = System.getenv(BK_CI_HOOK_REVISION)
        val mrMergeCommitSha = System.getenv(BK_REPO_GIT_WEBHOOK_MR_MERGE_COMMIT_SHA)
        /*
         切换到提交点，需要满足
         1. 是git事件触发
         2. 拉取的url与触发的url相同
         3. 拉取的分支与触发的目标分支相同
         */
        if (
            !GitUtil.isGitEvent(gitHookEventType) ||
            !GitUtil.isSameRepository(
                repositoryUrl = settings.repositoryUrl,
                otherRepositoryUrl = hookRepoUrl,
                hostNameList = settings.compatibleHostList
            ) ||
            hookBranch != settings.ref
        ) {
            return null
        }
        // PAC 版本发布后，MR_ACC事件改为MR事件，动作为merge
        // -- MR_ACC 事件拉取合并提交点
        // -- PR 和 MR 事件，当动作为merge时拉取提交点
        return when {
            GitUtil.isMergeRequestEvent(scmType = settings.scmType, gitHookEventType) -> mrMergeCommitSha

            gitHookEventType == CodeEventType.PUSH.name -> hookRevision
            else -> null
        }
    }
}
