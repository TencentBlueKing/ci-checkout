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
import com.tencent.bk.devops.git.core.constant.GitConstants.AGENT_PID_VAR
import com.tencent.bk.devops.git.core.enums.AuthHelperType
import com.tencent.bk.devops.git.core.enums.GitProtocolEnum
import com.tencent.bk.devops.git.core.exception.ParamInvalidException
import com.tencent.bk.devops.git.core.pojo.AuthInfo
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.pojo.ServerInfo
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.GitUtil
import com.tencent.bk.devops.git.core.util.SSHAgentUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI

class SshGitAuthHelper(
    private val git: GitCommandManager,
    private val settings: GitSourceSettings
) : AbGitAuthHelper(git = git, settings = settings) {

    companion object {
        private val logger = LoggerFactory.getLogger(SshGitAuthHelper::class.java)
        private const val SSH_AGENT_PID_PATH = ".git/ssh_agent_pid"
    }

    override fun removePreviousAuth() {
        val insteadOfKey = git.tryConfigGet(configKey = GitConstants.GIT_CREDENTIAL_INSTEADOF_KEY)
        if (insteadOfKey.isNotBlank()) {
            git.submoduleForeach(
                command = "git config --unset-all $insteadOfKey || true",
                recursive = true
            )
        }
        stopSshAgent()
    }

    override fun configureAuth() {
        if (authInfo.privateKey.isNullOrBlank()) {
            throw ParamInvalidException(errorMsg = "private key must not be empty")
        }
        EnvHelper.putContext(ContextConstants.CONTEXT_GIT_PROTOCOL, GitProtocolEnum.SSH.name)
        startSshAgent()
        git.setEnvironmentVariable(GitConstants.GIT_SSH_COMMAND, GitConstants.GIT_SSH_COMMAND_VALUE)
        git.config(
            configKey = GitConstants.GIT_CREDENTIAL_AUTH_HELPER,
            configValue = AuthHelperType.SSH.name
        )
        EnvHelper.putContext(GitConstants.GIT_CREDENTIAL_AUTH_HELPER, AuthHelperType.SSH.name)
        // 卸载子模块insteadOf时使用
        git.config(
            configKey = GitConstants.GIT_CREDENTIAL_INSTEADOF_KEY,
            configValue = "url.git@${serverInfo.hostName}:.insteadof"
        )
        if (settings.storeForkRepoCredential){
            replaceUrl(
                url = settings.sourceRepositoryUrl,
                remoteName = GitConstants.DEVOPS_VIRTUAL_REMOTE_NAME,
                authInfo = settings.forkRepoAuthInfo!!
            )
        }
    }

    override fun removeAuth() {
        git.tryConfigUnset(configKey = GitConstants.GIT_CREDENTIAL_INSTEADOF_KEY)
        git.tryConfigGet(configKey = GitConstants.GIT_CREDENTIAL_INSTEADOF_KEY)
        with(settings) {
            if (preMerge && !sourceRepoUrlEqualsRepoUrl) {
                git.remoteSetUrl(remoteName = GitConstants.DEVOPS_VIRTUAL_REMOTE_NAME, remoteUrl = sourceRepositoryUrl)
            }
        }
        stopSshAgent()
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
        commands.add("git config --remove-section url.${serverInfo.origin}:")
    }

    private fun replaceUrl(url: String, remoteName: String, authInfo: AuthInfo) {
        val uri = URI(url)
        val authUrl = "${uri.scheme}://${authInfo.username}:${GitUtil.urlEncode(authInfo.password!!)}" +
            "@${uri.host}${uri.path}"
        git.remoteSetUrl(remoteName = remoteName, remoteUrl = authUrl)
    }

    private fun startSshAgent() {
        SSHAgentUtils().addIdentity(privateKey = authInfo.privateKey!!, passPhrase = authInfo.passPhrase)
        // 将ssh-agent启动产生的pid写入文件,方便post-action阶段时清理
        val sshAgentPid = EnvHelper.getEnvVariable(AGENT_PID_VAR)
        if (!sshAgentPid.isNullOrBlank()) {
            val sshAgentPidFile = File(settings.repositoryPath, SSH_AGENT_PID_PATH)
            if (!sshAgentPidFile.exists()) {
                sshAgentPidFile.createNewFile()
            }
            sshAgentPidFile.writeText(sshAgentPid)
        }
    }

    private fun stopSshAgent() {
        val sshAgentPidFile = File(settings.repositoryPath, SSH_AGENT_PID_PATH)
        if (sshAgentPidFile.exists()) {
            val sshAgentPid = sshAgentPidFile.readText()
            if (sshAgentPid.isNotBlank()) {
                logger.info("kill ssh-agent $sshAgentPid")
                SSHAgentUtils().stop(repositoryPath = settings.repositoryPath, sshAgentPid = sshAgentPid)
                sshAgentPidFile.delete()
            }
        }
    }
}
