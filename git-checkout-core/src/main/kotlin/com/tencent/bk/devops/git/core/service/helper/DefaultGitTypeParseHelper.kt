package com.tencent.bk.devops.git.core.service.helper

import com.tencent.bk.devops.git.core.enums.ScmType

class DefaultGitTypeParseHelper : IGitTypeParseHelper {

    override fun getScmType(hostName: String): ScmType {
        return when {
            hostName.contains("github.com") -> {
                ScmType.GITHUB
            }
            hostName.contains("git.tencent.com") || hostName.contains("git.code.tencent.com") -> {
                ScmType.CODE_TGIT
            }
            else -> {
                ScmType.CODE_GITLAB
            }
        }
    }
}
