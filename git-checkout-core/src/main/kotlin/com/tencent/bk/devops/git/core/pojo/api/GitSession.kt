package com.tencent.bk.devops.git.core.pojo.api

data class GitSession(
    val id: String,
    val email: String,
    val username: String,
    val private_token: String
)