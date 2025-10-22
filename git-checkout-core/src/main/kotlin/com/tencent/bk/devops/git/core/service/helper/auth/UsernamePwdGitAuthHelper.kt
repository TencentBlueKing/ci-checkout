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

package com.tencent.bk.devops.git.core.service.helper.auth

import com.tencent.bk.devops.git.core.constant.ContextConstants
import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.enums.AuthHelperType
import com.tencent.bk.devops.git.core.enums.GitProtocolEnum
import com.tencent.bk.devops.git.core.pojo.AuthInfo
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.pojo.ServerInfo
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.GitUtil.urlEncode
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * 用户名密码的方式授权,这种方式用户名密码是明文存储在构建机上
 */
class UsernamePwdGitAuthHelper(
    private val git: GitCommandManager,
    private val settings: GitSourceSettings
) : HttpGitAuthHelper(git = git, settings = settings) {

    companion object {
        private val logger = LoggerFactory.getLogger(UsernamePwdGitAuthHelper::class.java)
    }

    override fun configureAuth() {
        logger.info("using username and password to set credentials ${authInfo.username}/******")
        EnvHelper.putContext(ContextConstants.CONTEXT_GIT_PROTOCOL, GitProtocolEnum.HTTP.name)
        with(settings) {
            replaceUrl(
                url = repositoryUrl,
                remoteName = GitConstants.ORIGIN_REMOTE_NAME,
                authInfo = authInfo
            )
            if (preMerge && !sourceRepoUrlEqualsRepoUrl) {
                replaceUrl(
                    url = sourceRepositoryUrl,
                    remoteName = GitConstants.DEVOPS_VIRTUAL_REMOTE_NAME,
                    authInfo = forkRepoAuthInfo ?: authInfo
                )
            }
        }
        git.config(
            configKey = GitConstants.GIT_CREDENTIAL_AUTH_HELPER,
            configValue = AuthHelperType.USERNAME_PASSWORD.name
        )
        // 卸载子模块insteadOf时使用
        git.config(
            configKey = GitConstants.GIT_CREDENTIAL_INSTEADOF_KEY,
            configValue = getInsteadofUrl()
        )
        EnvHelper.putContext(GitConstants.GIT_CREDENTIAL_AUTH_HELPER, AuthHelperType.USERNAME_PASSWORD.name)
    }

    private fun replaceUrl(url: String, remoteName: String, authInfo: AuthInfo) {
        val uri = URI(url)
        val authUrl = "${uri.scheme}://${authInfo.username}:${urlEncode(authInfo.password!!)}@${uri.host}${uri.path}"
        git.remoteSetUrl(remoteName = remoteName, remoteUrl = authUrl)
    }

    override fun insteadOf() {
        val insteadOfKey = getInsteadofUrl()
        val insteadOfHosts = getHostList()
        insteadOfHosts.forEach { host ->
            httpInsteadOfGit(
                host = host,
                insteadOfKey = insteadOfKey
            )
        }
        insteadOfHosts.forEach { host ->
            gitInsteadOfHttp(
                host = host,
                insteadOfKey = insteadOfKey
            )
        }
    }

    override fun removeAuth() {
        with(settings) {
            git.remoteSetUrl(remoteName = GitConstants.ORIGIN_REMOTE_NAME, remoteUrl = repositoryUrl)
            if (preMerge && !sourceRepoUrlEqualsRepoUrl) {
                git.remoteSetUrl(remoteName = GitConstants.DEVOPS_VIRTUAL_REMOTE_NAME, remoteUrl = sourceRepositoryUrl)
            }
            git.tryConfigUnset(configKey = GitConstants.GIT_CREDENTIAL_INSTEADOF_KEY)
        }
    }

    /**
     * 将所有子模块替换成与主库相同的http[\s]://username:password@/主库host
     */
    override fun configSubmoduleAuthCommand(
        moduleServerInfo: ServerInfo,
        commands: MutableList<String>
    ) {
        val insteadOfKey = getInsteadofUrl()
        val insteadOfValue = if (moduleServerInfo.httpProtocol == serverInfo.httpProtocol) {
            "${moduleServerInfo.origin}/"
        } else {
            "${moduleServerInfo.origin}:"
        }
        // 卸载上一步可能没有清理的配置
        commands.add("git config --unset-all $insteadOfKey")
        commands.add("git config $insteadOfKey $insteadOfValue")
    }

    override fun removeSubmoduleAuthCommand(
        moduleServerInfo: ServerInfo,
        commands: MutableList<String>
    ) {
        val insteadOfKey = git.tryConfigGet(configKey = GitConstants.GIT_CREDENTIAL_INSTEADOF_KEY)
        if (insteadOfKey.isNotBlank()) {
            commands.add("git config --unset-all $insteadOfKey")
        }
    }

    override fun submoduleInsteadOf(
        moduleServerInfo: ServerInfo,
        commands: MutableList<String>
    ) = Unit

    override fun submoduleUnsetInsteadOf(
        moduleServerInfo: ServerInfo,
        commands: MutableList<String>
    ) = Unit

    private fun getInsteadofUrl(): String {
        val uri = URI(settings.repositoryUrl)
        return "url.${uri.scheme}://${authInfo.username}:${urlEncode(authInfo.password!!)}@${uri.host}/.insteadOf"
    }

    override fun woaInsteadOf() = Unit
}
