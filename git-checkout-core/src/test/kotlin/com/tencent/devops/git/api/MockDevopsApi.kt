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

package com.tencent.devops.git.api

import com.tencent.bk.devops.plugin.pojo.Result
import com.tencent.devops.git.pojo.api.CommitData
import com.tencent.devops.git.pojo.api.CredentialInfo
import com.tencent.devops.git.pojo.api.CredentialType
import com.tencent.devops.git.pojo.api.GitToken
import com.tencent.devops.git.pojo.api.PipelineBuildMaterial
import com.tencent.devops.git.pojo.api.Repository
import com.tencent.devops.git.pojo.api.RepositoryConfig

class MockDevopsApi : IDevopsApi {
    override fun addCommit(commits: List<CommitData>): Result<Int> {
        return Result(0)
    }

    override fun getLatestCommit(
        pipelineId: String,
        elementId: String,
        repositoryConfig: RepositoryConfig
    ): Result<List<CommitData>> {
        return Result(emptyList())
    }

    override fun saveBuildMaterial(materialList: List<PipelineBuildMaterial>): Result<Int> {
        return Result(0)
    }

    override fun getCredential(credentialId: String, publicKey: String): Result<CredentialInfo> {
        return Result(CredentialInfo(publicKey = "", credentialType = CredentialType.ACCESSTOKEN, v1 = ""))
    }

    override fun getOauthToken(userId: String): Result<GitToken> {
        return Result(GitToken())
    }

    override fun getRepository(repositoryConfig: RepositoryConfig): Result<Repository> {
        return Result()
    }
}
