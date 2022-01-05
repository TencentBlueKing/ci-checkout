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

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.devops.atom.utils.json.JsonUtil
import com.tencent.bk.devops.git.core.constant.GitConstants.CONTEXT_ERROR_INFO_LIST
import com.tencent.bk.devops.git.core.exception.RetryException
import com.tencent.bk.devops.git.core.pojo.ErrorInfo
import com.tencent.bk.devops.git.core.util.EnvHelper
import kotlin.math.floor
import org.slf4j.LoggerFactory

class RetryHelper(
    private val maxAttempts: Int = 3,
    private val minSeconds: Int = 10,
    private val maxSeconds: Int = 20
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RetryHelper::class.java)
    }

    @SuppressWarnings("MagicNumber")
    fun <T> execute(action: () -> T): T {
        logger.info("插件重试")
        var attempt = 1
        while (attempt <= maxAttempts) {
            try {
                return action()
            } catch (e: RetryException) {
                reportErrorInfo(e)
                logger.error(e.message)
            }
            val seconds = getSleepAmount()
            logger.info("Attempting $attempt|Waiting $seconds seconds before trying again")
            Thread.sleep(seconds * 1000)
            attempt++
        }
        return action()
    }

    private fun getSleepAmount(): Long {
        return (floor(Math.random() * (maxSeconds - minSeconds + 1)).toLong() + minSeconds)
    }

    private fun reportErrorInfo(error: RetryException) {
        val errorInfoStr = EnvHelper.getContext(CONTEXT_ERROR_INFO_LIST)
        val errorInfo: List<ErrorInfo> = if (errorInfoStr != null) {
            val errorInfos = JsonUtil.fromJson(errorInfoStr, object : TypeReference<List<ErrorInfo>>() {})
            val mutableErrorInfos = errorInfos.toMutableList()
            mutableErrorInfos.add(
                ErrorInfo(
                    errorCode = error.errorCode,
                    errorType = error.errorType.num,
                    errorMsg = error.errorMsg
                )
            )
            mutableErrorInfos
        } else {
            listOf(
                ErrorInfo(
                    errorCode = error.errorCode,
                    errorType = error.errorType.num,
                    errorMsg = error.errorMsg
                )
            )
        }
        logger.info("retry error info is : ${JsonUtil.toJson(errorInfo)}")
        EnvHelper.putContext(CONTEXT_ERROR_INFO_LIST, JsonUtil.toJson(errorInfo))
    }
}
