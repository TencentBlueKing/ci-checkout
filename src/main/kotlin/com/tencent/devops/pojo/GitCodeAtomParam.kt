package com.tencent.devops.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.bk.devops.atom.pojo.AtomBaseParam
import com.tencent.devops.enums.CodePullStrategy
import com.tencent.devops.enums.GitPullModeType
import lombok.Data
import lombok.EqualsAndHashCode

@Data
@EqualsAndHashCode(callSuper = true)
class GitCodeAtomParam : AtomBaseParam() {
    var repositoryUrl: String = ""
    var ticketId: String? = null
    var localPath: String? = null
    var strategy: CodePullStrategy = CodePullStrategy.REVERT_UPDATE
    var enableSubmodule: Boolean = true
    var enableVirtualMergeBranch: Boolean = false
    var enableSubmoduleRemote: Boolean = false
    var enableAutoCrlf: Boolean = true
    var pullType: GitPullModeType = GitPullModeType.BRANCH
    var refName: String = "master"
    var includePath: String? = ""
    var excludePath: String? = ""
    var fetchDepth: Int? = null
    var enableGitClean: Boolean = true
    var accessToken: String? = null
    var username: String? = null
    var password: String? = null

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
    val noScmVariable: Boolean? = null
}
