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
import com.tencent.bk.devops.git.core.exception.ParamInvalidException
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.pojo.ServerInfo
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.SSHAgentUtils

class SshGitAuthHelper(
    private val git: GitCommandManager,
    private val settings: GitSourceSettings
) : AbGitAuthHelper(git = git, settings = settings) {

    override fun removePreviousAuth() {
        val insteadOfKey = git.tryConfigGet(configKey = GitConstants.GIT_CREDENTIAL_INSTEADOF_KEY)
        if (insteadOfKey.isNotBlank()) {
            git.submoduleForeach(
                command = "git config --unset-all $insteadOfKey || true",
                recursive = true
            )
        }
    }

    override fun configureAuth() {
        if (authInfo.privateKey.isNullOrBlank()) {
            throw ParamInvalidException(errorMsg = "private key must not be empty")
        }
        EnvHelper.putContext(ContextConstants.CONTEXT_GIT_PROTOCOL, GitProtocolEnum.SSH.name)
        SSHAgentUtils(privateKey = authInfo.privateKey, passPhrase = authInfo.passPhrase).addIdentity()
        git.setEnvironmentVariable(GitConstants.GIT_SSH_COMMAND, GitConstants.GIT_SSH_COMMAND_VALUE)
        git.config(
            configKey = GitConstants.GIT_CREDENTIAL_AUTH_HELPER,
            configValue = AuthHelperType.SSH.name
        )
        // 卸载子模块insteadOf时使用
        git.config(
            configKey = GitConstants.GIT_CREDENTIAL_INSTEADOF_KEY,
            configValue = "url.git@${serverInfo.hostName}:.insteadof"
        )
    }

    override fun removeAuth() {
        git.tryConfigUnset(configKey = GitConstants.GIT_CREDENTIAL_INSTEADOF_KEY)
    }

    override fun insteadOf() {
        val insteadOfHosts = getHostList()
        val insteadOfKey = "url.git@${serverInfo.hostName}:.insteadof"
        insteadOfHosts.forEach { host ->
            gitInsteadOfHttp(
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
        val insteadOfKey = "url.${serverInfo.origin}:.insteadOf"
        // 卸载上一步可能没有清理的配置
        commands.add("git config --unset-all $insteadOfKey")
        if (moduleServerInfo.httpProtocol != serverInfo.httpProtocol) {
            commands.add("git config $insteadOfKey ${moduleServerInfo.origin}/")
        }
    }

    override fun submoduleUnsetInsteadOf(
        moduleServerInfo: ServerInfo,
        commands: MutableList<String>
    ) {
        val insteadOfKey = "url.${serverInfo.origin}:.insteadOf"
        commands.add("git config --unset-all $insteadOfKey")
    }
}
