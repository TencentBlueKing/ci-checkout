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
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import org.slf4j.LoggerFactory

/**
 * 清理devops插件在构建机本地仓库上残留的git配置,供初始化和post-action两个阶段复用
 */
class GitCleanUpHelper(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GitCleanUpHelper::class.java)
    }

    /**
     * 统一清理devops残留配置(缓存、部分克隆、client-agent),用于post-action阶段
     */
    fun cleanUp() {
        GitCacheHelperFactory.getCacheHelper(settings, git)?.unsetConfig(settings = settings, git = git)
        // 只有本次构建开启了部分克隆才写入过相关配置,开启时才清理,避免无谓的查询
        if (settings.enablePartialClone == true) {
            cleanupPartialCloneConfig()
        }
        cleanupClientAgentConfig()
    }

    /**
     * 清理remote上残留的部分克隆配置(promisor、partialclonefilter)
     *
     * 先用一次正则查询列出真正存在的配置,只清理存在的key,避免固定空跑多次unset
     */
    fun cleanupPartialCloneConfig() {
        val residualKeys = git.tryConfigGetRegexp(
            configKeyRegex = GitConstants.PARTIAL_CLONE_CONFIG_KEY_REGEX
        ).mapNotNull { it.substringBefore(' ', "").ifBlank { null } }.distinct()
        residualKeys.forEach { key ->
            logger.info("cleanup residual partial clone config: $key")
            git.tryConfigUnset(configKey = key)
        }
    }

    /**
     * 清理devops注入的client-agent请求头(http.extraheader中以"Client-Agent: devops-"开头的值)
     *
     * 按值正则清除,不影响其他http.extraheader(如Authorization)
     */
    fun cleanupClientAgentConfig() {
        val existing = git.tryConfigGetAll(
            configKey = GitConstants.CLIENT_AGENT_CONFIG_KEY,
            configValueRegex = GitConstants.CLIENT_AGENT_VALUE_REGEX
        )
        if (existing.isEmpty()) {
            return
        }
        logger.info("cleanup residual client-agent config: ${GitConstants.CLIENT_AGENT_CONFIG_KEY}")
        git.tryConfigUnset(
            configKey = GitConstants.CLIENT_AGENT_CONFIG_KEY,
            configValueRegex = GitConstants.CLIENT_AGENT_VALUE_REGEX
        )
    }
}
