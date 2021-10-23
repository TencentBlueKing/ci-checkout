package com.tencent.bk.devops.git.core.api

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.devops.git.core.pojo.api.TGitProjectInfo
import com.tencent.bk.devops.git.core.pojo.api.TGitProjectMember
import com.tencent.bk.devops.git.core.util.GitUtil
import com.tencent.bk.devops.git.core.util.HttpUtil
import com.tencent.bk.devops.plugin.utils.JsonUtil
import java.net.URLEncoder

class TGitApi(
    val repositoryUrl: String,
    private val token: String
) {

    companion object {
        private const val API_PATH = "api/v3"
        private const val REPORTER_ACCESS_LEVEL = 20
    }
    private val serverInfo = GitUtil.getServerInfo(repositoryUrl)

    fun getProjectInfo(): TGitProjectInfo {
        val apiUrl =
            "${serverInfo.origin}/$API_PATH/" +
                "projects/${URLEncoder.encode(serverInfo.repositoryName, "UTF-8")}" +
                "?access_token=$token"
        val request = HttpUtil.buildGet(apiUrl)
        val responseContent = HttpUtil.retryRequest(request, "获取工蜂项目信息失败")
        return JsonUtil.to(responseContent, TGitProjectInfo::class.java)
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
    fun canViewProject(username: String): Boolean {
        return try {
            val projectInfo = getProjectInfo()
            // 公开项目，不需要校验
            if (projectInfo.public) {
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
