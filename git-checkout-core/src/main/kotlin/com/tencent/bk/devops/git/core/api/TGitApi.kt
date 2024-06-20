package com.tencent.bk.devops.git.core.api

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.devops.atom.exception.RemoteServiceException
import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.enums.HttpStatus
import com.tencent.bk.devops.git.core.exception.ApiException
import com.tencent.bk.devops.git.core.pojo.api.GitSession
import com.tencent.bk.devops.git.core.pojo.api.TGitProjectInfo
import com.tencent.bk.devops.git.core.pojo.api.TGitProjectMember
import com.tencent.bk.devops.git.core.util.GitUtil
import com.tencent.bk.devops.git.core.util.HttpUtil
import com.tencent.bk.devops.plugin.pojo.ErrorType
import com.tencent.bk.devops.plugin.utils.JsonUtil
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.net.URLEncoder

class TGitApi(
    val repositoryUrl: String,
    val userId: String,
    private val token: String,
    private val isOauth: Boolean? = false
) : GitApi {

    companion object {
        private val logger = LoggerFactory.getLogger(TGitApi::class.java)
        private const val API_PATH = "api/v3"
        private const val REPORTER_ACCESS_LEVEL = 20

        // 项目是否是公开项目
        private const val PUBLIC_PROJECT_VISIBILITY_LEVEL = 10

        /**
         * 获取会话
         *
         * @param hostName 域名
         * @param userId 登录的用户
         * @param password 用户的有效密码
         * @return
         */
        fun getSession(hostName: String, userId: String, password: String): GitSession? {
            val apiUrl =
                "https://$hostName/$API_PATH/session"
            val request = HttpUtil.buildPost(
                apiUrl,
                JsonUtil.toJson(
                    mapOf(
                        "login" to userId,
                        "password" to password
                    )
                )
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            )
            val responseContent = HttpUtil.retryRequest(request, "Failed to get session")
            if (responseContent.isEmpty()) {
                return null
            }
            return JsonUtil.to(responseContent, object : TypeReference<GitSession>() {})
        }
    }
    private val serverInfo = GitUtil.getServerInfo(repositoryUrl)

    fun getProjectInfo(): TGitProjectInfo {
        try {
            var apiUrl =
                "https://${serverInfo.hostName}/$API_PATH/" +
                    "projects/${URLEncoder.encode(serverInfo.repositoryName, "UTF-8")}"
            if (isOauth == true) {
                apiUrl += "?access_token=$token"
            } else {
                apiUrl += "?private_token=$token"
            }
            val request = HttpUtil.buildGet(apiUrl)
            val responseContent = HttpUtil.retryRequest(
                request,
                "Failed to get tgit repository info"
            )
            return JsonUtil.to(responseContent, TGitProjectInfo::class.java)
        } catch (ignore: RemoteServiceException) {
            if (ignore.httpStatus == HttpStatus.UNAUTHORIZED.statusCode ||
                ignore.httpStatus == HttpStatus.FORBIDDEN.statusCode
            ) {
                throw ApiException(
                    errorType = ErrorType.USER,
                    errorCode = GitConstants.CONFIG_ERROR,
                    httpStatus = ignore.httpStatus,
                    errorMsg = "Failed to verify permissions, " +
                        "user [$$userId] does not have access permissions to repository [$repositoryUrl]"
                )
            }
            throw ignore
        }
    }

    fun getProjectMembers(username: String): List<TGitProjectMember> {
        var apiUrl =
            "https://${serverInfo.hostName}/$API_PATH/" +
                "projects/${URLEncoder.encode(serverInfo.repositoryName, "UTF-8")}/members/all" +
                "?query=$username"
        if (isOauth == true) {
            apiUrl += "&access_token=$token"
        } else {
            apiUrl += "&private_token=$token"
        }
        val request = HttpUtil.buildGet(apiUrl)
        val responseContent = HttpUtil.retryRequest(request, "Failed to get tgit repository members info")
        return JsonUtil.to(responseContent, object : TypeReference<List<TGitProjectMember>>() {})
    }

    /**
     * 判断用户是否有权限访问当前项目
     */
    override fun canViewProject(username: String): Boolean {
        val projectInfo = getProjectInfo()
        // 公开项目，不需要校验
        if (projectInfo.visibilityLevel >= PUBLIC_PROJECT_VISIBILITY_LEVEL) {
            logger.info("${projectInfo.name} is public project")
            return true
        }
        // 私有项目，校验启动人是否是项目的成员
        val members = getProjectMembers(username)
        return members.find {
            it.username == username &&
                it.state == "active" &&
                it.accessLevel >= REPORTER_ACCESS_LEVEL
        } != null
    }

    override fun getProjectId(): Long? {
        if (token.isBlank()) return null
        return getProjectInfo().id
    }
}
