package com.tencent.bk.devops.git.core.util

import org.slf4j.LoggerFactory

object ConsoleTableUtil {

    private val logger = LoggerFactory.getLogger(ConsoleTableUtil::class.java)
    private const val format = "%s%-4s\t%s%-100s"

    fun printAsTable(errMsg: String, cause: String, solution: String) {
        logger.warn("************************************问题排查指引**********************************************")
        printSplit("错误信息", errMsg)
        printSplit("问题原因", cause)
        printSplit("解决办法", solution)
    }

    private fun printSplit(title: String, str: String) {
        str.split("\n").forEachIndexed { index, message ->
            if (index == 0) {
                logger.warn(String.format(format, "|", title, "|", message.trim()))
            } else {
                logger.warn(String.format(format, "|", "    ", "|", message.trim()))
            }
        }
    }
}
