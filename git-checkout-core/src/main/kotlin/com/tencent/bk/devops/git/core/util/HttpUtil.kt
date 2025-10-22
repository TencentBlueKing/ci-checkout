package com.tencent.bk.devops.git.core.util

import com.tencent.bk.devops.atom.exception.RemoteServiceException
import com.tencent.bk.devops.git.core.exception.ExceptionTranslator
import com.tencent.bk.devops.git.core.exception.ParamInvalidException
import com.tencent.bk.devops.git.core.service.helper.RetryHelper
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.cert.CertificateException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object HttpUtil {
    private const val CONNECT_TIMEOUT = 5L
    private const val READ_TIMEOUT = 30L
    private const val WRITE_TIMEOUT = 30L
    private const val LONG_CONNECT_TIMEOUT = 5 * 1000L
    private const val LONG_READ_TIMEOUT = 5 * 60 * 1000L
    private const val LONG_WRITE_TIMEOUT = 5 * 60 * 1000L
    private val logger = LoggerFactory.getLogger(HttpUtil::class.java)

    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit

        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit

        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
            return arrayOf()
        }
    })

    fun sslSocketFactory(): SSLSocketFactory {
        try {
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            return sslContext.socketFactory
        } catch (ignore: Exception) {
            throw ParamInvalidException(errorMsg = ignore.message!!)
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
        .protocols(listOf(Protocol.HTTP_1_1))
        .build()

    private val longHttpClient = OkHttpClient.Builder()
        .connectTimeout(LONG_CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(LONG_READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(LONG_WRITE_TIMEOUT, TimeUnit.SECONDS)
        .sslSocketFactory(sslSocketFactory(), trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .build()

    fun buildGet(url: String, headers: Map<String, String>? = null): Request {
        return build(url, headers).get().build()
    }

    fun buildPost(url: String, requestBody: RequestBody, headers: Map<String, String>? = null): Request {
        return build(url, headers).post(requestBody).build()
    }

    fun buildPut(url: String, requestBody: RequestBody, headers: Map<String, String>? = null): Request {
        return build(url, headers).put(requestBody).build()
    }

    fun build(url: String, headers: Map<String, String>? = null): Request.Builder {
        val builder = Request.Builder().url(url)
        if (headers != null) {
            builder.headers(headers.toHeaders())
        }
        return builder
    }

    @Suppress("MagicNumber")
    fun downloadFile(request: Request, destPath: File, maxAttempts: Int = 3): Long {
        return RetryHelper(maxAttempts = maxAttempts).execute {
            try {
                var length = 0L
                longHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        logger.warn(
                            "FAIL|Download file from ${request.url}|message=${response.message}| code=${response.code}"
                        )
                        throw RemoteServiceException("Get file fail", response.code, response.message)
                    }
                    if (!destPath.parentFile.exists()) destPath.parentFile.mkdirs()
                    response.body!!.byteStream().use { inputStream ->
                        BufferedOutputStream(FileOutputStream(destPath), 8192).use { outputStream ->
                            length += IOUtils.copy(inputStream, outputStream, 8192)
                        }
                    }
                }
                length
            } catch (ignore: Exception) {
                throw ExceptionTranslator.apiExceptionTranslator(ignore)
            }
        }
    }

    fun retryRequest(request: Request, errorMessage: String, maxAttempts: Int = 3): String {
        return RetryHelper(maxAttempts = maxAttempts).execute {
            try {
                okHttpClient.newCall(request).execute().use { response ->
                    val responseContent = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        logger.error(
                            "$errorMessage|Fail to request with code ${response.code} " +
                                "message ${response.message} and response $responseContent"
                        )
                        throw RemoteServiceException(errorMessage, response.code, responseContent)
                    }
                    responseContent
                }
            } catch (ignore: Exception) {
                throw ExceptionTranslator.apiExceptionTranslator(ignore)
            }
        }
    }
}
