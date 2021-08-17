package com.tencent.bk.devops.git.core.service.handler

import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import org.slf4j.LoggerFactory

class GitLfsHandler(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) : IGitHandler {

    companion object {
        private val logger = LoggerFactory.getLogger(GitLfsHandler::class.java)
    }

    override fun doHandle() {
        with(settings) {
            if (!lfs) {
                return
            }
            logger.groupStart("Fetching lfs")
            git.lfsPull()
            logger.groupEnd("")
        }
    }
}
