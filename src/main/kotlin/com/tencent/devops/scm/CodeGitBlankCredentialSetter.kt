package com.tencent.devops.scm

class CodeGitBlankCredentialSetter constructor() : GitCredentialSetter {
    override fun setGitCredential() {
    }

    override fun getCredentialUrl(url: String): String {
        return url
    }
}
