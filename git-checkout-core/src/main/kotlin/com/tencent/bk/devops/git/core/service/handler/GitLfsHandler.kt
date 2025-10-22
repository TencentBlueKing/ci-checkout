package com.tencent.bk.devops.git.core.service.handler

import com.tencent.bk.devops.git.core.constant.ContextConstants
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.service.helper.GitSparseCheckoutHelper
import com.tencent.bk.devops.git.core.util.EnvHelper
import org.slf4j.LoggerFactory

class GitLfsHandler(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) : IGitHandler {
    private val sparseCheckoutHelper = GitSparseCheckoutHelper(settings, git)

    companion object {
        private val logger = LoggerFactory.getLogger(GitLfsHandler::class.java)
    }

    @SuppressWarnings("NestedBlockDepth")
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
                val (fetchInclude, fetchExclude) = when {
                    sparseCheckoutHelper.useConeMode() -> {
                        // 如果使用cone模式，且拉取路径为空，则需指定拉取路径，否则容易将其他下级目录的空文件拉下来
                        if (includeSubPath.isNullOrBlank()) {
                            "/?"
                        } else {
                            includeSubPath
                        } to ""
                    }

                    else -> {
                        includeSubPath to excludeSubPath
                    }
                }
                git.lfsPull(
                    fetchInclude = fetchInclude,
                    fetchExclude = fetchExclude
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
