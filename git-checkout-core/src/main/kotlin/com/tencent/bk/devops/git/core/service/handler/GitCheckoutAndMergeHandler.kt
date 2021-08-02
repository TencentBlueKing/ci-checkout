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

import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.pojo.CommitLogInfo
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.service.helper.RefHelper
import com.tencent.bk.devops.git.core.util.EnvHelper
import org.slf4j.LoggerFactory

class GitCheckoutAndMergeHandler(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) : IGitHandler {

    private val refHelper = RefHelper(settings = settings)
    companion object {
        private val logger = LoggerFactory.getLogger(GitCheckoutAndMergeHandler::class.java)
    }

    override fun doHandle() {
        val checkoutInfo = refHelper.getCheckInfo()
        logger.groupStart("Checking out the ref ${checkoutInfo.ref}")
        git.checkout(checkoutInfo.ref, checkoutInfo.startPoint)
        val afterCheckoutLog = getHeadLog(git)
        // 保存切换前的commitId，当重试时需要切到这个commitId点
        EnvHelper.addEnvVariable(
            key = GitConstants.BK_CI_GIT_REPO_HEAD_COMMIT_ID + "_" + settings.pipelineTaskId,
            value = afterCheckoutLog?.commitId ?: ""
        )
        logger.groupEnd("")
        if (settings.preMerge) {
            val mergeRef = refHelper.getMergeInfo()
            logger.groupStart("merge $mergeRef into ${checkoutInfo.ref}")
            EnvHelper.addEnvVariable(
                key = GitConstants.BK_CI_GIT_REPO_MR_TARGET_HEAD_COMMIT_ID,
                value = afterCheckoutLog?.commitId ?: ""
            )
            git.merge(mergeRef)
            logger.groupEnd("")
        }
    }

    /**
     * 获取当前工作空间head日志
     */
    private fun getHeadLog(git: GitCommandManager): CommitLogInfo? {
        return git.log().firstOrNull()
    }
}
