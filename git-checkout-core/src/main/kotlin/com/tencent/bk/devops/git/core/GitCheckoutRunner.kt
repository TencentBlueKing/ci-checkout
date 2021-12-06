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

package com.tencent.bk.devops.git.core

import com.tencent.bk.devops.atom.AtomContext
import com.tencent.bk.devops.atom.common.Status
import com.tencent.bk.devops.atom.pojo.AtomBaseParam
import com.tencent.bk.devops.atom.pojo.MonitorData
import com.tencent.bk.devops.atom.pojo.StringData
import com.tencent.bk.devops.atom.utils.json.JsonUtil
import com.tencent.bk.devops.git.core.api.DevopsApi
import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_ATOM_CODE
import com.tencent.bk.devops.git.core.constant.GitConstants.CONTEXT_AUTH_COST_TIME
import com.tencent.bk.devops.git.core.constant.GitConstants.CONTEXT_CHECKOUT_COST_TIME
import com.tencent.bk.devops.git.core.constant.GitConstants.CONTEXT_FETCH_COST_TIME
import com.tencent.bk.devops.git.core.constant.GitConstants.CONTEXT_FETCH_STRATEGY
import com.tencent.bk.devops.git.core.constant.GitConstants.CONTEXT_GIT_PROTOCOL
import com.tencent.bk.devops.git.core.constant.GitConstants.CONTEXT_INIT_COST_TIME
import com.tencent.bk.devops.git.core.constant.GitConstants.CONTEXT_LFS_COST_TIME
import com.tencent.bk.devops.git.core.constant.GitConstants.CONTEXT_LOG_COST_TIME
import com.tencent.bk.devops.git.core.constant.GitConstants.CONTEXT_PREPARE_COST_TIME
import com.tencent.bk.devops.git.core.constant.GitConstants.CONTEXT_SUBMODULE_COST_TIME
import com.tencent.bk.devops.git.core.constant.GitConstants.CONTEXT_USER_ID
import com.tencent.bk.devops.git.core.enums.GitProtocolEnum
import com.tencent.bk.devops.git.core.enums.PullStrategy
import com.tencent.bk.devops.git.core.exception.TaskExecuteException
import com.tencent.bk.devops.git.core.pojo.GitMetricsInfo
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitSourceProvider
import com.tencent.bk.devops.git.core.service.helper.IGitMetricsHelper
import com.tencent.bk.devops.git.core.service.helper.IInputAdapter
import com.tencent.bk.devops.git.core.service.helper.VersionHelper
import com.tencent.bk.devops.git.core.util.DateUtil
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.GitUtil
import com.tencent.bk.devops.git.core.util.StringUtils
import com.tencent.bk.devops.plugin.pojo.ErrorType
import org.slf4j.LoggerFactory
import java.util.ServiceLoader

class GitCheckoutRunner {

    companion object {
        private val logger = LoggerFactory.getLogger(GitCheckoutRunner::class.java)
    }

    fun <T : AtomBaseParam> run(inputAdapter: IInputAdapter, atomContext: AtomContext<T>) {
        logger.info("git checkout core version: ${VersionHelper.getCheckoutCoreVersion()}")
        val monitorData = MonitorData()
        val startTime = System.currentTimeMillis()
        var settings: GitSourceSettings? = null
        try {
            settings = inputAdapter.getInputs()
            val sourceProvider = GitSourceProvider(settings = settings, devopsApi = DevopsApi())
            if (settings.postEntryParam == "True") {
                sourceProvider.cleanUp()
            } else {
                sourceProvider.getSource()
                EnvHelper.getEnvVariables().forEach { (k, v) ->
                    atomContext.result.data[k] = StringData(StringUtils.trimVariable(v))
                }
            }
        } catch (e: TaskExecuteException) {
            atomContext.result.errorType = e.errorType.num
            atomContext.result.errorCode = e.errorCode
            atomContext.result.message = e.message
            atomContext.result.status = Status.failure
        } catch (ignore: Throwable) {
            logger.error("git checkout error", ignore)
            atomContext.result.errorType = ErrorType.PLUGIN.num
            atomContext.result.errorCode = GitConstants.DEFAULT_ERROR
            atomContext.result.message = ignore.message
            atomContext.result.status = Status.failure
        } finally {
            val endTime = System.currentTimeMillis()
            monitorData.startTime = startTime
            monitorData.endTime = endTime
            atomContext.result.monitorData = monitorData
            if (atomContext.param.postEntryParam != "True") {
                reportMetrics(atomContext, settings, startTime, endTime)
            }
            printSummaryLog()
            EnvHelper.clearContext()
        }
    }

