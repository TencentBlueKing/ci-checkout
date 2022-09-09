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

import com.tencent.bk.devops.git.core.constant.ContextConstants
import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_VIRTUAL_REMOTE_NAME
import com.tencent.bk.devops.git.core.constant.GitConstants.ORIGIN_REMOTE_NAME
import com.tencent.bk.devops.git.core.constant.GitConstants.SUPPORT_PARTIAL_CLONE_GIT_VERSION
import com.tencent.bk.devops.git.core.enums.FetchStrategy
import com.tencent.bk.devops.git.core.enums.FilterValueEnum
import com.tencent.bk.devops.git.core.enums.GitConfigScope
import com.tencent.bk.devops.git.core.enums.OSType
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.service.helper.DefaultGitUserConfigHelper
import com.tencent.bk.devops.git.core.service.helper.IGitUserConfigHelper
import com.tencent.bk.devops.git.core.util.AgentEnv
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.GitUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.util.ServiceLoader

class InitRepoHandler(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) : IGitHandler {

    companion object {
        private val logger = LoggerFactory.getLogger(InitRepoHandler::class.java)
    }

    override fun doHandle() {
        val startEpoch = System.currentTimeMillis()
        try {
            with(settings) {
                // Initialize the repository
                logger.groupStart("Initializing the repository")
                initRepository()
                initConfig()
                if (settings.enableTrace == true) {
                    git.setEnvironmentVariable(GitConstants.GIT_TRACE, "1")
                }
                logger.groupEnd("")
            }
        } finally {
            EnvHelper.putContext(
                key = ContextConstants.CONTEXT_INIT_COST_TIME,
                value = (System.currentTimeMillis() - startEpoch).toString()
            )
        }
    }

    private fun GitSourceSettings.initRepository() {
        if (!File(repositoryPath, ".git").exists()) {
            EnvHelper.putContext(ContextConstants.CONTEXT_FETCH_STRATEGY, FetchStrategy.FULL.name)
            git.init()
            git.remoteAdd(ORIGIN_REMOTE_NAME, repositoryUrl)
            // if source repository is fork repo, adding devops-virtual-origin
            if (preMerge && !sourceRepoUrlEqualsRepoUrl
            ) {
                git.remoteAdd(DEVOPS_VIRTUAL_REMOTE_NAME, sourceRepositoryUrl)
            }
        } else {
            if (EnvHelper.getContext(ContextConstants.CONTEXT_FETCH_STRATEGY) == null) {
                EnvHelper.putContext(ContextConstants.CONTEXT_FETCH_STRATEGY, FetchStrategy.VM_CACHE.name)
            }
            git.remoteSetUrl(ORIGIN_REMOTE_NAME, repositoryUrl)
            if (preMerge && !sourceRepoUrlEqualsRepoUrl
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
        val userConfigHelper = ServiceLoader.load(IGitUserConfigHelper::class.java).firstOrNull()
            ?: DefaultGitUserConfigHelper()
        val (usernameConfig, userEmailConfig) = userConfigHelper.getUserConfig(settings)
        if (!usernameConfig.isNullOrBlank()) {
            git.config(configKey = "user.name", configValue = usernameConfig)
        }
        if (!userEmailConfig.isNullOrBlank()) {
            git.config(configKey = "user.email", configValue = userEmailConfig)
        }
        if (repositoryUrl.startsWith("http")) {
            git.config(configKey = "http.sslverify", configValue = "false", configScope = GitConfigScope.LOCAL)
        }
        git.config(configKey = "http.postBuffer", configValue = "524288000", configScope = GitConfigScope.LOCAL)
        git.config(configKey = "gc.auto", configValue = "0")
        initPartialClone()
        if (AgentEnv.getOS() == OSType.WINDOWS) {
            git.config(configKey = "core.longpaths", configValue = "true")
        }
    }

    private fun GitSourceSettings.initPartialClone() {
        if (enablePartialClone == true && git.isAtLeastVersion(SUPPORT_PARTIAL_CLONE_GIT_VERSION)) {
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
}
