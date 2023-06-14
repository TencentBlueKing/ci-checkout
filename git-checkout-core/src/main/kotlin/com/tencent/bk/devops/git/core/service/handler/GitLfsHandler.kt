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
                git.lfsVersion()
                if (lfsConcurrentTransfers != null && lfsConcurrentTransfers > 0) {
                    git.config(configKey = "lfs.concurrenttransfers", configValue = lfsConcurrentTransfers.toString())
                }
                if (settings.enableGitLfsClean == true) {
                    git.tryCleanLfs()
                }
                git.lfsPull(
                    fetchInclude = includeSubPath,
                    fetchExclude = excludeSubPath
                )
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
