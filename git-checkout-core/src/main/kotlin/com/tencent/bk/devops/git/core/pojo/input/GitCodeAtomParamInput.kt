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

package com.tencent.bk.devops.git.core.pojo.input

import com.tencent.bk.devops.git.core.enums.PullStrategy
import com.tencent.bk.devops.git.core.enums.PullType
import com.tencent.bk.devops.git.core.pojo.api.RepositoryType

@SuppressWarnings("ALL")
data class GitCodeAtomParamInput(
    // 系统参数
    val bkWorkspace: String = "",
    val pipelineId: String = "",
    val pipelineTaskId: String = "",
    val pipelineBuildId: String = "",
    val pipelineStartUserName: String = "",
    val postEntryParam: String? = "false",

    var repositoryType: String = RepositoryType.ID.name,
    var repositoryHashId: String? = null,
    var repositoryName: String? = null,
    var localPath: String? = null,
    var strategy: String = PullStrategy.REVERT_UPDATE.name,
    var enableSubmodule: Boolean = true,
    var submodulePath: String? = "",
    var enableVirtualMergeBranch: Boolean = true,
    var enableSubmoduleRemote: Boolean = false,
    var enableSubmoduleRecursive: Boolean? = true,
    var autoCrlf: String? = "",
    var pullType: String = PullType.BRANCH.name,
    var branchName: String = "master",
    var tagName: String? = "",
    var commitId: String? = "",
    var includePath: String? = "",
    var excludePath: String? = "",
    var fetchDepth: Int? = null,
    val enableFetchRefSpec: Boolean? = false,
    val fetchRefSpec: String? = null,
    var enableGitClean: Boolean = true,
    var enableGitCleanIgnore: Boolean = true,
    var enableGitCleanNested: Boolean = false,
    var enableGitLfs: Boolean = false,
    /**
     * lfs并发上传下载的数量
     */
    val lfsConcurrentTransfers: Int? = 0,

    // 非前端传递的参数
    val pipelineStartType: String? = null,
    val hookEventType: String? = null,
    val hookSourceBranch: String? = null,
    val hookTargetBranch: String? = null,
    val hookSourceUrl: String? = null,
    val hookTargetUrl: String? = null,

    // 重试时检出的commitId
    var retryStartPoint: String? = "",
    var persistCredentials: Boolean,
    var compatibleHostList: List<String>? = null,
    val enableTrace: Boolean? = false,

    var usernameConfig: String? = "",
    var userEmailConfig: String? = "",
    val enablePartialClone: Boolean? = false,
    /**
     * 缓存路径:自定义的制品库路径,保存仓库的.git压缩文件
     */
    val cachePath: String? = "",
    /**
     * 是否开启全局insteadOf
     */
    val enableGlobalInsteadOf: Boolean = false,
    /**
     * 是否使用自定义凭证
     *
     * 只要是http[s]，都是用自定义的checkout凭证,不管有没有配置全局的凭证
     */
    val useCustomCredential: Boolean = false
)
