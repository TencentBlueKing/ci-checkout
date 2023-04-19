package com.tencent.bk.devops.git.core.service.repository

import com.tencent.bk.devops.git.core.api.DevopsApi
import com.tencent.bk.devops.git.core.api.GitApi
import com.tencent.bk.devops.git.core.api.GithubApi
import com.tencent.bk.devops.git.core.api.GitlabApi
import com.tencent.bk.devops.git.core.api.TGitApi
import com.tencent.bk.devops.git.core.enums.ScmType
import com.tencent.bk.devops.git.core.pojo.AuthInfo
import com.tencent.bk.devops.git.core.service.auth.UserTokenGitAuthProvider
import com.tencent.bk.devops.git.core.util.GitUtil
import org.slf4j.LoggerFactory

class GitScmService(
    val scmType: ScmType,
    val repositoryUrl: String,
    private val authInfo: AuthInfo
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
        var token = authInfo.token ?: ""
        val username = authInfo.username ?: ""
        return when (scmType) {
            ScmType.GITHUB -> {
                if (token.isEmpty() && username.isNotEmpty()) {
                    token = UserTokenGitAuthProvider(
                        userId = username,
                        scmType = ScmType.GITHUB,
                        devopsApi = DevopsApi()
                    ).getAuthInfo().token ?: ""
                }
                GithubApi(
                    repositoryUrl = repositoryUrl,
                    userId = username,
                    token = token
                )
            }
            ScmType.CODE_TGIT, ScmType.CODE_GIT -> {
                var targetToken = token
                val password = authInfo.password ?: ""
                // 使用[用户名+密码]拉取工蜂代码库，可尝试获取私人令牌
                if (username.isNotEmpty() && password.isNotEmpty() && targetToken.isEmpty()) {
                    logger.info("try get tgit session info by user[$username]")
                    targetToken = TGitApi.getSession(
                        hostName = GitUtil.getServerInfo(repositoryUrl).hostName,
                        userId = username,
                        password = password
                    )?.private_token ?: ""
                }
                TGitApi(
                    repositoryUrl = repositoryUrl,
                    userId = username,
                    token = targetToken,
                    isOauth = authInfo.isOauth
                )
            }
            ScmType.CODE_GITLAB -> GitlabApi(
                repositoryUrl = repositoryUrl,
                userId = username,
                token = token
            )
            else -> TGitApi(
                repositoryUrl = repositoryUrl,
                userId = username,
                token = token
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GitScmService::class.java)
    }
}