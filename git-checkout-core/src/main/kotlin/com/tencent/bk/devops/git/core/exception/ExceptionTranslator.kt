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

package com.tencent.bk.devops.git.core.exception

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.devops.atom.exception.RemoteServiceException
import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.constant.GitConstants.USER_NEED_PROJECT_X_PERMISSION
import com.tencent.bk.devops.git.core.enums.HttpStatus
import com.tencent.bk.devops.plugin.pojo.ErrorType
import com.tencent.bk.devops.plugin.pojo.Result
import com.tencent.bk.devops.plugin.utils.JsonUtil
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ExceptionTranslator {

    @SuppressWarnings("ComplexMethod")
    fun apiExceptionTranslator(exception: Throwable): RuntimeException {
        return when (exception) {
            is UnknownHostException, is ConnectException ->
                RetryException(errorMsg = exception.message!!)
            is SocketTimeoutException -> {
                if ("timeout" == exception.message || "connect timed out" == exception.message) {
                    RetryException(errorMsg = "SocketTimeoutException:" + exception.message)
                } else {
                    ApiException(errorMsg = exception.message!!)
                }
            }
            is RemoteServiceException -> {
                handleRemoteException(exception)
            }
            else ->
                ApiException(errorMsg = exception.message ?: "")
        }
    }

    private fun handleRemoteException(exception: RemoteServiceException) = when {
        exception.httpStatus >= HttpStatus.INTERNAL_SERVER_ERROR.statusCode ->
            RetryException(errorMsg = exception.message ?: "")
        exception.httpStatus >= HttpStatus.BAD_REQUEST.statusCode &&
            exception.httpStatus < HttpStatus.INTERNAL_SERVER_ERROR.statusCode -> {
            val apiException = ApiException(
                errorType = ErrorType.USER,
                errorCode = GitConstants.CONFIG_ERROR,
                httpStatus = exception.httpStatus,
                errorMsg = exception.message ?: ""
            )
            try {
                val result =
                    JsonUtil.to(exception.responseContent, object : TypeReference<Result<Unit>>() {})
                // 因权限中心bug，可能会出现调用权限中心失败，导致接口失败，增加重试
                if (result.status == USER_NEED_PROJECT_X_PERMISSION) {
                    PermissionForbiddenException(
                        errorType = ErrorType.USER,
                        errorCode = GitConstants.CONFIG_ERROR,
                        errorMsg = exception.message ?: ""
                    )
                } else {
                    apiException
                }
            } catch (ignore: Exception) {
                apiException
            }
        }
        else ->
            ApiException(httpStatus = exception.httpStatus, errorMsg = exception.message ?: "")
    }
}
