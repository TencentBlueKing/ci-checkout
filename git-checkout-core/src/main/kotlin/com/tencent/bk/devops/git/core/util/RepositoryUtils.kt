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

package com.tencent.bk.devops.git.core.util

import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_ID
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_NAME
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_TYPE
import com.tencent.bk.devops.git.core.enums.ScmType
import com.tencent.bk.devops.git.core.pojo.api.CodeGitRepository
import com.tencent.bk.devops.git.core.pojo.api.CodeGitlabRepository
import com.tencent.bk.devops.git.core.pojo.api.CodeTGitRepository
import com.tencent.bk.devops.git.core.pojo.api.GithubRepository
import com.tencent.bk.devops.git.core.pojo.api.Repository
import com.tencent.bk.devops.git.core.pojo.api.RepositoryConfig
import com.tencent.bk.devops.git.core.pojo.api.RepositoryType

object RepositoryUtils {

    fun buildConfig(repositoryUrl: String): RepositoryConfig {
        val repositoryType = EnvHelper.getEnvVariable(BK_CI_GIT_REPO_TYPE)
        val repositoryId = EnvHelper.getEnvVariable(BK_CI_GIT_REPO_ID)
        val repositoryName = EnvHelper.getEnvVariable(BK_CI_GIT_REPO_NAME)
        return when {
            repositoryType == RepositoryType.ID.name && repositoryId != null ->
                RepositoryConfig(repositoryId, null, RepositoryType.ID)
            repositoryId == RepositoryType.NAME.name && repositoryName != null ->
                RepositoryConfig(null, repositoryName, RepositoryType.NAME)
            else ->
                RepositoryConfig(
                    null,
                    GitUtil.getServerInfo(repositoryUrl).repositoryName,
                    RepositoryType.NAME
                )
        }
    }

    fun buildConfig(repositoryId: String, repositoryType: RepositoryType?) =
        if (repositoryType == null || repositoryType == RepositoryType.ID) {
            RepositoryConfig(repositoryId, null, RepositoryType.ID)
        } else {
            RepositoryConfig(null, repositoryId, RepositoryType.NAME)
        }

    fun getScmType(repository: Repository): ScmType {
        return when (repository) {
            is CodeGitRepository -> ScmType.CODE_GIT
            is CodeGitlabRepository -> ScmType.CODE_GITLAB
            is GithubRepository -> ScmType.GITHUB
            is CodeTGitRepository -> ScmType.CODE_TGIT
            else ->
                ScmType.CODE_GIT
        }
    }

    fun getGitProjectId(repository: Repository) = when (repository) {
        is CodeGitRepository -> repository.gitProjectId
        is CodeGitlabRepository -> repository.gitProjectId
        is CodeTGitRepository -> repository.gitProjectId
        else -> 0L
    }
}
