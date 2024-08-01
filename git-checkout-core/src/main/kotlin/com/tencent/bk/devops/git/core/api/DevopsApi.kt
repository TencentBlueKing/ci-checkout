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
import com.tencent.bk.devops.atom.api.Header.AUTH_HEADER_PROJECT_ID
import com.tencent.bk.devops.atom.api.SdkEnv
import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_REPOSITORY_HASH_ID
import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.enums.HttpStatus
import com.tencent.bk.devops.git.core.exception.ApiException
import com.tencent.bk.devops.git.core.exception.PermissionForbiddenException
import com.tencent.bk.devops.git.core.i18n.GitErrorsText
import com.tencent.bk.devops.git.core.pojo.api.CommitData
import com.tencent.bk.devops.git.core.pojo.api.CredentialInfo
import com.tencent.bk.devops.git.core.pojo.api.GitToken
import com.tencent.bk.devops.git.core.pojo.api.GithubToken
import com.tencent.bk.devops.git.core.pojo.api.PipelineBuildMaterial
import com.tencent.bk.devops.git.core.pojo.api.Repository
import com.tencent.bk.devops.git.core.pojo.api.RepositoryConfig
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.HttpUtil
import com.tencent.bk.devops.git.core.util.PlaceholderResolver.Companion.defaultResolver
import com.tencent.bk.devops.plugin.pojo.ErrorType
import com.tencent.bk.devops.plugin.pojo.Result
import com.tencent.bk.devops.plugin.utils.JsonUtil
import okhttp3.RequestBody.Companion.toRequestBody

class DevopsApi : IDevopsApi, BaseApi() {

    override fun addCommit(commits: List<CommitData>): Result<Int> {
        val path = "/repository/api/build/commit/addCommit"
        val request = buildPost(path, getJsonRequest(commits), mutableMapOf())
        val responseContent = HttpUtil.retryRequest(
            request = request,
            errorMessage = "Failed to add repository commit information",
            maxAttempts = 1
        )
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
        val responseContent = HttpUtil.retryRequest(
            request = request,
            errorMessage = "Failed to get the last code commit information"
        )
        return JsonUtil.to(responseContent, object : TypeReference<Result<List<CommitData>>>() {})
    }

    override fun saveBuildMaterial(materialList: List<PipelineBuildMaterial>): Result<Int> {
        val path = "/process/api/build/repository/saveBuildMaterial"
        val request = buildPost(path, getJsonRequest(materialList), mutableMapOf())
        val responseContent = HttpUtil.retryRequest(
            request = request,
            errorMessage = "Failed to add source material information",
            maxAttempts = 1
        )
        return JsonUtil.to(responseContent, object : TypeReference<Result<Int>>() {})
    }

    override fun getCredential(credentialId: String, publicKey: String): Result<CredentialInfo> {
        val path = "/ticket/api/build/credentials/$credentialId?publicKey=${encode(publicKey)}"
        val request = buildGet(path)
        val responseContent = HttpUtil.retryRequest(
            request = request,
            errorMessage = "Failed to get credentials"
        )
        val result = JsonUtil.to(responseContent, object : TypeReference<Result<CredentialInfo>>() {})
        if (result.data == null) {
            val projectId = SdkEnv.getSdkHeader()[AUTH_HEADER_PROJECT_ID]
            val errorMsg = GitErrorsText.get().notExistCredential?.let {
                defaultResolver.resolveByMap(it, mapOf(
                    "credentialId" to credentialId,
                    "projectId" to projectId))
            } ?: "Credential does not exist"
            val reason = GitErrorsText.get().notExistCredentialCause?.let {
                defaultResolver.resolveByMap(it, mapOf(
                    "credentialId" to credentialId,
                    "projectId" to projectId))
            } ?: ""
            val solution = GitErrorsText.get().notExistCredentialSolution?.let {
                defaultResolver.resolveByMap(it, mapOf(
                    "credentialId" to credentialId,
                    "projectId" to projectId))
            } ?: ""
            val wiki = GitErrorsText.get().notExistCredentialWiki?.let {
                defaultResolver.resolveByMap(it, mapOf(
                    "credentialId" to credentialId,
                    "projectId" to projectId))
            } ?: ""
            throw ApiException(
                errorType = ErrorType.USER,
                errorCode = GitConstants.CONFIG_ERROR,
                errorMsg = errorMsg,
                reason = reason,
                solution = solution,
                wiki = wiki,
                httpStatus = HttpStatus.NOT_FOUND.statusCode
            )
        }
        return result
    }

