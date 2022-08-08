package com.tencent.bk.devops.git.core.api

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.devops.atom.exception.RemoteServiceException
import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.enums.HttpStatus
import com.tencent.bk.devops.git.core.exception.ApiException
import com.tencent.bk.devops.git.core.pojo.api.TGitProjectInfo
import com.tencent.bk.devops.git.core.pojo.api.TGitProjectMember
import com.tencent.bk.devops.git.core.util.GitUtil
import com.tencent.bk.devops.git.core.util.HttpUtil
import com.tencent.bk.devops.plugin.pojo.ErrorType
import com.tencent.bk.devops.plugin.utils.JsonUtil
import org.slf4j.LoggerFactory
import java.net.URLEncoder

class TGitApi(
    val repositoryUrl: String,
    val userId: String,
    private val token: String
): GitApi {

    companion object {
        private val logger = LoggerFactory.getLogger(TGitApi::class.java)
        private const val API_PATH = "api/v3"
        private const val REPORTER_ACCESS_LEVEL = 20
        // 项目是否是公开项目
        private const val PUBLIC_PROJECT_VISIBILITY_LEVEL = 10
    }
    private val serverInfo = GitUtil.getServerInfo(repositoryUrl)

    fun getProjectInfo(): TGitProjectInfo {
        try {
            val apiUrl =
                "${serverInfo.origin}/$API_PATH/" +
                    "projects/${URLEncoder.encode(serverInfo.repositoryName, "UTF-8")}" +
                    "?access_token=$token"
            val request = HttpUtil.buildGet(apiUrl)
            val responseContent = HttpUtil.retryRequest(request, "获取工蜂项目信息失败")
            return JsonUtil.to(responseContent, TGitProjectInfo::class.java)
        } catch (ignore: RemoteServiceException) {
            if (ignore.httpStatus == HttpStatus.UNAUTHORIZED.statusCode) {
                throw ApiException(
                    errorType = ErrorType.USER,
                    errorCode = GitConstants.CONFIG_ERROR,
                    httpStatus = ignore.httpStatus,
                    errorMsg = "验证权限失败，用户【$userId】没有仓库【$repositoryUrl】访问权限"
                )
            }
            throw ignore
        }
    }

    fun getProjectMembers(username: String): List<TGitProjectMember> {
        val apiUrl =
            "${serverInfo.origin}/$API_PATH/" +
                "projects/${URLEncoder.encode(serverInfo.repositoryName, "UTF-8")}/members/all" +
                "?access_token=$token&query=$username"
        val request = HttpUtil.buildGet(apiUrl)
        val responseContent = HttpUtil.retryRequest(request, "获取工蜂项目成员信息失败")
        return JsonUtil.to(responseContent, object : TypeReference<List<TGitProjectMember>>() {})
    }

    /**
     * 判断用户是否有权限访问当前项目
     */
    override fun canViewProject(username: String): Boolean {
        return try {
            val projectInfo = getProjectInfo()
            // 公开项目，不需要校验
            if (projectInfo.visibilityLevel >= PUBLIC_PROJECT_VISIBILITY_LEVEL) {
                logger.info("${projectInfo.name} is public project")
                return true
            }
            // 私有项目，校验启动人是否是项目的成员
            val members = getProjectMembers(username)
            members.find {
                it.username == username &&
                    it.state == "active" &&
                    it.accessLevel >= REPORTER_ACCESS_LEVEL
            } != null
        } catch (ignore: Throwable) {
            false
        }
    }
}
