package com.tencent.bk.devops.git.core.util

import com.tencent.bk.devops.atom.exception.RemoteServiceException
import com.tencent.bk.devops.git.core.exception.ExceptionTranslator
import com.tencent.bk.devops.git.core.service.helper.RetryHelper
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

object HttpUtil {
    private const val connectTimeout = 5L
    private const val readTimeout = 30L
    private const val writeTimeout = 30L
    private val logger = LoggerFactory.getLogger(HttpUtil::class.java)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(connectTimeout, TimeUnit.SECONDS)
        .readTimeout(readTimeout, TimeUnit.SECONDS)
        .writeTimeout(writeTimeout, TimeUnit.SECONDS)
        .build()

    fun buildGet(url: String, headers: Map<String?, String?>? = null): Request {
        return build(url, headers).get().build()
    }

    fun buildPost(url: String, requestBody: RequestBody, headers: Map<String?, String?>? = null): Request {
        return build(url, headers).post(requestBody).build()
    }

    fun build(url: String, headers: Map<String?, String?>? = null): Request.Builder {
        val builder = Request.Builder().url(url)
        if (headers != null) {
            builder.headers(Headers.of(headers))
        }
        return builder
    }

    fun retryRequest(request: Request, errorMessage: String, maxAttempts: Int = 3): String {
        return RetryHelper(maxAttempts = maxAttempts).execute {
            try {
                okHttpClient.newCall(request).execute().use { response ->
                    val responseContent = response.body()?.string() ?: ""
                    if (!response.isSuccessful) {
                        logger.error(
                            "Fail to request($request) with code ${response.code()} " +
                                "message ${response.message()} and response $responseContent"
                        )
                        throw RemoteServiceException(errorMessage, response.code(), response.body()?.string() ?: "")
                    }
                    responseContent
                }
            } catch (ignore: Exception) {
                throw ExceptionTranslator.apiExceptionTranslator(ignore)
            }
        }
    }
}
