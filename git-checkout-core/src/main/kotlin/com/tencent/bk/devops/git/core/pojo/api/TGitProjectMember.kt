package com.tencent.bk.devops.git.core.pojo.api

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 工蜂项目成员信息
 */
data class TGitProjectMember(
    val id: Long,
    val username: String,
    @JsonProperty("web_url")
    val webUrl: String,
    val name: String,
    val state: String,
    @JsonProperty("avatarUrl")
    val avatarUrl: String?,
    @JsonProperty("access_level")
    val accessLevel: Int
)
