package com.tencent.devops.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class GitToken(
    @JsonProperty("access_token")
    var accessToken: String = "",
    @JsonProperty("refresh_token")
    var refreshToken: String = "",
    @JsonProperty("token_type")
    val tokenType: String = "",
    @JsonProperty("expires_in")
    val expiresIn: Long = 0L
)
