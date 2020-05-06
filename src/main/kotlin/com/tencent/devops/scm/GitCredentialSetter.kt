package com.tencent.devops.scm

/**
 * deng
 * 30/01/2018
 */
interface GitCredentialSetter {
    fun setGitCredential()

    fun getCredentialUrl(url: String) = url
}
