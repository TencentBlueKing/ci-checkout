package com.tencent.bk.devops.git.core.service.repository

import com.tencent.bk.devops.git.core.api.GitApi
import com.tencent.bk.devops.git.core.api.GithubApi
import com.tencent.bk.devops.git.core.api.GitlabApi
import com.tencent.bk.devops.git.core.api.TGitApi
import com.tencent.bk.devops.git.core.enums.ScmType
import com.tencent.bk.devops.git.core.service.auth.IGitAuthProvider
import com.tencent.bk.devops.git.core.service.auth.UserNamePasswordGitAuthProvider
import com.tencent.bk.devops.git.core.util.GitUtil
import org.slf4j.LoggerFactory

class GitScmService(
    val scmType: ScmType,
    val repositoryUrl: String,
    private val authProvider: IGitAuthProvider
) {
    /**
     * 获取GitProjectId
     */
    fun getGitProjectId(): Long? {
        return try {
            getGitApi().getProjectId()
        } catch (e: Exception) {
            logger.warn("can't to get gitProjectId")
            null
        }
    }

    private fun getGitApi(): GitApi {
        val token = authProvider.getAuthInfo().token ?: ""
        val username = authProvider.getAuthInfo().username ?: ""
        return when (scmType) {
            ScmType.GITHUB -> GithubApi(repositoryUrl, userId = username, token = token)
            ScmType.CODE_TGIT, ScmType.CODE_GIT -> {
                var targetToken = token
                val password = authProvider.getAuthInfo().password ?: ""
                // 使用[用户名+密码]拉取工蜂代码库，可尝试获取私人令牌
                if (authProvider is UserNamePasswordGitAuthProvider && targetToken.isEmpty()) {
                    targetToken = TGitApi.getSession(
                        hostName = GitUtil.getServerInfo(repositoryUrl).hostName,
                        userId = username,
                        password = password
                    )?.private_token ?: ""
                }
                TGitApi(repositoryUrl, userId = username, token = targetToken)
            }
            ScmType.CODE_GITLAB -> GitlabApi(repositoryUrl, userId = username, token = token)
            else -> TGitApi(repositoryUrl, userId = username, token = token)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GitScmService::class.java)
    }
}