package com.tencent.bk.devops.git.core.pojo.api

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 工蜂项目信息
 */
data class TGitProjectInfo(
    val id: Long,
    val description: String?,
    val public: Boolean,
    @JsonProperty("visibility_level")
    val visibilityLevel: Int,
    val name: String,
    @JsonProperty("name_with_namespace")
    val nameWithNamespace: String,
    val path: String
)
