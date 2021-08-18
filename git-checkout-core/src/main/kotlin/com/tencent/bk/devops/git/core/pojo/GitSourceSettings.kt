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

package com.tencent.bk.devops.git.core.pojo

import com.tencent.bk.devops.git.core.enums.PullStrategy
import com.tencent.bk.devops.git.core.enums.PullType
import com.tencent.bk.devops.git.core.enums.ScmType
import com.tencent.bk.devops.git.core.util.GitUtil

data class GitSourceSettings(

    // 系统参数
    val bkWorkspace: String,
    val pipelineId: String,
    val pipelineTaskId: String,
    val pipelineBuildId: String,
    val postEntryParam: String? = "false",

    val scmType: ScmType,
    /***
     * 仓库url
     */
    val repositoryUrl: String,
    /**
     * The location on disk where the repository will be placed
     */
    val repositoryPath: String,
    /**
     * The ref to fetch
     */
    val ref: String,
    /**
     * 拉取类型
     */
    val pullType: PullType = PullType.BRANCH,
    /**
     * The commit to checkout and the value exists only when retrying
     */
    val commit: String = "",
    /**
     * 拉取策略
     */
    val pullStrategy: PullStrategy = PullStrategy.REVERT_UPDATE,
    /**
     * Indicates whether to clean the repository
     */
    val enableGitClean: Boolean,
    /**
     * 是否清理配置在.gitignore里面的文件
     */
    val enableGitCleanIgnore: Boolean? = true,

    /**
     * The depth when fetching
     */
    var fetchDepth: Int = 0,

    val enableFetchRefSpec: Boolean? = false,
    /**
     * 拉取指定的分支，以,分割
     */
    val fetchRefSpec: String? = null,

    /**
     * Indicates whether to fetch LFS objects
     */
    val lfs: Boolean = true,

    /**
     * Indicates whether to checkout submodules
     */
    val submodules: Boolean = true,

    /**
     * Indicates whether to recursively checkout submodules
     */
    val nestedSubmodules: Boolean = false,

    /**
     * 子模块初始化是否同步远程仓库，--remote
     */
    val submoduleRemote: Boolean = false,

    /**
     * specified submodule path when fetching
     */
    val submodulesPath: String = "",

    /**
     * Indicates whether to sparse checkout include
     */
    var includeSubPath: String? = "",
    /**
     *
     */
    var excludeSubPath: String? = "",

    /**
     * 指定授权用户
     */
    var authUserId: String? = "",

    /**
     * if auth type is oauth, the username value is oauth2.
     */
    val username: String? = null,

    /**
     * The password to use when fetching the repository
     */
    val password: String? = null,

    val privateKey: String? = null,

    val passPhrase: String? = null,

    /**
     * Indicates whether to persist the credentials on disk to enable scripting authenticated git commands
     */
    val persistCredentials: Boolean = false,

    /**
     * Indicates whether to merge source repository
     */
    var preMerge: Boolean = false,
    /**
     * The MR/PR's source repository url
     */
    var sourceRepositoryUrl: String = "",
    /**
     * The MR/PR's source branch name
     */
    var sourceBranchName: String = "",

    /**配置参数**/
    var autoCrlf: String? = "",
    var usernameConfig: String? = "",
    var userEmailConfig: String? = "",

    /**
     * 域名个兼容
     *
     * 当仓库服务有多个域名时使用
     */
    var compatibleHostList: List<String>? = null,

    /**
     * 是否开启调试功能
     */
    val enableTrace: Boolean? = false,

    /**
     * 是否开启部分克隆,部分克隆只有git版本大于2.22.0才可以使用
     */
    var enablePartialClone: Boolean? = false,
    /**
     * 缓存路径:自定义的制品库路径,保存仓库的.git压缩文件
     */
    val cachePath: String? = ""
) {
    val sourceRepoUrlEqualsRepoUrl: Boolean
        get() = GitUtil.isSameRepository(
            repositoryUrl = repositoryUrl,
            otherRepositoryUrl = sourceRepositoryUrl,
            hostNameList = compatibleHostList
        )
}
