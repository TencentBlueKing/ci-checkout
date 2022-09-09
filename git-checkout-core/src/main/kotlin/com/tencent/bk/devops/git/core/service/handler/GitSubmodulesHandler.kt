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
import com.tencent.bk.devops.git.core.enums.PullStrategy
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.service.helper.auth.GitAuthHelperFactory
import com.tencent.bk.devops.git.core.util.EnvHelper
import org.slf4j.LoggerFactory

class GitSubmodulesHandler(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) : IGitHandler {

    companion object {
        private val logger = LoggerFactory.getLogger(GitSubmodulesHandler::class.java)
    }
    private val authHelper by lazy { GitAuthHelperFactory.getAuthHelper(settings = settings, git = git) }

    override fun doHandle() {
        val startEpoch = System.currentTimeMillis()
        try {
            with(settings) {
                if (!submodules) {
                    return
                }
                logger.groupStart("Fetching submodules")
                git.submoduleSync(recursive = nestedSubmodules, path = submodulesPath)
                submoduleClean()
                git.submoduleUpdate(
                    recursive = nestedSubmodules,
                    path = submodulesPath,
                    submoduleRemote = submoduleRemote
                )
                git.submoduleForeach(command = "git config --local gc.auto 0", recursive = nestedSubmodules)
                if (lfs) {
                    git.submoduleForeach(command = "git lfs pull", recursive = nestedSubmodules)
                }
                if (settings.persistCredentials) {
                    authHelper.configureSubmoduleAuth()
                }
                logger.groupEnd("")
            }
        } finally {
            EnvHelper.putContext(
                key = ContextConstants.CONTEXT_SUBMODULE_COST_TIME,
                value = (System.currentTimeMillis() - startEpoch).toString()
            )
        }
    }

    private fun GitSourceSettings.submoduleClean() {
        if (pullStrategy != PullStrategy.REVERT_UPDATE) {
            return
        }
        git.submoduleForeach(
            command = "git reset --hard",
            recursive = nestedSubmodules
        )
        if (enableGitClean) {
            val builder = StringBuilder("git clean -fd ")
            if (enableGitCleanIgnore == true) {
                builder.append(" -x ")
            }
            if (enableGitCleanNested == true) {
                builder.append(" -f ")
            }
            git.submoduleForeach(
                command = builder.toString(),
                recursive = nestedSubmodules
            )
        }
    }
}
