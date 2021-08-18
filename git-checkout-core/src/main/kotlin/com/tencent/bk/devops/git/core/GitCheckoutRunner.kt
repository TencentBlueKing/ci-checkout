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
import com.tencent.bk.devops.git.core.api.DevopsApi
import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.exception.TaskExecuteException
import com.tencent.bk.devops.git.core.service.GitSourceProvider
import com.tencent.bk.devops.git.core.service.helper.IInputAdapter
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.StringUtils
import com.tencent.bk.devops.plugin.pojo.ErrorType
import org.slf4j.LoggerFactory

class GitCheckoutRunner {

    companion object {
        private val logger = LoggerFactory.getLogger(GitCheckoutRunner::class.java)
    }

    fun <T : AtomBaseParam> run(inputAdapter: IInputAdapter, atomContext: AtomContext<T>) {
        val monitorData = MonitorData()
        monitorData.startTime = System.currentTimeMillis()
        try {
            val settings = inputAdapter.getInputs()
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
            monitorData.endTime = System.currentTimeMillis()
            atomContext.result.monitorData = monitorData
        }
    }
}
