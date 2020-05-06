package com.tencent.devops.api

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.devops.atom.api.BaseApi
import com.tencent.bk.devops.atom.pojo.Result
import com.tencent.bk.devops.atom.utils.json.JsonUtil
import com.tencent.devops.enums.RepositoryConfig
import com.tencent.devops.pojo.utils.CommitData

class CommitResourceApi : BaseApi() {

    fun addCommit(commits: List<CommitData>): Result<Int> {
        val path = "/repository/api/build/commit/addCommit"
        val request = buildPost(path, getJsonRequest(commits), mutableMapOf())
        val responseContent = request(request, "添加代码库commit信息失败")
        return JsonUtil.fromJson(responseContent, object : TypeReference<Result<Int>>() {})
    }

    fun getLatestCommit(pipelineId: String, elementId: String, repositoryConfig: RepositoryConfig): Result<List<CommitData>> {
        val path = "/repository/api/build/commit/getLatestCommit?pipelineId=$pipelineId&elementId=$elementId" +
                "&repoId=${repositoryConfig.getRepositoryId()}&repositoryType=${repositoryConfig.repositoryType.name}"
        val request = buildGet(path)
        val responseContent = request(request, "获取最后一次代码commit信息失败")
        return JsonUtil.fromJson(responseContent, object : TypeReference<Result<List<CommitData>>>() {})
    }
}
