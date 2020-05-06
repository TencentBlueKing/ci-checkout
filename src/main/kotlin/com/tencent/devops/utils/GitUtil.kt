package com.tencent.devops.utils

import java.io.File
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

object GitUtil {

    private const val HTTPS_PREFIX = "https"
    private const val HTTP_PREFIX = "http"
    private const val SSH_PREFIX = "git"

    fun urlDecode(s: String) = URLDecoder.decode(s, "UTF-8")

    fun urlEncode(s: String) = URLEncoder.encode(s, "UTF-8")

    /**
     * git拉取代码被终止会导致index被锁住，需要删除.git/index.lock文件
     */
    fun deleteLock(workspace: File) {
        val lockFile = File(workspace, ".git/index.lock")
        if (lockFile.exists()) {
            lockFile.delete()
        }
    }

    fun getProjectName(gitUrl: String): String {
        if (!isInvalidUrl(gitUrl)) {
            throw RuntimeException("Invalid git url $gitUrl")
        }
        return if (gitUrl.contains(HTTPS_PREFIX) || gitUrl.contains(HTTP_PREFIX)) {
            val url = URL(gitUrl)
            url.path.removePrefix("/").removeSuffix(".git")
        } else {
            val matches = Regex("""git@(.*):(.*).git""").find(gitUrl)
            matches!!.groupValues[2]
        }
    }

    private fun isInvalidUrl(gitUrl: String): Boolean {
        return (gitUrl.startsWith(HTTP_PREFIX) || gitUrl.startsWith(SSH_PREFIX) || gitUrl.startsWith(HTTPS_PREFIX)) && gitUrl.endsWith(".git")
    }
}
