package com.tencent.bk.devops.git.credential.storage

import com.microsoft.alm.secret.Credential
import com.tencent.bk.devops.git.credential.Constants
import com.tencent.bk.devops.git.credential.helper.GitHelper
import com.tencent.bk.devops.git.credential.helper.GitOutput
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import java.util.concurrent.TimeUnit

class CacheSecureStore : ICredentialStore {
    companion object {
        private const val MAX_JOB_RUN_DAYS = 7L // Job运行最大天数
    }

    override fun get(targetUri: URI): Credential {
        val stdOuts = invokeHelper("get", targetUri).stdOuts
        if (stdOuts.isEmpty()) {
            // 返回空的账号密码
            return Credential.Empty
        }
        var username = ""
        var password = ""
        stdOuts.forEach { stdOut ->
            val pair = stdOut.split("=", limit = 2)
            if (pair.size == 2) {
                when (pair[0]) {
                    "username" -> username = pair[1]
                    "password" -> password = pair[1]
                }
            }
        }
        return Credential(username, password)
    }

    override fun add(targetUri: URI, credential: Credential) {
        invokeHelper("store", targetUri, credential)
    }

    override fun delete(targetUri: URI) {
        invokeHelper("erase", targetUri)
    }

    private fun invokeHelper(
        action: String,
        targetUri: URI,
        credential: Credential? = null
    ): GitOutput {
        val args = mutableListOf(
            "credential-cache",
            "--timeout=${TimeUnit.DAYS.toSeconds(MAX_JOB_RUN_DAYS).toInt()}",
            "--socket=${cacheSocketPath()}"
        )
        args.add(action)
        val builder = StringBuilder()
        builder.append("protocol=").append(targetUri.scheme).append("\n")
        builder.append("host=").append(targetUri.host).append("\n")
        if (credential != null) {
            builder.append("username=").append(credential.Username).append("\n")
            builder.append("password=").append(credential.Password).append("\n")
        }
        return GitHelper.invokeHelper(
            args = args,
            inputStream = ByteArrayInputStream(builder.toString().toByteArray())
        )
    }

    // 按照job级别指定缓存路径
    private fun cacheSocketPath(): String {
        val pipelineId = System.getenv(Constants.BK_CI_PIPELINE_ID)
        val vmSeqId = System.getenv(Constants.BK_CI_BUILD_JOB_ID)
        var socketPath = File(System.getProperty("user.home"), ".checkout")
        if (!pipelineId.isNullOrBlank()) {
            socketPath = File(socketPath, pipelineId)
        }
        if (!vmSeqId.isNullOrBlank()) {
            socketPath = File(socketPath, vmSeqId)
        }
        socketPath = File(socketPath, "credential/socket")
        return socketPath.absolutePath
    }
}
