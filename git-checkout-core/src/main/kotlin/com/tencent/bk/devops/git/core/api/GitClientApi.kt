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

package com.tencent.bk.devops.git.core.api

import com.tencent.bk.devops.git.core.constant.GitConstants.OAUTH2
import com.tencent.bk.devops.git.core.enums.HttpStatus
import com.tencent.bk.devops.git.core.pojo.AuthInfo
import com.tencent.bk.devops.git.core.util.HttpUtil
import com.tencent.bk.devops.git.core.util.HttpUtil.sslSocketFactory
import com.tencent.bk.devops.git.core.util.HttpUtil.trustAllCerts
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import javax.net.ssl.X509TrustManager

/**
 * git 客户端api接口
 */
class GitClientApi {

    companion object {
        private const val connectTimeout = 5L
        private const val readTimeout = 30L
        private const val writeTimeout = 30L
        private val logger = LoggerFactory.getLogger(GitClientApi::class.java)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(connectTimeout, TimeUnit.SECONDS)
        .readTimeout(readTimeout, TimeUnit.SECONDS)
        .writeTimeout(writeTimeout, TimeUnit.SECONDS)
        .sslSocketFactory(sslSocketFactory(), trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .followRedirects(false)
        .build()

    fun checkCredentials(
        repositoryUrl: String,
        authInfo: AuthInfo
    ): Boolean {
        if (authInfo.username.isNullOrBlank() || authInfo.password.isNullOrBlank()) {
            return false
        }
        val headers = mapOf(
            "Authorization" to Credentials.basic(authInfo.username, authInfo.password)
        )

        val candidates = mutableListOf<String>()
        candidates.add("$repositoryUrl/info/refs?service=git-upload-pack") // smart-http
        candidates.add("$repositoryUrl/info/refs") // dump-http
        if (!repositoryUrl.endsWith(".git")) {
            candidates.add("$repositoryUrl.git/info/refs?service=git-upload-pack") // smart-http
            candidates.add("$repositoryUrl.git/info/refs") // dump-http
        }
        var status = 0
        return try {
            for (url in candidates) {
                val request = HttpUtil.buildGet(url = url, headers = headers)
                var response = okHttpClient.newCall(request).execute()
                if (response.code() == HttpStatus.HTTP_MOVED_PERM.statusCode ||
                    response.code() == HttpStatus.HTTP_MOVED_TEMP.statusCode
                ) {
                    response = redirect(response, headers)
                }
                if (response.code() == HttpStatus.OK.statusCode &&
                    checkOauth2(username = authInfo.username, response = response)
                ) {
                    status = HttpStatus.OK.statusCode
                    break
                }
            }
            status == HttpStatus.OK.statusCode
        } catch (ignore: Exception) {
            logger.warn("Failed to check credential ${ignore.message}")
            false
        }
    }

    private fun redirect(
        response: Response,
        headers: Map<String, String>
    ): Response {
        val location = response.header("Location")
        if (location != null) {
            val newRequest = HttpUtil.buildGet(url = location, headers = headers)
            return okHttpClient.newCall(newRequest).execute()
        }
        return response
    }

    private fun checkOauth2(
        username: String,
        response: Response
    ): Boolean {
        if (username != OAUTH2) {
            return true
        }
        // 工蜂oauth2授权,如果token已经过期,则状态返回的是200,但是内容是Git repository not found
        return response.use {
            !response.body()!!.string().contains("Git repository not found")
        }
    }
}
