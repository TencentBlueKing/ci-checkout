package com.tencent.devops.pojo

data class CommitMaterial(
    val lastCommitId: String?,
    val newCommitId: String?,
    val newCommitComment: String?,
    val commitTimes: Int
)
