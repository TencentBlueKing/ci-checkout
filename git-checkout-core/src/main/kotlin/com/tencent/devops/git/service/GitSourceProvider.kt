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

package com.tencent.devops.git.service

import com.tencent.devops.git.api.IDevopsApi
import com.tencent.devops.git.constant.GitConstants.BK_CI_GIT_REPO_NAME
import com.tencent.devops.git.constant.GitConstants.BK_CI_GIT_REPO_URL
import com.tencent.devops.git.pojo.GitSourceSettings
import com.tencent.devops.git.service.handler.GitAuthHandler
import com.tencent.devops.git.service.handler.GitCheckoutAndMergeHandler
import com.tencent.devops.git.service.handler.GitFetchHandler
import com.tencent.devops.git.service.handler.GitLfsHandler
import com.tencent.devops.git.service.handler.GitLogHandler
import com.tencent.devops.git.service.handler.GitSubmodulesHandler
import com.tencent.devops.git.service.handler.HandlerExecutionChain
import com.tencent.devops.git.service.handler.InitRepoHandler
import com.tencent.devops.git.service.handler.PrepareWorkspaceHandler
import com.tencent.devops.git.service.helper.GitAuthHelper
import com.tencent.devops.git.util.EnvHelper
import com.tencent.devops.git.util.GitUtil
import java.io.File
import org.slf4j.LoggerFactory

class GitSourceProvider(
    private val settings: GitSourceSettings,
    private val devopsApi: IDevopsApi
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GitSourceProvider::class.java)
    }

    fun getSource() {
        with(settings) {
            logger.info("Syncing repository: ${settings.repositoryUrl}")
            EnvHelper.addEnvVariable(BK_CI_GIT_REPO_URL, settings.repositoryUrl)
            val repositoryName = GitUtil.getServerInfo(settings.repositoryUrl).repositoryName
            EnvHelper.addEnvVariable(BK_CI_GIT_REPO_NAME, repositoryName)
            if (preMerge) {
                if (sourceRepositoryUrl.isBlank() || sourceBranchName.isBlank()) {
                    preMerge = false
                }
            }

            logger.info("Working directory is: $repositoryPath")
            val workingDirectory = File(repositoryPath)
            val git = GitCommandManager(workingDirectory = workingDirectory, lfs = lfs)

            val handlerChain = HandlerExecutionChain(
                listOf(
                    PrepareWorkspaceHandler(settings, git),
                    InitRepoHandler(settings, git),
                    GitAuthHandler(settings, git),
                    GitFetchHandler(settings, git),
                    GitCheckoutAndMergeHandler(settings, git),
                    GitSubmodulesHandler(settings, git),
                    GitLfsHandler(settings, git),
                    GitLogHandler(settings, git, devopsApi)
                )
            )
            try {
                handlerChain.doHandle()
            } finally {
                handlerChain.afterHandle()
            }
        }
    }

    fun cleanUp() {
        with(settings) {
            val workingDirectory = File(repositoryPath)
            if (!workingDirectory.exists()) {
                return
            }
            val git = GitCommandManager(workingDirectory = workingDirectory, lfs = false)
            val authHelper = GitAuthHelper(git = git, settings = settings)
            authHelper.removeAuth()
        }
    }
}
