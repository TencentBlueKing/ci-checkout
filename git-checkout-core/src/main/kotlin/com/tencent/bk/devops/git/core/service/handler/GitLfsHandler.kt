package com.tencent.bk.devops.git.core.service.handler

import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.util.EnvHelper
import org.slf4j.LoggerFactory
import java.io.File

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
                if (!lfs || !File(repositoryPath, ".gitattributes").exists()) {
                    return
                }
                logger.groupStart("Fetching lfs")
                git.lfsPull(
                    fetchInclude = includeSubPath,
                    fetchExclude = excludeSubPath
                )
                logger.groupEnd("")
            }
        } finally {
            EnvHelper.putContext(
                key = GitConstants.CONTEXT_LFS_COST_TIME,
                value = (System.currentTimeMillis() - startEpoch).toString()
            )
        }
    }
}
