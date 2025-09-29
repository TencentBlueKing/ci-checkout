package com.tencent.bk.devops.git.core.service.handler

import com.tencent.bk.devops.git.core.constant.ContextConstants
import com.tencent.bk.devops.git.core.enums.OSType
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.util.AgentEnv
import com.tencent.bk.devops.git.core.util.EnvHelper
import org.slf4j.LoggerFactory
import java.io.File

/**
 * 工蜂域名切换,第三方镜像或者第三方构建机可能配置了proxy，但是no_proxy没有加git.woa.com，导致报403错误
 */
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

    override fun doHandle() {
        woaProxy()
        woaSshConfig()
    }

    private fun woaProxy() {
        if (!settings.repositoryUrl.contains("git.woa.com")) {
            return
        }
        val httpProxy = System.getenv(HTTP_PROXY)
        val httpsProxy = System.getenv(HTTPS_PROXY)
        val noProxy = System.getenv(NO_PROXY)
        val woaNoProxy =
            (!httpProxy.isNullOrBlank() || !httpsProxy.isNullOrBlank()) &&
                !noProxy.isNullOrBlank() &&
                !noProxy.split(",").any { it == ".woa.com" || it == "git.woa.com" }
        if (woaNoProxy) {
            EnvHelper.putContext(ContextConstants.CONTEXT_WOA_PROXY, "1")
            logger.warn("add 'git.woa.com' to no_proxy")
            git.setEnvironmentVariable(NO_PROXY, "$noProxy,git.woa.com")
        }
    }

    private fun woaSshConfig() {
        if (!settings.repositoryUrl.contains("git@git.woa.com")) {
            return
        }
        // 如果是docker容器，镜像中没有配置git.woa.com,则默认配置
        if (!AgentEnv.isThirdParty() && AgentEnv.getOS() == OSType.LINUX) {
            val sshConfigFile = File(System.getProperty("user.home"), ".ssh/config")
            if (sshConfigFile.exists()) {
                val configContent = sshConfigFile.readText()
                if (configContent.contains("git.code.oa.com") && !configContent.contains("git.woa.com")) {
                    EnvHelper.putContext(ContextConstants.CONTEXT_WOA_PROXY, "1")
                    logger.warn("add 'git.woa.com' host to ${sshConfigFile.absolutePath}")
                    // 有的镜像最后没有换行符,需补充
                    sshConfigFile.appendText("\n")
                    sshConfigFile.appendText("Host git.woa.com\n")
                    sshConfigFile.appendText("StrictHostKeyChecking no\n")
                    sshConfigFile.appendText("Port 22\n")
                }
            }
        }
    }
}
