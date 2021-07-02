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

package com.tencent.devops.git.service.helper

import com.tencent.devops.git.api.IDevopsApi
import com.tencent.devops.git.constant.GitConstants.BK_CI_GIT_REPO_ALIAS_NAME
import com.tencent.devops.git.constant.GitConstants.GIT_LOG_MAX_COUNT
import com.tencent.devops.git.enums.ScmType
import com.tencent.devops.git.pojo.GitSourceSettings
import com.tencent.devops.git.pojo.api.CommitData
import com.tencent.devops.git.pojo.api.CommitMaterial
import com.tencent.devops.git.pojo.api.PipelineBuildMaterial
import com.tencent.devops.git.pojo.api.RepositoryConfig
import com.tencent.devops.git.service.GitCommandManager
import com.tencent.devops.git.util.EnvHelper
import com.tencent.devops.git.util.RepositoryUtils
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
        val currCommitData = if (commits.isEmpty()) {
            preCommitData
        } else {
            commits.first()
        }
        val commitMaterial = CommitMaterial(
            lastCommitId = preCommitData?.commit,
            newCommitId = currCommitData?.commit,
            newCommitComment = currCommitData?.comment,
            newCommitAuthor = currCommitData?.author,
            newCommitCommitter = currCommitData?.committer,
            commitTimes = commits.size,
            commitIds = commitIds
        )
        saveBuildMaterial(commitMaterial = commitMaterial)
        EnvHelper.addLogEnv(
            commitMaterial = commitMaterial,
            elementId = settings.pipelineTaskId
        )
    }

    private fun saveCommits(
        preCommitData: CommitData?,
        repositoryConfig: RepositoryConfig
    ): List<CommitData> {

        val gitLogs = if (preCommitData == null) {
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
        val commits = gitLogs
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
                    repoId = repositoryConfig.repositoryHashId,
                    repoName = repositoryConfig.repositoryName,
                    elementId = settings.pipelineTaskId
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
                        repositoryConfig.repositoryHashId,
                        repositoryConfig.repositoryName,
                        settings.pipelineTaskId
                    )
                )
            )
        } else {
            saveCommit(commits)
        }
        return commits
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
                    branchName = settings.ref,
                    newCommitId = commitMaterial.newCommitId ?: commitMaterial.lastCommitId,
                    newCommitComment = commitMaterial.newCommitComment,
                    commitTimes = commitMaterial.commitTimes
                )
            )
        )
    }
}
