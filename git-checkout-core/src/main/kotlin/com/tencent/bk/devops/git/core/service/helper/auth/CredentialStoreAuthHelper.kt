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
import com.tencent.bk.devops.git.core.enums.GitConfigScope
import com.tencent.bk.devops.git.core.enums.GitProtocolEnum
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.pojo.ServerInfo
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.GitUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * 使用store凭证使凭证传递
 *
 * 用于公共构建机并且配置了全局凭证
 */
class CredentialStoreAuthHelper(
    private val git: GitCommandManager,
    private val settings: GitSourceSettings
) : HttpGitAuthHelper(git = git, settings = settings) {

    companion object {
        private val logger = LoggerFactory.getLogger(CredentialStoreAuthHelper::class.java)
    }
    private val storeFile = File.createTempFile("git_", "_credentials")

    override fun configureAuth() {
        logger.info("using store credential to set credentials ${authInfo.username}/******")
        EnvHelper.putContext(ContextConstants.CONTEXT_GIT_PROTOCOL, GitProtocolEnum.HTTP.name)
        git.config(
            configKey = GitConstants.GIT_CREDENTIAL_AUTH_HELPER,
            configValue = AuthHelperType.STORE_CREDENTIAL.name
        )
        EnvHelper.putContext(GitConstants.GIT_CREDENTIAL_AUTH_HELPER, AuthHelperType.CUSTOM_CREDENTIAL.name)
        storeGlobalCredential()
        writeStoreFile()
        if (git.isAtLeastVersion(GitConstants.SUPPORT_EMPTY_CRED_HELPER_GIT_VERSION)) {
            git.tryDisableOtherGitHelpers(configScope = GitConfigScope.LOCAL)
        }
        // 卸载子模块insteadOf时使用
        git.config(
            configKey = GitConstants.GIT_CREDENTIAL_INSTEADOF_KEY,
            configValue = "url.${serverInfo.origin}/.insteadOf"
        )
        git.configAdd(
            configKey = GitConstants.GIT_CREDENTIAL_HELPER,
            configValue = "store --file='${storeFile.absolutePath}'"
        )
    }

    override fun removeAuth() {
        val storeCredentialValue = git.tryConfigGet(
            configKey = GitConstants.GIT_CREDENTIAL_HELPER,
            configValueRegex = "store"
        )
        if (storeCredentialValue.isNotEmpty()) {
            val credentialFilePath = storeCredentialValue.substringAfter("--file=")
                .removePrefix("'").removeSuffix("'")
            if (credentialFilePath.isNotEmpty()) {
                Files.deleteIfExists(Paths.get(credentialFilePath))
            }
            git.tryConfigUnset(configKey = GitConstants.GIT_CREDENTIAL_HELPER)
            git.tryConfigUnset(configKey = GitConstants.GIT_CREDENTIAL_INSTEADOF_KEY)
            git.tryConfigGetAll(configKey = GitConstants.GIT_CREDENTIAL_HELPER)
        }
    }

    override fun configSubmoduleAuthCommand(
        moduleServerInfo: ServerInfo,
        commands: MutableList<String>
    ) {
        if (git.isAtLeastVersion(GitConstants.SUPPORT_EMPTY_CRED_HELPER_GIT_VERSION)) {
            commands.add("git config --add credential.helper \"\" ")
        }
        commands.add("git config --add credential.helper 'store --file=${storeFile.absolutePath}'")
    }

    private fun writeStoreFile() {
        combinableHost { protocol, host ->
            storeFile.appendText(
                "$protocol://" +
                    "${GitUtil.urlEncode(authInfo.username!!)}:${GitUtil.urlEncode(authInfo.password!!)}@$host\n"
            )
        }
    }
}
