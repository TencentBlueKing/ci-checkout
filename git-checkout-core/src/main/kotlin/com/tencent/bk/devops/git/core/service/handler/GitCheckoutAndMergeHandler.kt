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

package com.tencent.bk.devops.git.core.service.handler

import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_CHECKOUT_COST_TIME
import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_CHECKOUT_REF
import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.enums.PreMergeStrategy
import com.tencent.bk.devops.git.core.enums.PullStrategy
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.service.helper.GitMergeHelper
import com.tencent.bk.devops.git.core.service.helper.GitSparseCheckoutHelper
import com.tencent.bk.devops.git.core.service.helper.RefHelper
import com.tencent.bk.devops.git.core.util.EnvHelper
import org.slf4j.LoggerFactory

class GitCheckoutAndMergeHandler(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) : IGitHandler {

    private val refHelper = RefHelper(settings = settings, git = git)
    private val mergeHelper = GitMergeHelper(settings = settings, git = git)
    private val sparseCheckoutHelper = GitSparseCheckoutHelper(settings = settings, git = git)
    companion object {
        private val logger = LoggerFactory.getLogger(GitCheckoutAndMergeHandler::class.java)
    }

    override fun doHandle() {
        val startEpoch = System.currentTimeMillis()
        try {
            logger.groupStart("Checking out")
            val checkoutInfo = refHelper.getCheckInfo()
            sparseCheckoutHelper.initSparseCheckout(checkoutInfo)
            EnvHelper.putContext(CONTEXT_CHECKOUT_REF, checkoutInfo.ref)
            git.checkout(checkoutInfo.ref, checkoutInfo.startPoint)
            if (checkoutInfo.upstream.isNotBlank()) {
                git.branchUpstream(checkoutInfo.upstream)
            }
            val afterCheckoutLog = getHeadLog()
            // 保存切换前的commitId，当重试时需要切到这个commitId点
            EnvHelper.addEnvVariable(
                key = GitConstants.BK_CI_GIT_REPO_HEAD_COMMIT_ID + "_" + settings.pipelineTaskId,
                value = afterCheckoutLog?.commitId ?: ""
            )
            logger.groupEnd("")
            // 默认合并逻辑
            if (settings.preMergeInfo?.first == PreMergeStrategy.DEFAULT) {
                mergeHelper.doMerge(afterCheckoutLog, checkoutInfo)
            }
            if (settings.pullStrategy == PullStrategy.REVERT_UPDATE && settings.enableGitClean) {
                // checkout完成后再执行git clean命令,避免当子模块删除,但是构建机上子模块目录不会被清理的问题,影响下一次构建
                git.tryClean(
                    enableGitCleanIgnore = settings.enableGitCleanIgnore,
                    enableGitCleanNested = settings.enableGitCleanNested
                )
            }
        } finally {
            EnvHelper.putContext(
                key = CONTEXT_CHECKOUT_COST_TIME,
                value = (System.currentTimeMillis() - startEpoch).toString()
            )
        }
    }

    /**
     * 获取当前工作空间head日志
     */
    private fun getHeadLog() = git.log().firstOrNull()
}
