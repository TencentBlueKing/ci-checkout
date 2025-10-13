package com.tencent.bk.devops.git.core.service.repository

import com.tencent.bk.devops.atom.exception.RemoteServiceException
import com.tencent.bk.devops.git.core.api.GitApi
import com.tencent.bk.devops.git.core.api.GithubApi
import com.tencent.bk.devops.git.core.api.GitlabApi
import com.tencent.bk.devops.git.core.api.TGitApi
import com.tencent.bk.devops.git.core.enums.ScmType
import com.tencent.bk.devops.git.core.exception.ApiException
import com.tencent.bk.devops.git.core.pojo.AuthInfo
import com.tencent.bk.devops.git.core.pojo.PreMergeCommit
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
            getGitApi()?.getProjectId()
        } catch (ignored: Exception) {
            logger.debug("can't to get gitProjectId")
            null
        }
    }

    fun createPreMerge(mrIid: Int): PreMergeCommit? {
        return try {
            getGitApi()?.createPreMerge(mrIid)
        } catch (ignored: ApiException) {
            throw ignored
        } catch (ignored: Exception) {
            logger.debug("can't to create pre merge", ignored)
            null
        }
    }

    private fun getGitApi(): GitApi? {
        val token = authInfo.token ?: ""
        val username = authInfo.username ?: ""
        // 尝试获取会话信息
        val tryGetSession = tryGetSession(authInfo, scmType)
        if (token.isBlank() && !tryGetSession) return null
        return when (scmType) {
            ScmType.GITHUB -> {
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
                if (tryGetSession) {
                    logger.debug("try get tgit session info by user[$username]")
                    targetToken = TGitApi.getSession(
                        hostName = GitUtil.getServerInfo(repositoryUrl).hostName,
                        userId = username,
                        password = password
                    )?.privateToken ?: ""
                }
                if (targetToken.isBlank()) {
                    null
                } else {
                    // 缓存token，避免重复获取
                    authInfo.token = targetToken
                    TGitApi(
                        repositoryUrl = repositoryUrl,
                        userId = username,
                        token = targetToken,
                        isOauth = authInfo.isOauth
                    )
                }
            }
            ScmType.CODE_GITLAB -> GitlabApi(
                repositoryUrl = repositoryUrl,
                userId = username,
                token = token
            )
            else -> null
        }
    }

    /**
     * 尝试获取会话信息
     * 1. 非Oauth授权，如果token为空，且username和password都不为空，尝试获取私人令牌
     * 2. 仅支持工蜂仓库
     */
    private fun tryGetSession(authInfo: AuthInfo, scmType: ScmType) =
        listOf(ScmType.CODE_GIT, ScmType.CODE_TGIT).contains(scmType) &&
                authInfo.isOauth == false &&
                authInfo.token.isNullOrBlank() &&
                listOf(authInfo.username, authInfo.password).find { it.isNullOrBlank() } == null

    companion object {
        private val logger = LoggerFactory.getLogger(GitScmService::class.java)
    }
}
