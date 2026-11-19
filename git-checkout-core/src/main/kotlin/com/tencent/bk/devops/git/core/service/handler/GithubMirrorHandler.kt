package com.tencent.bk.devops.git.core.service.handler

import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_PROJECT_NAME
import com.tencent.bk.devops.git.core.constant.GitConstants.GITHUB_MIRROR_SITE
import com.tencent.bk.devops.git.core.constant.GitConstants.GITHUB_MIRROR_WHITE_PROJECT
import com.tencent.bk.devops.git.core.constant.GitConstants.GITHUB_PUSH_INSTEADOF_KEY
import com.tencent.bk.devops.git.core.constant.GitConstants.GITHUB_URL
import com.tencent.bk.devops.git.core.constant.GitConstants.GIT_GITHUB_MIRROR_INSTEADOF_KEY
import com.tencent.bk.devops.git.core.enums.GitConfigScope
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.service.helper.GitCleanUpHelper
import com.tencent.bk.devops.git.core.util.AgentEnv
import com.tencent.bk.devops.git.core.util.GitUtil
import org.slf4j.LoggerFactory

/**
 * github拉取走镜像站点,通过全局insteadOf改写https://github.com/,同时用pushInsteadOf保证push仍然推送到github
 *
 * 配置在整个job内生效,进入时先清理再添加,post-action阶段由GitCleanUpHelper卸载
 *
 * 以下场景不代理:
 * 1. 第三方构建机
 * 2. 构建机上已经配置了github的insteadOf
 * 3. 不在白名单中的蓝盾项目
 * 4. 主仓库是ssh协议的github仓库
 */
class GithubMirrorHandler(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) : IGitHandler {

    companion object {
        private const val GITHUB_HOST = "github.com"
        private val logger = LoggerFactory.getLogger(GithubMirrorHandler::class.java)
    }

    override fun doHandle() {
        try {
            val mirrorSite = settings.sensitiveConfInfo[GITHUB_MIRROR_SITE]?.trim()
            if (!mirrorSite.isNullOrBlank() && isSupport()) {
                logger.groupStart("Setting up github mirror")
                try {
                    configGithubMirror(mirrorUrl = if (mirrorSite.endsWith("/")) mirrorSite else "$mirrorSite/")
                } finally {
                    logger.groupEnd("")
                }
            }
        } catch (ignore: Throwable) {
            logger.warn("Failed to config github mirror: ${ignore.message}")
        }
    }

    private fun isSupport(): Boolean {
        val serverInfo = GitUtil.getServerInfo(settings.repositoryUrl)
        val projectId = System.getenv(BK_CI_PROJECT_NAME)
        val whiteProjects = settings.sensitiveConfInfo[GITHUB_MIRROR_WHITE_PROJECT]?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
        return when {
            AgentEnv.isThirdParty() || AgentEnv.isThirdDocker() -> false
            // ssh的github主仓库,授权时会把https://github.com/改写成ssh,与镜像的insteadOf冲突
            !serverInfo.httpProtocol && serverInfo.hostName == GITHUB_HOST -> false
            projectId.isNullOrBlank() || whiteProjects.isNullOrEmpty() -> false
            else -> whiteProjects.contains(projectId)
        }
    }

    private fun configGithubMirror(mirrorUrl: String) {
        val insteadOfKey = "url.$mirrorUrl.insteadOf"
        // 先清理同一个job中其他拉代码插件或前序构建配置的镜像,避免被误判为用户自己配置的insteadOf
        if (git.configExists(configKey = insteadOfKey, configScope = GitConfigScope.GLOBAL)) {
            GitCleanUpHelper(settings, git).unsetGithubMirrorConfig(insteadOfKey)
        }
        if (existGithubInsteadOf()) {
            logger.info("github insteadOf already configured on build machine, skip github mirror")
        } else {
            logger.info("Using github mirror $mirrorUrl")
            git.configAdd(
                configKey = insteadOfKey,
                configValue = GITHUB_URL,
                configScope = GitConfigScope.GLOBAL
            )
            git.configAdd(
                configKey = GITHUB_PUSH_INSTEADOF_KEY,
                configValue = GITHUB_URL,
                configScope = GitConfigScope.GLOBAL
            )
            git.config(configKey = GIT_GITHUB_MIRROR_INSTEADOF_KEY, configValue = insteadOfKey)
        }
    }

    /**
     * 是否存在改写github的insteadOf
     *
     * 不指定scope,读取system、global、xdg、local所有生效的配置
     */
    private fun existGithubInsteadOf(): Boolean {
        return git.tryConfigGetRegexp(
            configKeyRegex = "^url\\..*\\.insteadof$",
            configValueRegex = "^https?://github\\.com"
        ).isNotEmpty()
    }
}
