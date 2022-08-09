package com.tencent.devops.git.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.bk.devops.atom.pojo.AtomBaseParam
import com.tencent.bk.devops.git.core.enums.AuthType

class CheckoutAtomParam : AtomBaseParam() {
    var repositoryType: String = "ID"
    var repositoryHashId: String? = null
    var repositoryName: String? = null
    var repositoryUrl: String = ""
    var authType: AuthType? = null
    var authUserId: String? = null
    var ticketId: String? = null
    var accessToken: String? = null
    var personalAccessToken: String? = null
    var username: String? = null
    var password: String? = null
    var pullType: String = "BRANCH"
    var refName: String = "master"
    var localPath: String? = null
    var strategy: String = "REVERT_UPDATE"
    var fetchDepth: Int = 0
    val enableFetchRefSpec: Boolean = false
    val fetchRefSpec: String? = null
    var enableGitLfs: Boolean = true
    var enableVirtualMergeBranch: Boolean = true

    var enableSubmodule: Boolean = true
    var submodulePath: String? = ""
    var enableSubmoduleRemote: Boolean = false
    var enableSubmoduleRecursive: Boolean? = false

    var autoCrlf: String? = ""
    var enableGitClean: Boolean = true
    val enableGitCleanIgnore: Boolean = true
    var includePath: String? = ""
    var excludePath: String? = ""

    // 非前端传递的参数
    @JsonProperty("pipeline.start.type")
    val pipelineStartType: String? = null
    val hookEventType: String? = null
    val hookSourceBranch: String? = null
    val hookTargetBranch: String? = null
    val hookSourceUrl: String? = null
    val hookTargetUrl: String? = null
    @JsonProperty("git_mr_number")
    val gitMrNumber: String? = null

    // 重试时检出的commitId
    var retryStartPoint: String? = ""
    var persistCredentials: Boolean? = true
    var enableTrace: Boolean? = false
    /**
     * 是否开启部分克隆,部分克隆只有git版本大于2.22.0才可以使用
     */
    var enablePartialClone: Boolean? = false

    /**
     * 归档的缓存路径
     */
    val cachePath: String = ""
    val usernameConfig: String? = null
    val userEmailConfig: String? = null
}
