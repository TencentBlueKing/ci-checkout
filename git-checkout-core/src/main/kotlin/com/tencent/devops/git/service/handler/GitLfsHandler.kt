package com.tencent.devops.git.service.handler

import com.tencent.devops.git.pojo.GitSourceSettings
import com.tencent.devops.git.service.GitCommandManager
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
            git.lfsFetch()
            git.lfsCheckout()
            logger.groupEnd("")
        }
    }
}
