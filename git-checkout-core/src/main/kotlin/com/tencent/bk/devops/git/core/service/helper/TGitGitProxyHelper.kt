package com.tencent.bk.devops.git.core.service.helper

import com.tencent.bk.devops.git.core.constant.GitConstants.ORIGIN_REMOTE_NAME
import com.tencent.bk.devops.git.core.enums.FetchStrategy
import com.tencent.bk.devops.git.core.enums.ScmType
import com.tencent.bk.devops.git.core.exception.RetryException
import com.tencent.bk.devops.git.core.pojo.AuthInfo
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.util.CompressUtil
import com.tencent.bk.devops.git.core.util.GitUtil
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import kotlin.io.path.deleteIfExists

/**
 * 工蜂缓存加速,提高代码拉取速度
 */
class TGitGitProxyHelper : IGitProxyHelper {

    companion object {
        private val logger = LoggerFactory.getLogger(TGitGitProxyHelper::class.java)
    }

    override fun support(settings: GitSourceSettings): Boolean {
        // 开启工蜂边缘节点加速,并且是http协议，并且是工蜂域名
        return settings.enableTGitCache == true
                && GitUtil.isHttpProtocol(settings.repositoryUrl)
                && settings.scmType == ScmType.CODE_GIT
                && !settings.tGitCacheUrl.isNullOrBlank()

    }

    override fun getName(): String {
        return FetchStrategy.TGIT_CACHE.name
    }

    override fun getOrder(): Int {
        return FetchStrategy.TGIT_CACHE.ordinal
    }

    override fun fetch(settings: GitSourceSettings, git: GitCommandManager): Boolean {
        val repositoryPath = settings.repositoryPath
        val repositoryUrl = settings.repositoryUrl
        val proxyUrl = settings.tGitCacheUrl!!

        val tarFile = Files.createTempFile(".git_", ".tar")
        val cacheLockFile = Paths.get(repositoryPath, ".git", "cache.lock").toFile()
        try {
            val serverInfo = GitUtil.getServerInfo(repositoryUrl)
            val repositoryName = serverInfo.repositoryName

            if (!File(repositoryPath).exists()) {
                File(repositoryPath).mkdirs()
            }
            git.init()
            downloadFileToLocal(
                proxyUrl = proxyUrl,
                repositoryName = repositoryName,
                authInfo = settings.authInfo,
                saveFilePath = tarFile.toString()
            )
            val startTime = System.currentTimeMillis()
            CompressUtil.deCompressTar(sourcePath = tarFile.toString(), File(repositoryPath, ".git").path)
            logger.info("compress cache file, time:${System.currentTimeMillis() - startTime}(ms)")

            git.remoteAdd(ORIGIN_REMOTE_NAME, repositoryUrl)
            val origin = serverInfo.origin
            // fetch需要使用git protocol v2协议
            git.config("protocol.version", "2")
            git.config("http.$origin.proxy", proxyUrl)
            git.config("http.$origin.sslverify", "false")
            return true
        } catch (ignore: Throwable) {
            logger.error("Failed to download from tgit cache:${ignore.message}")
            return false
        } finally {
            if (cacheLockFile.exists()) {
                cacheLockFile.delete()
            }
            tarFile.deleteIfExists()
        }
    }

    private fun downloadFileToLocal(
        proxyUrl: String,
        repositoryName: String,
        authInfo: AuthInfo,
        saveFilePath: String
    ) {
        val startTime = System.currentTimeMillis()
        return RetryHelper().execute {
            try {
                val saveDirFile = File(saveFilePath)
                if (!saveDirFile.exists()) {
                    saveDirFile.mkdirs()
                }
                val netUrl = URL("$proxyUrl/$repositoryName/git-upload-pack?service=archive")
                logger.info("tgit cache url:$netUrl")
                val conn = netUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5 * 1000
                conn.readTimeout = 5 * 60 * 1000

                if (!authInfo.username.isNullOrBlank() && !authInfo.password.isNullOrBlank()) {
                    val username = authInfo.username
                    val password = authInfo.password
                    val auth = "$username:$password"
                    val encodedAuth = Base64.getEncoder().encodeToString(auth.toByteArray())
                    // 设置用户名和密码
                    conn.setRequestProperty("Authorization", "Basic $encodedAuth")
                }

                var length = 0L
                // 把文件下载到saveFilePath目录下面
                conn.inputStream.use { inputStream ->
                    BufferedOutputStream(FileOutputStream(saveFilePath), 8192).use { outputStream ->
                        length += IOUtils.copy(inputStream, outputStream, 8192)
                    }
                }
                val elapse = (System.currentTimeMillis() - startTime)
                logger.info("fetch cache file,time:$elapse(ms), size:${length/1024}(KB)")
            } catch (ignored: Exception) {
                logger.warn("Failed to download from tgit cache: ${ignored.message}")
                throw RetryException(errorMsg = ignored.message ?: "Failed to download from tgit cache")
            }
        }
    }
}
