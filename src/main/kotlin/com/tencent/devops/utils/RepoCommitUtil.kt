package com.tencent.devops.utils

import com.tencent.devops.api.CommitResourceApi
import com.tencent.devops.enums.RepositoryConfig
import com.tencent.devops.enums.ScmType
import com.tencent.devops.pojo.CommitMaterial
import com.tencent.devops.pojo.utils.CommitData
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.LogCommand
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.slf4j.LoggerFactory

object RepoCommitUtil {

    private val commitResourceApi = CommitResourceApi()
    private val logger = LoggerFactory.getLogger(RepoCommitUtil::class.java)

    /**
     * 保存并返回commit信息
     * return (lastCommitId. newCommitId)
     */
    fun saveGitCommit(
        git: Git,
        repo: Repository,
        pipelineId: String,
        buildId: String,
        elementId: String,
        repositoryConfig: RepositoryConfig,
        gitType: ScmType = ScmType.CODE_GIT
    ): CommitMaterial {
        try {
            return doSaveGitCommit(git, repo, pipelineId, buildId, elementId, repositoryConfig, gitType)
        } catch (e: Exception) {
            logger.error("save commit fail: ${e.message}")
        }
        return CommitMaterial(null, null, null, 0)
    }

    private fun doSaveGitCommit(
        git: Git,
        repo: Repository,
        pipelineId: String,
        buildId: String,
        elementId: String,
        repositoryConfig: RepositoryConfig,
        gitType: ScmType
    ): CommitMaterial {
        val logsCommand = git.log()
        val latestCommit = getLastCommitId(pipelineId, elementId, repositoryConfig, repo, logsCommand)
        val latestCommitId = latestCommit?.commit

        val commits = logsCommand.call().map {
            CommitData(
                ScmType.parse(gitType),
                pipelineId,
                buildId,
                it.name,
                it.committerIdent.name,
                it.commitTime.toLong(), // 单位:秒
                it.shortMessage,
                repositoryConfig.repositoryHashId,
                repositoryConfig.repositoryName,
                elementId
            )
        }

        val headCommitId: String
        val headComment: String
        if (commits.isEmpty()) {
            // 这次构建没有新的提交
            headCommitId = latestCommitId ?: ""
            headComment = latestCommit?.comment ?: ""
            saveCommit(
                listOf(
                    CommitData(
                        ScmType.parse(gitType),
                        pipelineId,
                        buildId,
                        "",
                        "",
                        0L,
                        "",
                        repositoryConfig.repositoryHashId,
                        repositoryConfig.repositoryName,
                        elementId
                    )
                )
            )
        } else {
            headCommitId = commits.first().commit
            headComment = commits.first().comment ?: ""
            saveCommit(commits)
        }
        println("head commit: $headCommitId")

        return CommitMaterial(latestCommitId, headCommitId, headComment, commits.size)
    }

    private fun getLastCommitId(pipelineId: String, elementId: String, repositoryConfig: RepositoryConfig, repo: Repository, logsCommand: LogCommand): CommitData? {
        val latestCommitList = commitResourceApi.getLatestCommit(pipelineId, elementId, repositoryConfig).data
        latestCommitList!!.forEach { latestCommit ->
            val lastCommitId = latestCommit.commit
            try {
                logsCommand.not(resolveRev(repo, lastCommitId))
                println("last commit: $lastCommitId")
                return latestCommit
            } catch (e: Exception) {
                println("resolve commit fail($lastCommitId): ${e.message} ")
            }
        }
        logsCommand.setMaxCount(1)
        return null
    }

    private fun saveCommit(commits: List<CommitData>) {
        commitResourceApi.addCommit(commits)
    }

    private fun resolveRev(repo: Repository, rev: String): ObjectId {
        val ref = repo.findRef(rev)
        return if (ref == null) {
            repo.resolve(rev)
        } else {
            getActualRefObjectId(repo, ref)
        }
    }

    private fun getActualRefObjectId(repo: Repository, ref: Ref): ObjectId {
        val repoPeeled = repo.peel(ref)
        return if (repoPeeled.peeledObjectId != null) {
            repoPeeled.peeledObjectId
        } else ref.objectId
    }
}
