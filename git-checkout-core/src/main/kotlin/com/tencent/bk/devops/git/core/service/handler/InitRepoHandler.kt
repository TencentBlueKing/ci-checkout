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
import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_FETCH_STRATEGY
import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_BUILD_ID
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_PIPELINE_ID
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_PROJECT_NAME
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
import com.tencent.bk.devops.git.core.service.helper.GitCacheHelperFactory
import com.tencent.bk.devops.git.core.service.helper.IGitUserConfigHelper
import com.tencent.bk.devops.git.core.util.AgentEnv
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.GitUtil
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.File.separator
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

    override fun afterHandle() {
        if (EnvHelper.getContext(CONTEXT_FETCH_STRATEGY) == FetchStrategy.MIRROR_CACHE.name) {
            git.remoteSetUrl(ORIGIN_REMOTE_NAME, settings.repositoryUrl)
        }
    }

    private fun GitSourceSettings.initRepository() {
        if (!File(repositoryPath, ".git").exists()) {
            EnvHelper.putContext(CONTEXT_FETCH_STRATEGY, FetchStrategy.FULL.name)
            git.init()
            // 设置安全目录
            setSafeDir()
            git.remoteAdd(ORIGIN_REMOTE_NAME, repositoryUrl)
            // if source repository is fork repo, adding devops-virtual-origin
            if (preMerge && !sourceRepoUrlEqualsRepoUrl
            ) {
                git.remoteAdd(DEVOPS_VIRTUAL_REMOTE_NAME, sourceRepositoryUrl)
            }
        } else {
            if (EnvHelper.getContext(CONTEXT_FETCH_STRATEGY) == null) {
                EnvHelper.putContext(CONTEXT_FETCH_STRATEGY, FetchStrategy.VM_CACHE.name)
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
        if (repositoryUrl.startsWith("http:")) {
            git.config(configKey = "http.sslverify", configValue = "false", configScope = GitConfigScope.LOCAL)
        }
        git.config(configKey = "http.postBuffer", configValue = "524288000", configScope = GitConfigScope.LOCAL)
        git.config(configKey = "gc.auto", configValue = "0")
        initPartialClone()
        if (AgentEnv.getOS() == OSType.WINDOWS) {
            git.config(configKey = "core.longpaths", configValue = "true")
        }
        initClientAgent()
        GitCacheHelperFactory.getCacheHelper(settings, git)?.config(settings, git)
    }

    private fun GitSourceSettings.initPartialClone() {
        // git版本不支持部分克隆时,既不会写入也不会残留相关配置,直接跳过
        if (!git.isAtLeastVersion(SUPPORT_PARTIAL_CLONE_GIT_VERSION)) {
            return
        }
        if (enablePartialClone == true) {
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
        } else {
            // 用户主动关闭部分克隆时,清理构建机上可能残留的部分克隆配置,
            // 避免上次构建开启部分克隆后,本次关闭仍复用残留配置继续走部分克隆
            cleanupPartialCloneConfig()
        }
    }

    /**
     * 清理remote上残留的部分克隆配置(promisor、partialclonefilter)
     */
    private fun cleanupPartialCloneConfig() {
        listOf(ORIGIN_REMOTE_NAME, DEVOPS_VIRTUAL_REMOTE_NAME).forEach { remoteName ->
            git.tryConfigUnset(configKey = "remote.$remoteName.promisor")
            git.tryConfigUnset(configKey = "remote.$remoteName.partialclonefilter")
        }
    }

    private fun GitSourceSettings.setSafeDir() {
        if (setSafeDirectory == true) {
            val safeDirs = git.tryConfigGetAll(configKey = "safe.directory", configScope = GitConfigScope.GLOBAL)
            // 补充斜杠
            val repoDir = git.workingDirectory.canonicalPath.apply {
                if (!this.endsWith(separator)) {
                    "$this$separator"
                }
            }
            // 设置了全路径则直接跳过
            if (safeDirs.contains("*") || safeDirs.contains("**")) {
                return
            }
            val noExistsConfig = safeDirs.filter { it.isNotBlank() }.find {
                val canonicalPath = FileUtils.getFile(it).canonicalPath.apply {
                    if (!it.endsWith(separator)) {
                        "$it$separator"
                    }
                }
                it == repoDir || repoDir.startsWith(canonicalPath)
            }.isNullOrBlank()
            if (noExistsConfig) {
                git.configAdd(
                    "safe.directory",
                    repoDir,
                    GitConfigScope.GLOBAL
                )
            }
        }
    }

    private fun initClientAgent() {
        val projectId = System.getenv(BK_CI_PROJECT_NAME)
        val pipelineId = System.getenv(BK_CI_PIPELINE_ID)
        val buildId = System.getenv(BK_CI_BUILD_ID)
        if (projectId.isNullOrBlank() || pipelineId.isNullOrBlank() || buildId.isNullOrBlank()) {
            return
        }
        val targetValue = "Client-Agent: devops-$projectId-$pipelineId-$buildId"
        val extraHeaders = git.tryConfigGetAll(configKey = "http.extraheader")
        val existingClientAgents = extraHeaders.filter { it.startsWith("Client-Agent:") }
        when {
            // 已存在且仅有一条 Client-Agent，值与目标一致，跳过
            existingClientAgents.size == 1 && existingClientAgents.first() == targetValue -> {
            }
            // 不存在 Client-Agent 配置，追加(不影响其他 http.extraheader，如 Authorization)
            existingClientAgents.isEmpty() -> {
                git.configAdd(
                    configKey = "http.extraheader",
                    configValue = targetValue
                )
            }
            // 已存在 Client-Agent 但值不一致或有多条，先按值正则清除旧的再追加新的
            else -> {
                git.tryConfigUnset(
                    configKey = "http.extraheader",
                    configValueRegex = "^Client-Agent:.*"
                )
                git.configAdd(
                    configKey = "http.extraheader",
                    configValue = targetValue
                )
            }
        }
    }
}
