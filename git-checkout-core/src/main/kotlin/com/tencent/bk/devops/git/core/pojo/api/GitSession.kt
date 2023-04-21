package com.tencent.bk.devops.git.core.pojo.api

import com.fasterxml.jackson.annotation.JsonProperty

data class GitSession(
    val id: String,
    val email: String,
    val username: String,
    @JsonProperty("private_token")
    val privateToken: String
)
