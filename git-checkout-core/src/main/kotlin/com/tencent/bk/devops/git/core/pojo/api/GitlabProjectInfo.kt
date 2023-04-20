package com.tencent.bk.devops.git.core.pojo.api

import com.fasterxml.jackson.annotation.JsonProperty

class GitlabProjectInfo(
    val id: Long,
    val description: String?,
    val name: String,
    @JsonProperty("name_with_namespace")
    val nameWithNamespace: String,
    @JsonProperty("ssh_url_to_repo")
    val sshUrlToRepo: String,
    @JsonProperty("http_url_to_repo")
    val httpUrlToRepo: String
)