package com.tencent.bk.devops.git.core.api

import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.enums.HttpStatus
import com.tencent.bk.devops.git.core.exception.ApiException
import com.tencent.bk.devops.git.core.pojo.api.GitlabProjectInfo
import com.tencent.bk.devops.git.core.util.GitUtil
import com.tencent.bk.devops.git.core.util.HttpUtil
import com.tencent.bk.devops.plugin.pojo.ErrorType
import com.tencent.bk.devops.plugin.utils.JsonUtil
import org.slf4j.LoggerFactory
import java.net.URLEncoder

class GitlabApi(
    val repositoryUrl: String,
    val userId: String,
    private val token: String
) : GitApi {

    companion object {
        private val logger = LoggerFactory.getLogger(GitlabApi::class.java)
        private const val API_PATH = "api/v4"
    }

    private val serverInfo = GitUtil.getServerInfo(repositoryUrl)

    fun getProjectInfo(authAccess: Boolean? = true): GitlabProjectInfo {
        try {
            var apiUrl =
                "https://${serverInfo.hostName}/$API_PATH/" +
                    "projects/${URLEncoder.encode(serverInfo.repositoryName, "UTF-8")}"
            if (authAccess == true) {
                apiUrl += "?access_token=$token"
            }
            val request = HttpUtil.buildGet(apiUrl)
            val responseContent = HttpUtil.retryRequest(
                request,
                "Failed to get gitlab repository info"
            )
            return JsonUtil.to(responseContent, GitlabProjectInfo::class.java)
        } catch (ignore: ApiException) {
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

    /**
     * 判断用户是否有权限访问当前项目
     */
    override fun canViewProject(username: String) = true

    override fun getProjectId(): Long {
        val gitProjectId: Long
        try {
            gitProjectId = getProjectInfo().id
        } catch (ignore: ApiException) {
            if (ignore.httpStatus == HttpStatus.UNAUTHORIZED.statusCode) {
                // 尝试直接访问Gitlab API，不携带token，仅public仓库可用
                logger.warn("can't to get gitlab repository info,try non-authorization access repository")
                return getProjectInfo(false).id
            }
            throw ignore
        }
        return gitProjectId
    }
}
