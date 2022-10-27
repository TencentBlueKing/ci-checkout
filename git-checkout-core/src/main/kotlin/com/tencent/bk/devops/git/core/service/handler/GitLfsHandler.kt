package com.tencent.bk.devops.git.core.service.handler

import com.tencent.bk.devops.git.core.constant.ContextConstants
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.util.EnvHelper
import org.slf4j.LoggerFactory

class GitLfsHandler(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) : IGitHandler {

    companion object {
        private val logger = LoggerFactory.getLogger(GitLfsHandler::class.java)
    }

    override fun doHandle() {
        val startEpoch = System.currentTimeMillis()
        try {
            with(settings) {
                if (!lfs) {
                    return
                }
                logger.groupStart("Fetching lfs")
                if (lfsConcurrentTransfers != null && lfsConcurrentTransfers > 0) {
                    git.config(configKey = "lfs.concurrenttransfers", configValue = lfsConcurrentTransfers.toString())
                }
                // lfs过滤规则跟sparse checkout不一样,不能直接用sparse checkout配置的路径
                git.lfsPull()
                logger.groupEnd("")
            }
        } finally {
            EnvHelper.putContext(
                key = ContextConstants.CONTEXT_LFS_COST_TIME,
                value = (System.currentTimeMillis() - startEpoch).toString()
            )
        }
    }
}
