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

import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.enums.CredentialActionEnum
import com.tencent.bk.devops.git.core.pojo.CredentialArguments
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.pojo.ServerInfo
import com.tencent.bk.devops.git.core.service.GitCommandManager
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * http或https协议的授权管理
 */
abstract class HttpGitAuthHelper(
    private val git: GitCommandManager,
    private val settings: GitSourceSettings
) : AbGitAuthHelper(git = git, settings = settings) {

    companion object {
        private val logger = LoggerFactory.getLogger(HttpGitAuthHelper::class.java)
    }

    override fun removePreviousAuth() {
        git.tryConfigUnset(configKey = GitConstants.GIT_CREDENTIAL_HELPER)
        val insteadOfKey = git.tryConfigGet(configKey = GitConstants.GIT_CREDENTIAL_INSTEADOF_KEY)
        if (insteadOfKey.isNotBlank()) {
            git.submoduleForeach(
                command = "git config --unset-all credential.helper;git config --unset-all $insteadOfKey || true",
                recursive = true
            )
        }
    }

    override fun insteadOf() {
        val insteadOfHosts = getHostList()
        val insteadOfKey = "url.${serverInfo.origin}/.insteadOf"
        insteadOfHosts.forEach { host ->
            httpInsteadOfGit(
                host = host,
                insteadOfKey = insteadOfKey
            )
        }
    }

    override fun unsetInsteadOf() {
        val insteadOfHosts = getHostList()
        insteadOfHosts.forEach { host ->
            unsetGitInsteadOfHttp(host = host)
            unsetHttpInsteadOfGit(host = host)
        }
    }

    override fun submoduleInsteadOf(
        moduleServerInfo: ServerInfo,
        commands: MutableList<String>
    ) {
        val insteadOfKey = "url.${serverInfo.origin}/.insteadOf"
        val insteadOfValue = if (moduleServerInfo.httpProtocol == serverInfo.httpProtocol) {
            "${moduleServerInfo.origin}/"
        } else {
            "${moduleServerInfo.origin}:"
        }
        // 卸载上一步可能没有清理的配置
        commands.add("git config --unset-all $insteadOfKey")
        commands.add("git config $insteadOfKey $insteadOfValue")
    }

    override fun submoduleUnsetInsteadOf(
        moduleServerInfo: ServerInfo,
        commands: MutableList<String>
    ) {
        commands.add("git config --remove-section url.${serverInfo.origin}/")
    }

    override fun removeSubmoduleAuthCommand(
        moduleServerInfo: ServerInfo,
        commands: MutableList<String>
    ) {
        commands.add("git config --remove-section credential")
    }

    /**
     * 存储全局凭证,保证凭证能够向下游插件传递,兼容http和https
     *
     * 1. 调用全局凭证管理,将用户名密码保存到凭证管理中使凭证能够向下游插件传递,同时覆盖构建机上错误的凭证
     * 2. 保存全局凭证必须在禁用凭证之前,否则调用全局凭证无用
     * 3. 保存的全局凭证在下游插件可能不生效，因为在同一个私有构建机，
     *    如果同时执行多条流水线,每条流水线拉代码的账号oauth不同就可能被覆盖
     *
     */
    fun storeGlobalCredential(writeCompatibleHost: Boolean) {
        logger.info("save username and password to global credentials")
        println("##[command]$ git credential approve")
        if (writeCompatibleHost) {
            combinableHost { protocol, host ->
                git.credential(
                    action = CredentialActionEnum.APPROVE,
                    inputStream = CredentialArguments(
                        protocol = protocol,
                        host = host,
                        username = authInfo.username,
                        password = authInfo.password
                    ).convertInputStream()
                )
            }
        } else {
            val targetUri = URI(settings.repositoryUrl)
            git.credential(
                action = CredentialActionEnum.APPROVE,
                inputStream = CredentialArguments(
                    protocol = targetUri.scheme,
                    host = targetUri.host,
                    username = authInfo.username,
                    password = authInfo.password
                ).convertInputStream()
            )
        }
    }

    fun combinableHost(action: (protocol: String, host: String) -> Unit) {
        getHostList().forEach { host ->
            listOf("https", "http").forEach { protocol ->
                action.invoke(protocol, host)
            }
        }
    }
}
