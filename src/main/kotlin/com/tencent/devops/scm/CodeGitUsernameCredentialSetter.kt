package com.tencent.devops.scm

import java.net.URL
import java.net.URLEncoder
import org.slf4j.LoggerFactory

class CodeGitUsernameCredentialSetter constructor(
    private val username: String,
    private val password: String
) : GitCredentialSetter {
    override fun setGitCredential() {
    }

    override fun getCredentialUrl(url: String): String {
        try {
            val u = URL(url)
            val host = if (u.host.endsWith("/")) {
                u.host.removeSuffix("/")
            } else {
                u.host
            }

            val path = if (u.path.startsWith("/")) {
                u.path.removePrefix("/")
            } else {
                u.path
            }
            return "http://${URLEncoder.encode(username, "UTF8")}:${URLEncoder.encode(password, "UTF8")}@$host/$path"
        } catch (t: Throwable) {
            logger.warn("Fail to get the username and password credential url for $url", t)
        }
        return url
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeGitUsernameCredentialSetter::class.java)
    }
}
