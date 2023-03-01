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

import com.tencent.bk.devops.git.core.api.IDevopsApi
import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_ALIAS_NAME
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_REF
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_REPO_GIT_WEBHOOK_MR_SOURCE_COMMIT
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_REPO_GIT_WEBHOOK_MR_TARGET_COMMIT
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_REPO_GIT_WEBHOOK_PUSH_AFTER_COMMIT
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_REPO_GIT_WEBHOOK_PUSH_BEFORE_COMMIT
import com.tencent.bk.devops.git.core.constant.GitConstants.GIT_LOG_MAX_COUNT
import com.tencent.bk.devops.git.core.enums.ChannelCode
import com.tencent.bk.devops.git.core.enums.CodeEventType
import com.tencent.bk.devops.git.core.enums.ScmType
import com.tencent.bk.devops.git.core.pojo.CommitLogInfo
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.pojo.api.CommitData
import com.tencent.bk.devops.git.core.pojo.api.CommitMaterial
import com.tencent.bk.devops.git.core.pojo.api.PipelineBuildMaterial
import com.tencent.bk.devops.git.core.pojo.api.RepositoryConfig
import com.tencent.bk.devops.git.core.pojo.api.RepositoryType
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.GitUtil
import com.tencent.bk.devops.git.core.util.RepositoryUtils
import org.slf4j.LoggerFactory

