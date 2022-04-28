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
import com.tencent.bk.devops.git.core.enums.PullStrategy
import com.tencent.bk.devops.git.core.pojo.CommitLogInfo
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.service.helper.RefHelper
import com.tencent.bk.devops.git.core.util.EnvHelper
import org.slf4j.LoggerFactory
import java.io.File

class GitCheckoutAndMergeHandler(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) : IGitHandler {

    private val refHelper = RefHelper(settings = settings)
    companion object {
        private val logger = LoggerFactory.getLogger(GitCheckoutAndMergeHandler::class.java)
    }

    override fun doHandle() {
        val startEpoch = System.currentTimeMillis()
        try {
            val checkoutInfo = refHelper.getCheckInfo()
            logger.groupStart("Checking out the ref ${checkoutInfo.ref}")
            settings.initSparseCheckout()
            git.checkout(checkoutInfo.ref, checkoutInfo.startPoint)
            val afterCheckoutLog = getHeadLog()
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
                val sourceBranchLog = getBranchHeadLog(mergeRef)
                EnvHelper.addEnvVariable(
                    key = GitConstants.BK_CI_GIT_REPO_MR_SOURCE_HEAD_COMMIT_ID,
                    value = sourceBranchLog?.commitId ?: ""
                )
                EnvHelper.addEnvVariable(
                    key = GitConstants.BK_CI_GIT_REPO_MR_SOURCE_HEAD_COMMIT_COMMENT,
                    value = sourceBranchLog?.commitMessage ?: ""
                )
                git.merge(mergeRef)
                logger.groupEnd("")
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
                key = GitConstants.CONTEXT_CHECKOUT_COST_TIME,
                value = (System.currentTimeMillis() - startEpoch).toString()
            )
        }
    }

    /**
     * sparse checkout
     */
    private fun GitSourceSettings.initSparseCheckout() {
        val sparseFile = File(repositoryPath, ".git/info/sparse-checkout")
        val content = StringBuilder()
        if (!excludeSubPath.isNullOrBlank()) {
            content.append("/*").append(System.lineSeparator())
            excludeSubPath!!.split(",").forEach {
                content.append("!").append(it.trim()).append(System.lineSeparator())
            }
        }
        if (!includeSubPath.isNullOrBlank()) {
            includeSubPath!!.split(",").forEach {
                content.append("/").append(it.trim().removePrefix("/")).append(System.lineSeparator())
            }
        }

        val checkInfo = refHelper.getCheckInfo()
        if (content.toString().isBlank()) {
            /*
                #24 如果由sparse checkout改成正常拉取,需要把内容设置为*, 不然执行`git checkout`文件内容不会发生改变.
                参考: https://ftp.mcs.anl.gov/pub/pdetools/nightlylogs/xsdk/xsdk-configuration
                -tester/packages/trilinos/sparse_checkout.sh
             */
            if (sparseFile.exists()) {
                sparseFile.writeText("*")
                git.config(configKey = "core.sparsecheckout", configValue = "true")
                if (checkInfo.startPoint.isBlank()) {
                    git.readTree(options = listOf("--reset", "-u", checkInfo.ref))
                } else {
                    git.readTree(options = listOf("--reset", "-u", checkInfo.startPoint))
                }

                sparseFile.delete()
            }
            git.config(configKey = "core.sparsecheckout", configValue = "false")
        } else {
            if (!sparseFile.parentFile.exists()) sparseFile.parentFile.mkdirs()
            if (!sparseFile.exists()) sparseFile.createNewFile()
            sparseFile.writeText(content.toString())
            git.config(configKey = "core.sparsecheckout", configValue = "true")
            if (checkInfo.startPoint.isBlank()) {
                git.readTree(options = listOf("-m", "-u", checkInfo.ref))
            } else {
                git.readTree(options = listOf("-m", "-u", checkInfo.startPoint))
            }
        }
    }

    /**
     * 获取当前工作空间head日志
     */
    private fun getHeadLog(): CommitLogInfo? {
        return git.log().firstOrNull()
    }

    private fun getBranchHeadLog(branchName: String): CommitLogInfo? {
        return git.log(branchName = branchName).firstOrNull()
    }
}
