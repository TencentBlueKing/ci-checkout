package com.tencent.bk.devops.git.core.pojo

data class GitMetricsInfo(
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val taskId: String,
    val url: String,
    val startTime: String,
    val endTime: String,
    val costTime: Long
)
