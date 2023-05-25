/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.devops.git.core.api

import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.enums.GithubAccessLevelEnum
import com.tencent.bk.devops.git.core.enums.HttpStatus
import com.tencent.bk.devops.git.core.exception.ApiException
import com.tencent.bk.devops.git.core.pojo.api.GithubMemberPermissions
import com.tencent.bk.devops.git.core.pojo.api.GithubRepo
import com.tencent.bk.devops.git.core.util.GitUtil
import com.tencent.bk.devops.git.core.util.HttpUtil
import com.tencent.bk.devops.plugin.pojo.ErrorType
import com.tencent.bk.devops.plugin.utils.JsonUtil
import org.slf4j.LoggerFactory

class GithubApi(
    val repositoryUrl: String,
    val userId: String,
    private val token: String
) : GitApi {

    companion object {
        private const val GITHUB_API = "https://api.github.com"
        private val logger = LoggerFactory.getLogger(GithubApi::class.java)
    }
    private val serverInfo = GitUtil.getServerInfo(repositoryUrl)

    private fun getProjectInfo(authAccess: Boolean? = true): GithubRepo {
        try {
            val apiUrl =
                "$GITHUB_API/repos/${serverInfo.repositoryName}"
            val headers = if (authAccess == true){
                mapOf(
                    "Authorization" to "token  $token",
                    "Accept" to "application/vnd.github.v3+json"
                )
            }else {
                mapOf()
            }
            val request = HttpUtil.buildGet(apiUrl, headers)
            val responseContent = HttpUtil.retryRequest(request, "Failed to get github repository info")
            return JsonUtil.to(responseContent, GithubRepo::class.java)
        } catch (ignore: ApiException) {
            if (ignore.httpStatus == HttpStatus.UNAUTHORIZED.statusCode) {
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

    private fun getMemberPermissions(username: String): GithubMemberPermissions {
        val apiUrl =
            "$GITHUB_API/repos/${serverInfo.repositoryName}/collaborators/$username/permission"
        val headers = mapOf(
            "Authorization" to "token  $token",
            "Accept" to "application/vnd.github.v3+json"
        )
        val request = HttpUtil.buildGet(apiUrl, headers)
        val responseContent = HttpUtil.retryRequest(request, "Failed to get github repository members info")
        return JsonUtil.to(responseContent, GithubMemberPermissions::class.java)
    }

    override fun canViewProject(username: String): Boolean {
        return try {
            val projectInfo = getProjectInfo()
            // 公开项目，不需要校验
            if (!projectInfo.private) {
                logger.info("${projectInfo.name} is public project")
                return true
            }
            // 私有项目，校验启动人是否是项目的成员
            val permissions = getMemberPermissions(username).permission
            GithubAccessLevelEnum.getGithubAccessLevel(permissions).level >= GithubAccessLevelEnum.READ.level
        } catch (ignore: Throwable) {
            false
        }
    }

    override fun getProjectId(): Long {
        val gitProjectId: Long
        try {
            gitProjectId = getProjectInfo().id
        } catch (ignore: ApiException) {
            if (ignore.httpStatus == HttpStatus.UNAUTHORIZED.statusCode) {
                // 尝试直接访问GitHub API，不携带token，仅public仓库可用
                logger.debug("can't to get github repository info,try non-authorization access repository ")
                return getProjectInfo(false).id
            }
            throw ignore
        }
        return gitProjectId
    }
}
