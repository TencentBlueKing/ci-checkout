package com.tencent.devops.pojo

import com.tencent.devops.enums.GitPullModeType

data class GitPullMode(
    val type: GitPullModeType,
    val value: String
)
