package com.tencent.bk.devops.git.core.pojo.api

import com.tencent.bk.devops.git.core.enums.RepoAuthType

data class ScmGitRepository(
    override val aliasName: String,
    override val url: String,
    override val credentialId: String,
    override val projectName: String,
    override var userName: String,
    val authType: RepoAuthType? = RepoAuthType.SSH,
    override var projectId: String?,
    override val repoHashId: String?,
    val gitProjectId: Long?,
    val atom: Boolean? = false,
    override val scmCode: String,
    val credentialType: String? = ""
) : Repository {
    companion object {
        const val classType = "scmGit"
    }
}