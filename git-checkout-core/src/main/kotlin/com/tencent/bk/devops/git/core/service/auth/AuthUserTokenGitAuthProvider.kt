package com.tencent.bk.devops.git.core.service.auth

import com.tencent.bk.devops.git.core.api.IDevopsApi
import com.tencent.bk.devops.git.core.api.TGitApi
import com.tencent.bk.devops.git.core.exception.ParamInvalidException
import com.tencent.bk.devops.git.core.pojo.AuthInfo

/**
 * 指定用户名授权
 */
class AuthUserTokenGitAuthProvider(
    private val pipelineStartUserName: String,
    private val userId: String?,
    private val repositoryUrl: String,
    private val devopsApi: IDevopsApi
) : IGitAuthProvider {

    override fun getAuthInfo(): AuthInfo {
        val authInfo = UserTokenGitAuthProvider(userId = userId, devopsApi = devopsApi).getAuthInfo()
        // 如果授权用户与启动过用户不相同，需要验证启动用户是否有权限下载代码
        if (pipelineStartUserName != userId) {
            val tGitApi = TGitApi(repositoryUrl = repositoryUrl, token = authInfo.password!!)
            if (!tGitApi.canViewProject(pipelineStartUserName)) {
                throw ParamInvalidException(
                    errorMsg = "流水线启动用户【$pipelineStartUserName】没有权限访问仓库【$repositoryUrl】"
                )
            }
        }
        return authInfo
    }
}