    @SuppressWarnings("ComplexMethod")
    private fun <T : AtomBaseParam> reportMetrics(
        atomContext: AtomContext<T>,
        settings: GitSourceSettings?,
        startTime: Long,
        endTime: Long
    ) {
        val atomCode = System.getenv(BK_CI_ATOM_CODE)
        if (settings == null || atomCode == null) {
            return
        }
        try {
            val metricsHelper = ServiceLoader.load(IGitMetricsHelper::class.java).firstOrNull()
            val gitMetricsInfo = with(atomContext.param) {
                GitMetricsInfo(
                    atomCode = atomCode,
                    projectId = projectName,
                    pipelineId = pipelineId,
                    buildId = pipelineBuildId,
                    taskId = pipelineTaskId,
                    scmType = settings.scmType.name,
                    url = settings.repositoryUrl,
                    projectName = GitUtil.getServerInfo(settings.repositoryUrl).repositoryName,
                    startTime = DateUtil.format(startTime),
                    endTime = DateUtil.format(endTime),
                    costTime = endTime - startTime,
                    prepareCostTime = EnvHelper.getContext(CONTEXT_PREPARE_COST_TIME)?.toLong() ?: 0L,
                    initCostTime = EnvHelper.getContext(CONTEXT_INIT_COST_TIME)?.toLong() ?: 0L,
                    submoduleCostTime = EnvHelper.getContext(CONTEXT_SUBMODULE_COST_TIME)?.toLong() ?: 0L,
                    lfsCostTime = EnvHelper.getContext(CONTEXT_LFS_COST_TIME)?.toLong() ?: 0L,
                    fetchCostTime = EnvHelper.getContext(CONTEXT_FETCH_COST_TIME)?.toLong() ?: 0L,
                    checkoutCostTime = EnvHelper.getContext(CONTEXT_CHECKOUT_COST_TIME)?.toLong() ?: 0L,
                    logCostTime = EnvHelper.getContext(CONTEXT_LOG_COST_TIME)?.toLong() ?: 0L,
                    authCostTime = EnvHelper.getContext(CONTEXT_AUTH_COST_TIME)?.toLong() ?: 0L,
                    fetchStrategy = EnvHelper.getContext(CONTEXT_FETCH_STRATEGY) ?: "",
                    errorType = atomContext.result.errorType,
                    errorCode = atomContext.result.errorCode,
                    errorMessage = atomContext.result.message,
                    status = atomContext.result.status.name
                )
            }
            if (metricsHelper != null) {
                logger.info("metricsInfo:${JsonUtil.toJson(gitMetricsInfo)}")
                metricsHelper.reportMetrics(atomCode = "git", metricsInfo = gitMetricsInfo)
            }
        } catch (ignore: Throwable) {
            logger.error("report metrics error, ${ignore.message}")
        }
    }

    private fun printSummaryLog() {
        val summary = StringBuilder("本次构建")
        if (EnvHelper.getContext(CONTEXT_GIT_PROTOCOL) == GitProtocolEnum.HTTP.name &&
            EnvHelper.getContext(CONTEXT_USER_ID) != null
        ) {
            summary.append("使用【${EnvHelper.getContext(CONTEXT_USER_ID)}】的权限")
        }
        when (EnvHelper.getContext(CONTEXT_FETCH_STRATEGY)) {
            PullStrategy.FRESH_CHECKOUT.name ->
                summary.append("【全量】拉取代码")
            PullStrategy.REVERT_UPDATE.name ->
                summary.append("【增量】拉取代码")
        }
        logger.warn(summary.toString())
    }
}
