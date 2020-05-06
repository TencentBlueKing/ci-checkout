package com.tencent.devops.pojo.utils

data class CommitData(
    val type: Short, // 1-svn, 2-git, 3-gitlab
    val pipelineId: String,
    val buildId: String,
    val commit: String,
    val committer: String,
    val commitTime: Long,
    val comment: String?,
    val repoId: String?,
    val repoName: String?,
    val elementId: String
)
