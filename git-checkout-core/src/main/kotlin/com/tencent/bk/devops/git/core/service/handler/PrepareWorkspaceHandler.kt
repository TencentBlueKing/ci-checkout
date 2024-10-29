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

import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_CACHE_DOWNLOAD_COST_TIME
import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_CACHE_DOWNLOAD_RESULT
import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_PREPARE_COST_TIME
import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_VM_EXIST_REPO
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.service.helper.GitCacheHelperFactory
import com.tencent.bk.devops.git.core.service.helper.GitDirectoryHelper
import com.tencent.bk.devops.git.core.service.helper.VersionHelper
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.FileUtils
import org.slf4j.LoggerFactory
import java.io.File

class PrepareWorkspaceHandler(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) : IGitHandler {

    companion object {
        private val logger = LoggerFactory.getLogger(PrepareWorkspaceHandler::class.java)
    }

    override fun doHandle() {
        val startEpoch = System.currentTimeMillis()
        try {
            doHandlePrepare()
        } finally {
            EnvHelper.putContext(CONTEXT_PREPARE_COST_TIME, (System.currentTimeMillis() - startEpoch).toString())
        }
    }

    private fun doHandlePrepare() {
        with(settings) {
            val workingDirectory = File(repositoryPath)
            if (!workingDirectory.exists()) {
                workingDirectory.mkdirs()
            }
            val gitVersion = git.getGitVersion()
            settings.gitVersion = VersionHelper.computeGitVersion(gitVersion)
            // 如果缓存锁存在,说明上次构建下载缓存没有成功,需要先清理工作空间
            if (File(repositoryPath, ".git/cache.lock").exists()) {
                logger.warn("previously build download from cache repository failed,cleaning workspace")
                FileUtils.deleteRepositoryFile(repositoryPath)
            }
            if (File(repositoryPath, ".git").exists()) {
                EnvHelper.putContext(CONTEXT_VM_EXIST_REPO, "1")
            } else {
                EnvHelper.putContext(CONTEXT_VM_EXIST_REPO, "0")
                // 如果仓库不存在,并且配置了开启缓存加速,则先从缓存库下载
                downloadCacheRepo()
            }

            // 不要和上面的代码库合到一块，双重检测，如果有缓存,会初始化工作空间
            if (File(repositoryPath, ".git").exists()) {
                logger.groupStart("cleaning workspace")
                GitDirectoryHelper(git = git, settings = settings).prepareExistingDirectory()
                logger.groupEnd("")
            }
        }
    }

    private fun GitSourceSettings.downloadCacheRepo() {
        val startEpoch = System.currentTimeMillis()
        val cacheRepoHelper = GitCacheHelperFactory.getCacheHelper(settings) ?: return
        val name = cacheRepoHelper.getName()
        logger.groupStart("download cache from $name")
        val downloadResult = cacheRepoHelper.download(
            settings = settings,
            git = git
        )
        if (downloadResult) {
            logger.info("Success to download cache from $name")
            EnvHelper.putContext(CONTEXT_CACHE_DOWNLOAD_RESULT, "success")
        } else {
            logger.error("Failed to download cache repository from $name,cleaning workspace")
            FileUtils.deleteRepositoryFile(repositoryPath)
            EnvHelper.putContext(CONTEXT_CACHE_DOWNLOAD_RESULT, "failed")
        }
        EnvHelper.putContext(
            CONTEXT_CACHE_DOWNLOAD_COST_TIME,
            (System.currentTimeMillis() - startEpoch).toString()
        )
        logger.groupEnd("")
    }
}
