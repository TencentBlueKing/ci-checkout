package com.tencent.bk.devops.git.core.util

import com.tencent.bk.devops.git.core.constant.GitConstants
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

object LockHelper {

    fun lock() {
        getLockFile().writeText(System.getenv(GitConstants.BK_CI_BUILD_ID) ?: "")
    }

    fun unlock(): Boolean {
        val lockFile = getLockFile()
        if (!lockFile.exists()) {
            return true
        }
        val lockValue = lockFile.readText()
        val buildId = System.getenv(GitConstants.BK_CI_BUILD_ID) ?: ""
        return if (lockValue == buildId) {
            lockFile.delete()
            true
        } else {
            logger.warn("Unlock failed, lock value: $lockValue, buildId: $buildId")
            false
        }
    }

    private fun getLockFile(): File {
        val lockPath = Paths.get(
            System.getProperty("user.home"),
            ".checkout",
            System.getenv(GitConstants.BK_CI_PIPELINE_ID),
            System.getenv(GitConstants.BK_CI_BUILD_JOB_ID)
        )
        if (!Files.exists(lockPath)) {
            Files.createDirectories(lockPath)
        }
        return File(lockPath.toString(), "credential.lock")
    }

    private val logger = LoggerFactory.getLogger(LockHelper::class.java)
}
