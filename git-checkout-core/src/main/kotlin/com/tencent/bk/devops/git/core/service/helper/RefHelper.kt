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
import com.tencent.bk.devops.git.core.util.GitUtil

class RefHelper(
    private val settings: GitSourceSettings
) {

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

        if (preMerge && sourceRepoUrlEqualsRepoUrl) {
            addBranchRefSpec(branchName = sourceBranchName, refSpec = refSpec)
        }

        fetchRefSpec?.split(",")?.filter {
            it.isNotBlank() && it != ref && it != sourceBranchName
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
                "refs/remotes/$DEVOPS_VIRTUAL_REMOTE_NAME/${settings.sourceBranchName}")
    }

    fun getCheckInfo(): CheckoutInfo {
        with(settings) {
            val hookCommitId = getHookCommitId()
            return when (pullType) {
                PullType.BRANCH -> {
                    val startPoint = when {
                        GitUtil.isPrePushBranch(ref) ->
                            "FETCH_HEAD"
                        hookCommitId != null -> {
                            hookCommitId
                        }
                        commit.isBlank() ->
                            "refs/remotes/$ORIGIN_REMOTE_NAME/$ref"
                        else ->
                            commit
                    }
                    if (preMerge) {
                        CheckoutInfo(
                            ref = DEVOPS_VIRTUAL_BRANCH,
                            startPoint = startPoint
                        )
                    } else {
                        CheckoutInfo(ref = ref, startPoint = startPoint)
                    }
                }
                PullType.TAG ->
                    CheckoutInfo(ref = ref, startPoint = "refs/tags/$ref")
                PullType.COMMIT_ID ->
                    CheckoutInfo(ref = ref, startPoint = "")
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
        if (!GitUtil.isSameRepository(
                repositoryUrl = settings.repositoryUrl,
                otherRepositoryUrl = hookRepoUrl,
                hostNameList = settings.compatibleHostList
            ) ||
            hookBranch != settings.ref
        ) {
            return null
        }
        return when (gitHookEventType) {
            CodeEventType.MERGE_REQUEST_ACCEPT.name -> mrMergeCommitSha
            CodeEventType.PUSH.name -> hookRevision
            else -> null
        }
    }
}
