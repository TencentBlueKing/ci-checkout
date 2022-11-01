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
import com.tencent.bk.devops.git.core.enums.CommandLogLevel
import com.tencent.bk.devops.git.core.enums.GitConfigScope
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.pojo.ServerInfo
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.service.helper.IGitAuthHelper
import com.tencent.bk.devops.git.core.util.AgentEnv
import com.tencent.bk.devops.git.core.util.CommandUtil
import com.tencent.bk.devops.git.core.util.FileUtils
import com.tencent.bk.devops.git.core.util.GitUtil
import com.tencent.bk.devops.git.core.util.SubmoduleUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

@SuppressWarnings("TooManyFunctions")
abstract class AbGitAuthHelper(
    private val git: GitCommandManager,
    private val settings: GitSourceSettings
) : IGitAuthHelper {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    protected val serverInfo = GitUtil.getServerInfo(settings.repositoryUrl)
    protected val authInfo = settings.authInfo

    override fun configGlobalAuth() {
        if (settings.enableGlobalInsteadOf && !AgentEnv.isThirdParty()) {
            unsetInsteadOf()
            insteadOf()
            configGlobalAuthCommand()
        } else {
            // 如果构建机上有git insteadOf http,应该卸载,不然凭证会失败
            if (serverInfo.httpProtocol) {
                unsetInsteadOf()
            }
            /**
             * 第三方构建机,为了不污染第三方构建机用户git环境,采用临时覆盖HOME环境变量,然后再执行insteadOf命令
             */
            val tempHomePath = Files.createTempDirectory("checkout")
            val newGitConfigPath = Paths.get(tempHomePath.toString(), ".gitconfig")
            Files.createFile(newGitConfigPath)
            logger.info("Temporarily overriding HOME='$tempHomePath' for fetching submodules")
            git.setEnvironmentVariable(GitConstants.HOME, tempHomePath.toString())
            insteadOf()
            configXdgAuthCommand()
            configureXDGConfig()
        }
    }

    override fun removeGlobalAuth() {
        if (!AgentEnv.isThirdParty()) return
        val gitXdgConfigHome = git.removeEnvironmentVariable(GitConstants.XDG_CONFIG_HOME)
        if (!gitXdgConfigHome.isNullOrBlank()) {
            val gitXdgConfigFile = Paths.get(gitXdgConfigHome, "git", "config")
            logger.info("Deleting Temporarily XDG_CONFIG_HOME='$gitXdgConfigHome'")
            Files.deleteIfExists(gitXdgConfigFile)
        }
    }

    override fun configureSubmoduleAuth() {
        SubmoduleUtil.submoduleForeach(
            repositoryDir = File(settings.repositoryPath),
            recursive = settings.nestedSubmodules
        ) { submodule ->
            try {
                val moduleServerInfo = GitUtil.getServerInfo(submodule.url)
                // 如果是相同的git服务端,但是域名不同,则执行insteadOf命令
                // .gitmodules中声明了子模块,但是目录被删除了,这种子模块不会被初始化
                if (getHostList().contains(moduleServerInfo.hostName) && File(submodule.absolutePath).exists()) {
                    val commands = mutableListOf<String>()
                    configSubmoduleAuthCommand(moduleServerInfo, commands)
                    // 如果schema://HOSTNAME不相同,则统一转换成主库的协议拉取
                    if (moduleServerInfo.origin != serverInfo.origin) {
                        submoduleInsteadOf(moduleServerInfo, commands)
                    }
                    CommandUtil.execute(
                        command = commands.joinToString("\n"),
                        workingDirectory = File(submodule.absolutePath),
                        printLogger = true,
                        logLevel = CommandLogLevel.DEBUG,
                        allowAllExitCodes = true
                    )
                }
            } catch (ignore: Throwable) {
                logger.debug("Failed to config ${submodule.name} auth")
            }
        }
    }

    override fun removeSubmoduleAuth() {
        SubmoduleUtil.submoduleForeach(
            repositoryDir = File(settings.repositoryPath),
            recursive = settings.nestedSubmodules
        ) { submodule ->
            try {
                val moduleServerInfo = GitUtil.getServerInfo(submodule.url)
                // 如果是相同的git服务端,但是域名不同,则执行unset insteadOf命令
                // .gitmodules中声明了子模块,但是目录被删除了,这种子模块不会被初始化
                if (getHostList().contains(moduleServerInfo.hostName) && File(submodule.absolutePath).exists()) {
                    val commands = mutableListOf<String>()
                    removeSubmoduleAuthCommand(moduleServerInfo, commands)
                    if (moduleServerInfo.origin != serverInfo.origin) {
                        submoduleUnsetInsteadOf(moduleServerInfo, commands)
                    }
                    CommandUtil.execute(
                        command = commands.joinToString("\n"),
                        workingDirectory = File(submodule.absolutePath),
                        printLogger = true,
                        logLevel = CommandLogLevel.DEBUG,
                        allowAllExitCodes = true
                    )
                }
            } catch (ignore: Throwable) {
                logger.debug("Failed to remove ${submodule.name} auth")
            }
        }
    }

    /**
     * 配置全局的insteadOf
     */
    abstract fun insteadOf()

    /**
     * 卸载全局的insteadOf
     */
    abstract fun unsetInsteadOf()

    /**
     * 配置子模块insteadOf
     */
    abstract fun submoduleInsteadOf(
        moduleServerInfo: ServerInfo,
        commands: MutableList<String>
    )

    /**
     * 卸载子模块insteadOf
     */
    abstract fun submoduleUnsetInsteadOf(
        moduleServerInfo: ServerInfo,
        commands: MutableList<String>
    )

    /**
     * 子类添加全局配置 expand
     */
    open fun configGlobalAuthCommand() = Unit

    /**
     * 子类添加全局配置 expand
     */
    open fun configXdgAuthCommand() = Unit

    /**
     * 子类配置授权时submodule操作命令
     */
    open fun configSubmoduleAuthCommand(
        moduleServerInfo: ServerInfo,
        commands: MutableList<String>
    ) = Unit

    /**
     * 子类移除授权时submodule操作命令
     */
    open fun removeSubmoduleAuthCommand(
        moduleServerInfo: ServerInfo,
        commands: MutableList<String>
    ) = Unit

    protected fun getHostList(): MutableSet<String> {
        val insteadOfHosts = mutableSetOf(serverInfo.hostName)
        // 当一个git远程仓库有多个域名时，需要都兼容
        val compatibleHostList = settings.compatibleHostList
        if (!compatibleHostList.isNullOrEmpty() && compatibleHostList.contains(serverInfo.hostName)) {
            insteadOfHosts.addAll(compatibleHostList)
        }
        return insteadOfHosts
    }

    protected fun unsetGitInsteadOfHttp(host: String) {
        git.tryConfigUnset(
            configKey = "url.git@$host:.insteadof",
            configScope = GitConfigScope.GLOBAL
        )
    }

    protected fun unsetHttpInsteadOfGit(host: String) {
        listOf("http", "https").forEach { protocol ->
            git.tryConfigUnset(
                configKey = "url.$protocol://$host/.insteadof",
                configScope = GitConfigScope.GLOBAL
            )
        }
    }

    protected fun httpInsteadOfGit(host: String, insteadOfKey: String) {
        git.configAdd(
            configKey = insteadOfKey,
            configValue = "git@$host:",
            configScope = GitConfigScope.GLOBAL
        )
    }

    protected fun gitInsteadOfHttp(host: String, insteadOfKey: String) {
        listOf("http", "https").forEach { protocol ->
            git.configAdd(
                configKey = insteadOfKey,
                configValue = "$protocol://$host/",
                configScope = GitConfigScope.GLOBAL
            )
        }
    }

    /**
     * 凭证管理(如mac读取钥匙串,cache读取~/.cache)依赖HOME环境变量，不能覆盖HOME，所以覆盖XDG_CONFIG_HOME
     *
     * 先设置全局的凭证,然后将全局凭证的配置复制到xdg配置中
     */
    private fun configureXDGConfig() {
        // 移除全局配置,然后把配置文件复制到xdg_config_home的git/config中，
        // git配置读取顺序是: home->xdg_config_home->~/.gitconfig->.git/config
        val tempHome = git.removeEnvironmentVariable(GitConstants.HOME)
        val gitConfigPath = Paths.get(tempHome!!, ".gitconfig")
        val gitXdgConfigHome = Paths.get(
            System.getProperty("user.home"),
            ".checkout",
            System.getenv(GitConstants.BK_CI_PIPELINE_ID) ?: "",
            System.getenv(GitConstants.BK_CI_BUILD_JOB_ID) ?: ""
        ).toString()
        val gitXdgConfigFile = Paths.get(gitXdgConfigHome, "git", "config")
        org.apache.commons.io.FileUtils.copyFile(gitConfigPath.toFile(), gitXdgConfigFile.toFile())
        logger.info(
            "Removing Temporarily HOME AND " +
                "Temporarily overriding XDG_CONFIG_HOME='$gitXdgConfigHome' for fetching submodules"
        )
        // 设置临时的xdg_config_home
        FileUtils.deleteDirectory(File(tempHome))
        git.setEnvironmentVariable(GitConstants.XDG_CONFIG_HOME, gitXdgConfigHome)
    }
}
