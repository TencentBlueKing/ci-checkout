package com.tencent.bk.devops.git.core.service.handler

import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import org.slf4j.LoggerFactory

class WoaProxyHandler(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) : IGitHandler {

    companion object {
        private const val HTTP_PROXY = "http_proxy"
        private const val HTTPS_PROXY = "https_proxy"
        private const val NO_PROXY = "no_proxy"
        private val logger = LoggerFactory.getLogger(WoaProxyHandler::class.java)
    }

    override fun doHandle() = Unit
}
