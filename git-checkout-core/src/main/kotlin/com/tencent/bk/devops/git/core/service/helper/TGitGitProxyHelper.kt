package com.tencent.bk.devops.git.core.service.helper

import com.tencent.bk.devops.git.core.constant.GitConstants.ORIGIN_REMOTE_NAME
import com.tencent.bk.devops.git.core.enums.FetchStrategy
import com.tencent.bk.devops.git.core.enums.ScmType
import com.tencent.bk.devops.git.core.pojo.AuthInfo
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.util.CompressUtil
import com.tencent.bk.devops.git.core.util.GitUtil
import com.tencent.bk.devops.git.core.util.HttpUtil
import okhttp3.Credentials
import okhttp3.Request
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
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
                && !settings.tGitCacheProxyUrl.isNullOrBlank()

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
        val cacheUrl = settings.tGitCacheUrl!!
        val cacheProxyUrl = settings.tGitCacheProxyUrl!!

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
                proxyUrl = cacheUrl,
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
            git.config("http.$origin.proxy", cacheProxyUrl)
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

    @Suppress("MagicNumber")
    private fun downloadFileToLocal(
        proxyUrl: String,
        repositoryName: String,
        authInfo: AuthInfo,
        saveFilePath: String
    ) {
        val startTime = System.currentTimeMillis()
        val saveDirFile = File(saveFilePath)
        val builder = Request.Builder().url("$proxyUrl/${repositoryName}.git/git-upload-pack?service=archive")
        if (!authInfo.username.isNullOrBlank() && !authInfo.password.isNullOrBlank()) {
            // 设置用户名和密码
            builder.header("Authorization", Credentials.basic(authInfo.username, authInfo.password))
        }
        val request = builder.build()
        logger.info("tgit cache url:${request.url}")
        val length = HttpUtil.downloadFile(request, saveDirFile)

        val elapse = (System.currentTimeMillis() - startTime)
        logger.info("fetch cache file,time:$elapse(ms), size:${length / 1024}(KB)")
    }
}
