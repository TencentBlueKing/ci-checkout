/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.devops.git.core.api

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.devops.atom.api.BaseApi
import com.tencent.bk.devops.atom.exception.RemoteServiceException
import com.tencent.bk.devops.git.core.exception.ExceptionTranslator
import com.tencent.bk.devops.git.core.pojo.api.CommitData
import com.tencent.bk.devops.git.core.pojo.api.CredentialInfo
import com.tencent.bk.devops.git.core.pojo.api.GitToken
import com.tencent.bk.devops.git.core.pojo.api.PipelineBuildMaterial
import com.tencent.bk.devops.git.core.pojo.api.Repository
import com.tencent.bk.devops.git.core.pojo.api.RepositoryConfig
import com.tencent.bk.devops.git.core.service.helper.RetryHelper
import com.tencent.bk.devops.plugin.pojo.Result
import com.tencent.bk.devops.plugin.utils.JsonUtil
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory

class DevopsApi : IDevopsApi, BaseApi() {

    companion object {
        private const val connectTimeout = 5L
        private const val readTimeout = 30L
        private const val writeTimeout = 30L
        private val logger = LoggerFactory.getLogger(DevopsApi::class.java)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(connectTimeout, TimeUnit.SECONDS)
        .readTimeout(readTimeout, TimeUnit.SECONDS)
        .writeTimeout(writeTimeout, TimeUnit.SECONDS)
        .build()

    override fun addCommit(commits: List<CommitData>): Result<Int> {
        val path = "/repository/api/build/commit/addCommit"
        val request = buildPost(path, getJsonRequest(commits), mutableMapOf())
        val responseContent = retryRequest(request, "添加代码库commit信息失败", 1)
        return JsonUtil.to(responseContent, object : TypeReference<Result<Int>>() {})
    }

    override fun getLatestCommit(
        pipelineId: String,
        elementId: String,
        repositoryConfig: RepositoryConfig
    ): Result<List<CommitData>> {
        val path = "/repository/api/build/commit/getLatestCommit?pipelineId=$pipelineId&elementId=$elementId" +
            "&repoId=${repositoryConfig.getRepositoryId()}&repositoryType=${repositoryConfig.repositoryType.name}"
        val request = buildGet(path)
        val responseContent = retryRequest(request, "获取最后一次代码commit信息失败")
        return JsonUtil.to(responseContent, object : TypeReference<Result<List<CommitData>>>() {})
    }

    override fun saveBuildMaterial(materialList: List<PipelineBuildMaterial>): Result<Int> {
        val path = "/process/api/build/repository/saveBuildMaterial"
        val request = buildPost(path, getJsonRequest(materialList), mutableMapOf())
        val responseContent = retryRequest(request, "添加源材料信息失败", 1)
        return JsonUtil.to(responseContent, object : TypeReference<Result<Int>>() {})
    }

    override fun getCredential(credentialId: String, publicKey: String): Result<CredentialInfo> {
        val path = "/ticket/api/build/credentials/$credentialId?publicKey=${encode(publicKey)}"
        val request = buildGet(path)
        val responseContent = retryRequest(request, "获取凭据失败")
        return JsonUtil.to(responseContent, object : TypeReference<Result<CredentialInfo>>() {})
    }

    override fun getOauthToken(userId: String): Result<GitToken> {
        val path = "/repository/api/build/oauth/git/$userId"
        val request = buildGet(path)
        val responseContent = retryRequest(request, "获取oauth认证信息失败")
        return JsonUtil.to(responseContent, object : TypeReference<Result<GitToken>>() {})
    }

    override fun getRepository(repositoryConfig: RepositoryConfig): Result<Repository> {
        val path = "/repository/api/build/repositories?" +
            "repositoryId=${repositoryConfig.getURLEncodeRepositoryId()}&" +
            "repositoryType=${repositoryConfig.repositoryType.name}"
        val request = buildGet(path)
        val responseContent = retryRequest(request, "获取代码库失败")
        return JsonUtil.to(responseContent, object : TypeReference<Result<Repository>>() {})
    }

    override fun reportAtomMetrics(atomCode: String, data: String): Result<Boolean> {
        val path = "/monitoring/api/build/atom/metrics/report/$atomCode"
        val requestBody = RequestBody.create(JSON_CONTENT_TYPE, data)
        val request = buildPost(path, requestBody, mutableMapOf())
        val responseContent = retryRequest(request, "上报插件度量信息失败", 1)
        return JsonUtil.to(responseContent, object : TypeReference<Result<Boolean>>() {})
    }

    private fun retryRequest(request: Request, errorMessage: String, maxAttempts: Int = 3): String {
        return RetryHelper(maxAttempts = maxAttempts).execute {
            try {
                okHttpClient.newCall(request).execute().use { response ->
                    val responseContent = response.body()?.string() ?: ""
                    if (!response.isSuccessful) {
                        logger.error(
                            "Fail to request($request) with code ${response.code()} " +
                                "message ${response.message()} and response $responseContent"
                        )
                        throw RemoteServiceException(errorMessage, response.code(), response.body()?.string() ?: "")
                    }
                    responseContent
                }
            } catch (ignore: Exception) {
                throw ExceptionTranslator.apiExceptionTranslator(ignore)
            }
        }
    }
}
