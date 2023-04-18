package com.tencent.bk.devops.git.core.pojo.api

class GitlabProjectInfo(
    val id: Long,
    val description: String?,
    val name: String,
    val name_with_namespace: String,
    val ssh_url_to_repo: String,
    val http_url_to_repo: String
)