package com.tencent.devops.api

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.devops.atom.api.BaseApi
import com.tencent.bk.devops.atom.pojo.Result
import com.tencent.bk.devops.atom.utils.json.JsonUtil
import com.tencent.devops.pojo.GitToken

class OauthResourceApi : BaseApi() {

    fun get(userId: String): Result<GitToken> {
        val path = "/repository/api/build/oauth/git/$userId"
        val request = buildGet(path)
        val responseContent = request(request, "获取oauth认证信息失败")
        return JsonUtil.fromJson(responseContent, object : TypeReference<Result<GitToken>>() {})
    }
}
