package com.tencent.bk.devops.git.core.service.auth

import com.tencent.bk.devops.git.core.api.IDevopsApi
import com.tencent.bk.devops.git.core.api.TGitApi
import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_REPOSITORY_URL
import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_USER_ID
import com.tencent.bk.devops.git.core.constant.GitConstants.CI_EVENT
import com.tencent.bk.devops.git.core.enums.ScmType
import com.tencent.bk.devops.git.core.exception.ParamInvalidException
import com.tencent.bk.devops.git.core.i18n.GitErrorsText
import com.tencent.bk.devops.git.core.pojo.AuthInfo
import com.tencent.bk.devops.git.core.util.PlaceholderResolver.Companion.defaultResolver

/**
 * 指定用户名授权,只在stream环境中有效
 */
class AuthUserTokenGitAuthProvider(
    private val pipelineStartUserName: String,
    private val userId: String?,
    private val repositoryUrl: String,
    private val devopsApi: IDevopsApi,
    private val scmType: ScmType
) : IGitAuthProvider {

    override fun getAuthInfo(): AuthInfo {
        val authInfo = UserTokenGitAuthProvider(
            userId = userId,
            devopsApi = devopsApi,
            scmType = scmType
        ).getAuthInfo()
        // 如果授权用户与启动用户不相同,并且事件类型不是定时触发，需要验证启动用户是否有权限下载代码
        if (System.getenv(CI_EVENT) != "schedule" && pipelineStartUserName != userId && scmType == ScmType.CODE_GIT) {
            val gitApi = TGitApi(
                repositoryUrl = repositoryUrl,
                userId = userId!!,
                token = authInfo.password!!,
                isOauth = authInfo.isOauth
            )
            if (!gitApi.canViewProject(pipelineStartUserName)) {
                throw ParamInvalidException(
                    errorMsg = defaultResolver.resolveByMap(
                        content = GitErrorsText.get().startUserIdAuthenticationFailed!!,
                        valueMap = mapOf(
                            CONTEXT_USER_ID to pipelineStartUserName,
                            CONTEXT_REPOSITORY_URL to repositoryUrl
                        )
                    )
                )
            }
        }
        return authInfo
    }
}
