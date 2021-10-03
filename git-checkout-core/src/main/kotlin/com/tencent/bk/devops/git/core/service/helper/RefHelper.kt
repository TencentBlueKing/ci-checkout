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

import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_VIRTUAL_BRANCH
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_VIRTUAL_REMOTE_NAME
import com.tencent.bk.devops.git.core.constant.GitConstants.FETCH_HEAD
import com.tencent.bk.devops.git.core.constant.GitConstants.ORIGIN_REMOTE_NAME
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
                    // 工蜂pre-push，直接按照分支拉取
                    return if (GitUtil.isPrePushBranch(ref)) {
                        listOf(ref)
                    } else {
                        val refSpec = mutableListOf(
                            "--no-tags",
                            "+refs/heads/$ref:refs/remotes/$ORIGIN_REMOTE_NAME/$ref"
                        )
                        if (isAddSourceRef()) {
                            refSpec.add(
                                "+refs/heads/$sourceBranchName:refs/remotes/$ORIGIN_REMOTE_NAME/$sourceBranchName"
                            )
                        }
                        fetchRefSpec?.split(",")?.filter {
                            it != ref || it != sourceBranchName
                        }?.forEach { branch ->
                            refSpec.add("+refs/heads/$branch:refs/remotes/$ORIGIN_REMOTE_NAME/$branch")
                        }
                        refSpec
                    }

                }
                PullType.TAG ->
                    listOf("+refs/tags/$ref:refs/tags/$ref")
                PullType.COMMIT_ID ->
                    listOf(ref)
            }
        }
    }

    fun getSourceRefSpec(): List<String> {
        return listOf(
            "+refs/heads/${settings.sourceBranchName}:" +
                "refs/remotes/$DEVOPS_VIRTUAL_REMOTE_NAME/${settings.sourceBranchName}")
    }

    fun getCheckInfo(): CheckoutInfo {
        with(settings) {
            return when (pullType) {
                PullType.BRANCH -> {
                    val startPoint = when {
                        GitUtil.isPrePushBranch(ref) ->
                            "FETCH_HEAD"
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

    private fun GitSourceSettings.isAddSourceRef() = preMerge && sourceRepoUrlEqualsRepoUrl
}
