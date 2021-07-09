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

package com.tencent.bk.devops.git.core.service.helper

import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.constant.GitConstants.GIT_CREDENTIAL_COMPATIBLEHOST
import com.tencent.bk.devops.git.core.constant.GitConstants.GIT_REPO_PATH
import com.tencent.bk.devops.git.core.constant.GitConstants.XDG_CONFIG_HOME
import com.tencent.bk.devops.git.core.enums.GitConfigScope
import com.tencent.bk.devops.git.core.exception.ParamInvalidException
import com.tencent.bk.devops.git.core.pojo.CredentialArguments
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.util.CommandUtil
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.GitUtil
import com.tencent.bk.devops.git.core.util.SSHAgentUtils
import java.io.File
import java.net.URL
import java.nio.file.Paths
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory

@Suppress("ALL")
class GitAuthHelper(
    private val git: GitCommandManager,
    private val settings: GitSourceSettings
) : IGitAuthHelper {

    companion object {
        private val logger = LoggerFactory.getLogger(GitAuthHelper::class.java)
    }

    private val serverInfo = GitUtil.getServerInfo(settings.repositoryUrl)

    private val credentialHome = File(System.getProperty("user.home"), "git-checkout-credential").absolutePath
    private val xdgConfigHome = Paths.get(
        System.getProperty("user.home"),
        "git-checkout-credential",
        System.getenv(GitConstants.BK_CI_PIPELINE_ID) ?: "",
        ".config"
    ).normalize().toString()
    private val xdfConfigPath = Paths.get(xdgConfigHome, "git", "config").normalize().toString()
    private val credentialJarPath = File(credentialHome, "git-checkout-credential.jar").absolutePath

    init {
        val xdgConfigParentFile = File(xdfConfigPath).parentFile
        if (!xdgConfigParentFile.exists()) {
            xdgConfigParentFile.mkdirs()
        }
        EnvHelper.addEnvVariable(
            XDG_CONFIG_HOME, Paths.get(
                "~",
                "git-checkout-credential",
                System.getenv(GitConstants.BK_CI_PIPELINE_ID) ?: "",
                ".config"
            ).normalize().toString()
        )
        git.setEnvironmentVariable(XDG_CONFIG_HOME, xdgConfigHome)
    }

    private fun configureHttp() {
        if (!serverInfo.httpProtocol ||
            settings.username.isNullOrBlank() ||
            settings.password.isNullOrBlank()) {
            return
        }
        if (!settings.compatibleHostList.isNullOrEmpty()) {
            git.config(
                configKey = GIT_CREDENTIAL_COMPATIBLEHOST,
                configValue = settings.compatibleHostList!!.joinToString(","),
                configScope = GitConfigScope.GLOBAL
            )
        }

        store()
        insteadOf()
    }

    private fun store() {
        val credentialJarParentFile = File(credentialHome)
        if (!credentialJarParentFile.exists()) {
            credentialJarParentFile.mkdirs()
        }
        val credentialJarFile = File(credentialJarParentFile, "git-checkout-credential.jar")
        copyCredentialJarFile(credentialJarParentFile = credentialJarParentFile, credentialJarFile = credentialJarFile)
        with(URL(settings.repositoryUrl).toURI()) {
            CommandUtil.execute(
                executable = getJavaFilePath(),
                args = listOf(
                    "-Dfile.encoding=utf-8",
                    "-Ddebug=${settings.enableTrace}",
                    "-jar",
                    credentialJarFile.absolutePath,
                    "devopsStore"
                ),
                runtimeEnv = mapOf(
                    XDG_CONFIG_HOME to xdgConfigHome,
                    GIT_REPO_PATH to settings.repositoryPath
                ),
                inputStream = CredentialArguments(
                    protocol = scheme,
                    host = host,
                    path = path.removePrefix("/"),
                    username = settings.username,
                    password = settings.password
                ).convertInputStream()
            )
        }
    }

    private fun copyCredentialJarFile(credentialJarParentFile: File, credentialJarFile: File) {
        if (!credentialJarFile.exists()) {
            FileUtils.copyToFile(
                javaClass.classLoader.getResourceAsStream("script/git-checkout-credential.jar"),
                File(credentialJarParentFile, "git-checkout-credential.jar")
            )
        } else {
            val newFileMd5 = javaClass.classLoader
                .getResourceAsStream("script/git-checkout-credential.jar").use {
                    DigestUtils.md5Hex(it)
                }
            val oldFileMd5 = credentialJarFile.inputStream().use {
                DigestUtils.md5Hex(it)
            }
            if (newFileMd5 != oldFileMd5) {
                credentialJarFile.delete()
                FileUtils.copyToFile(
                    javaClass.classLoader.getResourceAsStream("script/git-checkout-credential.jar"),
                    File(credentialJarParentFile, "git-checkout-credential.jar")
                )
            }
        }
    }

    private fun insteadOf() {
        httpInsteadOfGit(host = serverInfo.hostName)

        // 配置其他域名权限
        settings.compatibleHostList?.filter { it != serverInfo.hostName }?.forEach { otherHostName ->
            httpInsteadOfGit(host = otherHostName)
        }
    }

    private fun httpInsteadOfGit(host: String) {
        val insteadOfKey = "url.${serverInfo.origin}/.insteadOf"
        // 把全局的insteadOf先去掉
        if (git.configExists(
                configKey = "url.git@$host:.insteadof",
                configScope = GitConfigScope.GLOBAL
            )
        ) {
            git.tryConfigUnset(
                configKey = "url.git@$host:.insteadof",
                configScope = GitConfigScope.GLOBAL
            )
        }
        // 如果没有配置使用http替换ssh,配置
        if (!git.configExists(
                configKey = insteadOfKey,
                configValueRegex = "git@$host:",
                configScope = GitConfigScope.FILE,
                configFile = xdfConfigPath
            )
        ) {
            git.configAdd(
                configKey = insteadOfKey,
                configValue = "git@$host:",
                configScope = GitConfigScope.FILE,
                configFile = xdfConfigPath
            )
        }
    }

    private fun configureSsh() {
        if (serverInfo.httpProtocol) {
            return
        }
        if (settings.privateKey.isNullOrBlank()) {
            throw ParamInvalidException(errorMsg = "private key must not be empty")
        }
        SSHAgentUtils().addIdentity(privateKey = settings.privateKey, passPhrase = settings.passPhrase)
        val insteadOfKey = "url.${serverInfo.origin}:.insteadOf"
        git.tryConfigUnset(
            configKey = insteadOfKey,
            configScope = GitConfigScope.FILE,
            configFile = xdfConfigPath
        )
        listOf(
            "http://${serverInfo.hostName}/",
            "https://${serverInfo.hostName}/"
        ).forEach {
            git.configAdd(
                configKey = insteadOfKey,
                configValue = it,
                configScope = GitConfigScope.FILE,
                configFile = xdfConfigPath
            )
        }

        settings.compatibleHostList?.filter { it != serverInfo.hostName }?.forEach { otherHostName ->
            listOf("http", "https").forEach { protocol ->
                git.configAdd(
                    configKey = insteadOfKey,
                    configValue = "$protocol://$otherHostName/",
                    configScope = GitConfigScope.FILE,
                    configFile = xdfConfigPath
                )
            }
        }
    }

    private fun getJavaFilePath() = File(System.getProperty("java.home"), "/bin/java").absolutePath

    override fun configureAuth() {
        configureHttp()
        configureSsh()
    }

    override fun removeAuth() {
        if (!serverInfo.httpProtocol ||
            settings.username.isNullOrBlank() ||
            settings.password.isNullOrBlank()) {
            return
        }
        // 删除凭证
        with(URL(settings.repositoryUrl).toURI()) {
            CommandUtil.execute(
                executable = getJavaFilePath(),
                args = listOf(
                    "-Dfile.encoding=utf-8",
                    "-Ddebug=${settings.enableTrace}",
                    "-jar",
                    credentialJarPath,
                    "devopsErase"
                ),
                runtimeEnv = mapOf(
                    XDG_CONFIG_HOME to xdgConfigHome,
                    GIT_REPO_PATH to settings.repositoryPath
                ),
                inputStream = CredentialArguments(
                    protocol = scheme,
                    host = host,
                    path = path.removePrefix("/")
                ).convertInputStream()
            )
        }
    }
}