    override fun getOauthToken(userId: String): Result<GitToken> {
        val path = "/repository/api/build/oauth/git/$userId"
        val request = buildGet(path)
        try {
            val responseContent = HttpUtil.retryRequest(
                request = request,
                errorMessage = "Failed to get oauth token information"
            )
            val result = JsonUtil.to(responseContent, object : TypeReference<Result<GitToken>>() {})
            if (result.data == null) {
                throw ApiException(
                    errorType = ErrorType.USER,
                    errorCode = GitConstants.CONFIG_ERROR,
                    errorMsg = "User [$userId] has no oauth authorization"
                )
            }
            return result
        } catch (ignored: PermissionForbiddenException) {
            val projectId = SdkEnv.getSdkHeader()[AUTH_HEADER_PROJECT_ID]
            throw PermissionForbiddenException(
                errorType = ignored.errorType,
                errorCode = ignored.errorCode,
                errorMsg = GitErrorsText.get().notPermissionGetOauthToken?.let {
                    defaultResolver.resolveByMap(it, mapOf("userId" to userId))
                } ?: ignored.errorMsg,
                reason = GitErrorsText.get().notPermissionGetOauthTokenCause?.let {
                    defaultResolver.resolveByMap(it, mapOf("userId" to userId, "projectId" to projectId))
                } ?: "",
                solution = GitErrorsText.get().notPermissionGetOauthTokenSolution?.let {
                    defaultResolver.resolveByMap(
                        it,
                        mapOf(
                            "userId" to userId,
                            "projectId" to projectId,
                            "repository_hash_id" to EnvHelper.getContext(CONTEXT_REPOSITORY_HASH_ID)
                        )
                    )
                } ?: "",
                wiki = GitErrorsText.get().notPermissionGetOauthTokenWiki ?: ""
            )
        }
    }

    override fun getGithubOauthToken(userId: String): Result<GithubToken> {
        val path = "/repository/api/build/oauth/github/$userId"
        val request = buildGet(path)
        val responseContent = HttpUtil.retryRequest(request, "Failed to get oauth token information")
        val result = JsonUtil.to(responseContent, object : TypeReference<Result<GithubToken>>() {})
        if (result.data == null) {
            throw ApiException(
                errorType = ErrorType.USER,
                errorCode = GitConstants.CONFIG_ERROR,
                errorMsg = "User [$userId] has no oauth authorization"
            )
        }
        return result
    }

    override fun getRepository(repositoryConfig: RepositoryConfig): Result<Repository> {
        try {
            val path = "/repository/api/build/repositories?" +
                    "repositoryId=${repositoryConfig.getURLEncodeRepositoryId()}&" +
                    "repositoryType=${repositoryConfig.repositoryType.name}"
            val request = buildGet(path)
            val responseContent = HttpUtil.retryRequest(request, "Failed to get repository information")
            val result = JsonUtil.to(responseContent, object : TypeReference<Result<Repository>>() {})
            return result
        } catch (ignored: ApiException) {
            if (ignored.httpStatus == HttpStatus.NOT_FOUND.statusCode) {
                val projectId = SdkEnv.getSdkHeader()[AUTH_HEADER_PROJECT_ID]
                val repositoryId = repositoryConfig.getRepositoryId()
                val errorMsg = GitErrorsText.get().notExistRepository?.let {
                    defaultResolver.resolveByMap(it, mapOf(
                        "repositoryId" to repositoryId,
                        "projectId" to projectId))
                } ?: "Repository does not exist or has been deleted."
                val reason = GitErrorsText.get().notExistRepositoryCause?.let {
                    defaultResolver.resolveByMap(it, mapOf(
                        "repositoryId" to repositoryId,
                        "projectId" to projectId))
                } ?: ""
                val solution = GitErrorsText.get().notExistRepositorySolution?.let {
                    defaultResolver.resolveByMap(it, mapOf(
                        "repositoryId" to repositoryId,
                        "projectId" to projectId))
                } ?: ""
                val wiki = GitErrorsText.get().notExistRepositoryWiki?.let {
                    defaultResolver.resolveByMap(it, mapOf(
                        "repositoryId" to repositoryId,
                        "projectId" to projectId))
                } ?: ""

                throw ApiException(
                    errorType = ignored.errorType,
                    errorCode = ignored.errorCode,
                    errorMsg = errorMsg,
                    reason = reason,
                    solution = solution,
                    wiki = wiki,
                    httpStatus = ignored.httpStatus
                )
            } else {
                throw ignored
            }
        }
    }

    override fun reportAtomMetrics(atomCode: String, data: String): Result<Boolean> {
        val path = "/monitoring/api/build/atom/metrics/report/$atomCode"
        val requestBody = data.toRequestBody(JSON_CONTENT_TYPE)
        val request = buildPost(path, requestBody, mutableMapOf())
        val responseContent = HttpUtil.retryRequest(
            request = request,
            errorMessage = "Failed to report measurement information",
            maxAttempts = 1
        )
        return JsonUtil.to(responseContent, object : TypeReference<Result<Boolean>>() {})
    }
}
