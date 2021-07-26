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

package com.tencent.bk.devops.git.core.service.handler

import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_VIRTUAL_REMOTE_NAME
import com.tencent.bk.devops.git.core.constant.GitConstants.ORIGIN_REMOTE_NAME
import com.tencent.bk.devops.git.core.constant.GitConstants.SUPPORT_PARTIAL_CLONE_GIT_VERSION
import com.tencent.bk.devops.git.core.enums.FilterValueEnum
import com.tencent.bk.devops.git.core.enums.GitConfigScope
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.util.GitUtil
import java.io.File
import org.slf4j.LoggerFactory

class InitRepoHandler(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) : IGitHandler {

    companion object {
        private val logger = LoggerFactory.getLogger(InitRepoHandler::class.java)
    }

    override fun doHandle() {
        with(settings) {
            // Initialize the repository
            logger.groupStart("Initializing the repository")
            val gitVersion = git.getGitVersion()
            git.setEnvironmentVariable(GitConstants.GIT_HTTP_USER_AGENT, "git/$gitVersion (landun-git-checkout)")
            if (settings.enableTrace == true) {
                git.setEnvironmentVariable(GitConstants.GIT_TRACE, "1")
            }
            initRepository()
            initConfig()
            initSparseCheckout()
            if (lfs) {
                git.lfsInstall()
            }
            logger.groupEnd("")
        }
    }

    private fun GitSourceSettings.initRepository() {
        if (!File(repositoryPath, ".git").exists()) {
            git.init()
            git.remoteAdd(ORIGIN_REMOTE_NAME, repositoryUrl)
            // if source repository is fork repo, adding devops-virtual-origin
            if (preMerge && !GitUtil.isSameRepository(
                    repositoryUrl = repositoryUrl,
                    otherRepositoryUrl = sourceRepositoryUrl,
                    hostNameList = compatibleHostList
                )
            ) {
                git.remoteAdd(DEVOPS_VIRTUAL_REMOTE_NAME, sourceRepositoryUrl)
            }
        } else {
            git.remoteSetUrl(ORIGIN_REMOTE_NAME, repositoryUrl)
            if (preMerge && !GitUtil.isSameRepository(
                    repositoryUrl = repositoryUrl,
                    otherRepositoryUrl = sourceRepositoryUrl,
                    hostNameList = compatibleHostList
                )
            ) {
                git.remoteRemove(DEVOPS_VIRTUAL_REMOTE_NAME)
                git.remoteAdd(DEVOPS_VIRTUAL_REMOTE_NAME, sourceRepositoryUrl)
            }
        }
        git.remoteList()
    }

    private fun GitSourceSettings.initConfig() {
        if (!autoCrlf.isNullOrBlank()) {
            git.config(configKey = "core.autocrlf", configValue = autoCrlf!!)
        }
        if (!usernameConfig.isNullOrBlank()) {
            git.config(configKey = "user.name", configValue = usernameConfig!!)
        }
        if (!userEmailConfig.isNullOrBlank()) {
            git.config(configKey = "user.email", configValue = userEmailConfig!!)
        }
        git.config(configKey = "http.sslverify", configValue = "false", configScope = GitConfigScope.GLOBAL)
        git.config(configKey = "http.postBuffer", configValue = "524288000", configScope = GitConfigScope.GLOBAL)
        git.config(configKey = "gc.auto", configValue = "0")
        if (enablePartialClone == true && git.isAtLeastVersion(SUPPORT_PARTIAL_CLONE_GIT_VERSION)) {
            // 如果开启部分克隆,那么浅克隆应该关闭
            settings.fetchDepth = 0
            git.config(configKey = "remote.$ORIGIN_REMOTE_NAME.promisor", configValue = "true")
            git.config(
                configKey = "remote.$ORIGIN_REMOTE_NAME.partialclonefilter",
                configValue = FilterValueEnum.TREELESS.value
            )
            if (preMerge && !GitUtil.isSameRepository(
                    repositoryUrl = repositoryUrl,
                    otherRepositoryUrl = sourceRepositoryUrl,
                    hostNameList = compatibleHostList
                )
            ) {
                git.config(configKey = "remote.$DEVOPS_VIRTUAL_REMOTE_NAME.promisor", configValue = "true")
                git.config(
                    configKey = "remote.$DEVOPS_VIRTUAL_REMOTE_NAME.partialclonefilter",
                    configValue = FilterValueEnum.TREELESS.value
                )
            }
        }
    }

    /**
     * sparse checkout
     */
    private fun GitSourceSettings.initSparseCheckout() {
        val sparseFile = File(repositoryPath, ".git/info/sparse-checkout")
        val content = StringBuilder()
        if (!excludeSubPath.isNullOrBlank()) {
            content.append("/*").append(System.lineSeparator())
            excludeSubPath!!.split(",").forEach {
                content.append("!").append(it.trim()).append(System.lineSeparator())
            }
        }
        if (!includeSubPath.isNullOrBlank()) {
            includeSubPath!!.split(",").forEach {
                content.append("/").append(it.trim().removePrefix("/")).append(System.lineSeparator())
            }
        }

        if (content.toString().isBlank()) {
            // 如果由sparse checkout改成正常拉取,需要把内容设置为*, 不然执行`git checkout`文件内容不会发生改变.
            if (sparseFile.exists()) {
                sparseFile.writeText("*")
            }
            git.config(configKey = "core.sparsecheckout", configValue = "false")
        } else {
            if (!sparseFile.parentFile.exists()) sparseFile.parentFile.mkdirs()
            if (!sparseFile.exists()) sparseFile.createNewFile()
            sparseFile.writeText(content.toString())
            git.config(configKey = "core.sparsecheckout", configValue = "true")
        }
    }
}