class GitLogHelper(
    private val git: GitCommandManager,
    private val settings: GitSourceSettings,
    private val devopsApi: IDevopsApi
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GitLogHelper::class.java)
    }

    fun saveGitCommit() {
        if (!isSaveCommit()) {
            return
        }
        val repositoryConfig = RepositoryUtils.buildConfig(settings.repositoryUrl)
        val preCommitData = getLastCommitId(
            pipelineId = settings.pipelineId,
            elementId = settings.pipelineTaskId,
            repositoryConfig = repositoryConfig
        )

        val commits = saveCommits(
            preCommitData = preCommitData,
            repositoryConfig = repositoryConfig
        )
        val commitIds = commits.map { it.commit }
        val commitMaterial = git.log().map { log ->
            CommitMaterial(
                lastCommitId = preCommitData?.commit,
                newCommitId = log.commitId,
                newCommitComment = log.commitMessage,
                newCommitAuthor = log.authorName,
                newCommitCommitter = log.committerName,
                commitTimes = commits.size,
                commitIds = commitIds,
                scmType = settings.scmType
            )
        }.first()
        saveBuildMaterial(commitMaterial = commitMaterial)
        EnvHelper.addLogEnv(
            projectName = GitUtil.getServerInfo(settings.repositoryUrl).repositoryName,
            repositoryConfig = repositoryConfig,
            commitMaterial = commitMaterial
        )
    }

    /**
     * codecc不保存代码提交记录
     */
    private fun isSaveCommit(): Boolean {
        return when (System.getenv("BK_CI_START_CHANNEL")) {
            ChannelCode.CODECC.name, ChannelCode.GONGFENGSCAN.name, ChannelCode.CODECC_EE.name -> false
            else -> true
        }
    }

    private fun saveCommits(
        preCommitData: CommitData?,
        repositoryConfig: RepositoryConfig
    ): List<CommitData> {
        val repositoryType = EnvHelper.getEnvVariable(GitConstants.BK_CI_GIT_REPO_TYPE)
        val commits = getLogs(preCommitData)
            .map { log ->
                CommitData(
                    type = ScmType.parse(settings.scmType),
                    pipelineId = settings.pipelineId,
                    buildId = settings.pipelineBuildId,
                    commit = log.commitId,
                    committer = log.committerName,
                    author = log.authorName,
                    commitTime = log.commitTime, // 单位:秒
                    comment = log.commitMessage,
                    repoId = if (repositoryType != RepositoryType.URL.name) {
                        repositoryConfig.repositoryHashId
                    } else {
                        ""
                    },
                    repoName = if (repositoryType != RepositoryType.URL.name) {
                        repositoryConfig.repositoryName
                    } else {
                        ""
                    },
                    elementId = settings.pipelineTaskId,
                    url = settings.repositoryUrl
                )
            }
        if (commits.isEmpty()) {
            // 这次构建没有新的提交
            saveCommit(
                listOf(
                    CommitData(
                        ScmType.parse(settings.scmType),
                        settings.pipelineId,
                        settings.pipelineBuildId,
                        "",
                        "",
                        "",
                        0L,
                        "",
                        repoId = if (repositoryType != RepositoryType.URL.name) {
                            repositoryConfig.repositoryHashId
                        } else {
                            ""
                        },
                        repoName = if (repositoryType != RepositoryType.URL.name) {
                            repositoryConfig.repositoryName
                        } else {
                            ""
                        },
                        elementId = settings.pipelineTaskId,
                        url = settings.repositoryUrl
                    )
                )
            )
        } else {
            saveCommit(commits)
        }
        return commits
    }

    /**
     * 1. 如果是拉取代码库与触发库相同，代码变更记录应与触发的代码变更记录相同
     * 2. 如果拉取代码库与触发库不相同或手工触发，对比上一次构建与本次构建的差异
     */
    @SuppressWarnings("ComplexMethod")
    private fun getLogs(preCommitData: CommitData?): List<CommitLogInfo> {
        val gitHookEventType = System.getenv(GitConstants.BK_CI_REPO_GIT_WEBHOOK_EVENT_TYPE)
        val hookRepoUrl = System.getenv(GitConstants.BK_CI_REPO_WEBHOOK_REPO_URL)
        val isHook = GitUtil.isGitEvent(gitHookEventType) &&
            GitUtil.isSameRepository(
                repositoryUrl = settings.repositoryUrl,
                otherRepositoryUrl = hookRepoUrl,
                hostNameList = settings.compatibleHostList
            )

        return when {
            isHook && gitHookEventType == CodeEventType.PUSH.name && settings.fetchDepth == 0 -> {
                val before = System.getenv(BK_REPO_GIT_WEBHOOK_PUSH_BEFORE_COMMIT)
                val after = System.getenv(BK_REPO_GIT_WEBHOOK_PUSH_AFTER_COMMIT)
                if (before.isNullOrBlank() || after.isNullOrBlank()) {
                    localDiff(preCommitData)
                } else {
                    git.log(
                        maxCount = GIT_LOG_MAX_COUNT,
                        revisionRange = "$before..HEAD"
                    )
                }
            }
            isHook && gitHookEventType == CodeEventType.MERGE_REQUEST.name -> {
                val source = System.getenv(BK_REPO_GIT_WEBHOOK_MR_SOURCE_COMMIT)
                val target = System.getenv(BK_REPO_GIT_WEBHOOK_MR_TARGET_COMMIT)
                if (source.isNullOrBlank() || target.isNullOrBlank()) {
                    localDiff(preCommitData)
                } else {
                    git.log(
                        maxCount = GIT_LOG_MAX_COUNT,
                        revisionRange = "$target...$source"
                    )
                }
            }
            else -> localDiff(preCommitData)
        }
    }

    private fun localDiff(preCommitData: CommitData?): List<CommitLogInfo> {
        return if (preCommitData == null) {
            git.log()
        } else {
            logger.info(
                "previously build commit info|buildId:${preCommitData.buildId}|commitId:${preCommitData.commit}"
            )
            git.log(
                maxCount = GIT_LOG_MAX_COUNT,
                revisionRange = "${preCommitData.commit}..HEAD"
            )
        }
    }

    private fun getLastCommitId(
        pipelineId: String,
        elementId: String,
        repositoryConfig: RepositoryConfig
    ): CommitData? {
        devopsApi.getLatestCommit(pipelineId, elementId, repositoryConfig).data?.forEach { latestCommit ->
            val lastCommitId = latestCommit.commit
            if (git.commitExists(lastCommitId)) {
                return latestCommit
            }
        }
        return null
    }

    private fun saveCommit(commits: List<CommitData>) {
        try {
            devopsApi.addCommit(commits)
        } catch (ignore: Exception) {
            logger.error("save commit fail: ${ignore.message}")
        }
    }

    private fun saveBuildMaterial(commitMaterial: CommitMaterial) {
        val aliasName = EnvHelper.getEnvVariable(BK_CI_GIT_REPO_ALIAS_NAME) ?: return
        devopsApi.saveBuildMaterial(
            listOf(
                PipelineBuildMaterial(
                    aliasName = aliasName,
                    url = settings.repositoryUrl,
                    branchName = EnvHelper.getEnvVariable(BK_CI_GIT_REPO_REF),
                    newCommitId = commitMaterial.newCommitId ?: commitMaterial.lastCommitId,
                    newCommitComment = commitMaterial.newCommitComment,
                    commitTimes = commitMaterial.commitTimes,
                    scmType = commitMaterial.scmType
                )
            )
        )
    }
}
