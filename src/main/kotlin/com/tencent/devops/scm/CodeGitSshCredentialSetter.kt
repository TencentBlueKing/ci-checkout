package com.tencent.devops.scm

class CodeGitSshCredentialSetter constructor(
    private val privateKey: String,
    private val passPhrase: String?
) : GitCredentialSetter {
    override fun setGitCredential() {
    }
}
