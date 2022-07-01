package com.tencent.bk.devops.git.core.util

import org.slf4j.LoggerFactory

object ConsoleTableUtil {

    private val logger = LoggerFactory.getLogger(ConsoleTableUtil::class.java)

    fun printAsTable(errMsg: String, cause: String, solution: String) {
        logger.warn("************************************问题排查指引**********************************************")
        printSplit("错误信息", errMsg)
        printSplit("问题原因", cause)
        printSplit("解决办法", solution)
        logger.warn("********************************************************************************************")
    }

    private fun printSplit(title: String, str: String) {
        str.split("\n").forEachIndexed { index, message ->
            if (index == 0) {
                logger.warn("$title:${message.trim()}")
            } else {
                logger.warn("       ${message.trim()}")
            }
        }
    }
}
