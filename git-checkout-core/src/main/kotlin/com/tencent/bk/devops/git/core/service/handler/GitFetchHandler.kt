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
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.service.helper.RefHelper
import org.slf4j.LoggerFactory

class GitFetchHandler(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) : IGitHandler {

    private val refHelper = RefHelper(settings = settings)
    companion object {
        private val logger = LoggerFactory.getLogger(GitFetchHandler::class.java)
    }

    override fun doHandle() {
        with(settings) {
            logger.groupStart("Fetching the repository")
            fetchTargetRepository()
            fetchSourceRepository()
            logger.groupEnd("")
        }
    }

    private fun GitSourceSettings.fetchSourceRepository() {
        if (preMerge && !sourceRepoUrlEqualsRepoUrl) {
            val refSpec = refHelper.getSourceRefSpec()
            git.fetch(
                refSpec = refSpec,
                fetchDepth = fetchDepth,
                remoteName = GitConstants.DEVOPS_VIRTUAL_REMOTE_NAME,
                preMerge = preMerge,
                enablePartialClone = enablePartialClone
            )
        }
    }

    private fun GitSourceSettings.fetchTargetRepository() {
        val refSpec = if (enableFetchRefSpec) {
            refHelper.getRefSpec()
        } else {
            refHelper.getRefSpecForAllHistory()
        }
        git.fetch(
            refSpec = refSpec,
            fetchDepth = fetchDepth,
            remoteName = GitConstants.ORIGIN_REMOTE_NAME,
            preMerge = preMerge,
            enablePartialClone = enablePartialClone
        )
    }
}
