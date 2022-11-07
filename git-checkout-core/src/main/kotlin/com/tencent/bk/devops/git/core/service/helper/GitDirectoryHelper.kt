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

import com.tencent.bk.devops.git.core.constant.GitConstants.GIT_LFS_SKIP_SMUDGE
import com.tencent.bk.devops.git.core.enums.PullStrategy
import com.tencent.bk.devops.git.core.enums.PullType
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.util.FileUtils
import com.tencent.bk.devops.git.core.util.GitUtil
import org.slf4j.LoggerFactory
import java.io.File

@Suppress("TooGenericExceptionCaught")
class GitDirectoryHelper(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) {

    private val logger = LoggerFactory.getLogger(GitDirectoryHelper::class.java)

    fun prepareExistingDirectory() {
        with(settings) {
            val remove = when (pullStrategy) {
                PullStrategy.FRESH_CHECKOUT ->
                    true
                PullStrategy.REVERT_UPDATE -> {
                    revertUpdate()
                }
                else ->
                    false
            }
            if (remove) {
                logger.info("Deleting the contents of $repositoryPath")
                FileUtils.deleteRepositoryFile(repositoryPath)
            }
        }
    }

    private fun GitSourceSettings.revertUpdate(): Boolean {
        var remove = false
        when {
            !File(repositoryPath, ".git").exists() ->
                remove = false
            !GitUtil.isSameRepository(
                repositoryUrl = repositoryUrl,
                otherRepositoryUrl = git.tryGetFetchUrl(),
                hostNameList = compatibleHostList
            ) ->
                remove = true
            else -> {
                removeLockFile(repositoryPath)
                try {
                    if (enableGitClean) {
                        remove = clean()
                    }
                    if (remove) {
                        logger.warn(
                            "Unable to clean or reset the repository. " +
                                "The repository will be recreated instead."
                        )
                    }
                    // 如果拉取类型是tag,先删除旧的tag再拉取,防止切换到旧的tag
                    if (settings.pullType == PullType.TAG) {
                        git.tagDelete(settings.ref)
                    }
                } catch (ignore: Exception) {
                    logger.warn(
                        "Unable to prepare the existing repository. " +
                            "The repository will be recreated instead."
                    )
                    remove = true
                }
            }
        }
        return remove
    }

    private fun clean(): Boolean {
        var remove = false
        // 清理阶段可能会拉取lfs，但是此时还没有权限，应该禁用lfs拉取
        git.setEnvironmentVariable(GIT_LFS_SKIP_SMUDGE, "1")
        if (!git.tryClean(settings.enableGitCleanIgnore, settings.enableGitCleanNested)) {
            logger.info(
                "The clean command failed. This might be caused by: " +
                    "1) path too long, " +
                    "2) permission issue, or " +
                    "3) file in use. For futher investigation, " +
                    "manually run 'git clean -ffdx' on the directory '\$repositoryPath."
            )
            remove = true
        } else if (git.headExists() && !git.tryReset("HEAD")) {
            // If the last checkout failed, then the HEAD is empty
            remove = true
        }
        git.removeEnvironmentVariable(GIT_LFS_SKIP_SMUDGE)
        return remove
    }

    private fun removeLockFile(repositoryPath: String) {
        val lockFiles = mutableListOf(
            File(repositoryPath, ".git/index.lock"),
            File(repositoryPath, ".git/shallow.lock"),
            File(repositoryPath, ".git/config.lock")
        )
        lockFiles.addAll(findRefLockFile(File(repositoryPath, ".git/refs")))
        lockFiles.forEach { lockFile ->
            try {
                lockFile.delete()
            } catch (e: Exception) {
                logger.error("Unable to delete ${lockFile.name}", e.message)
            }
        }
    }

    private fun findRefLockFile(directory: File): List<File> {
        return org.apache.commons.io.FileUtils.listFiles(directory, arrayOf("lock"), true).toList()
    }
}
