package com.tencent.bk.devops.git.core.util

import org.slf4j.LoggerFactory

object LogUtil {

    private val logger = LoggerFactory.getLogger(LogUtil::class.java)

    fun printException(errMsg: String, reason: String, solution: String, wiki: String) {
        logger.warn("************************************问题排查指引**********************************************")
        if (wiki.isNotBlank()) {
            printSplit("【错误信息】", "<a target='_blank' href='$wiki'>$errMsg</a>")
        } else {
            printSplit("【错误信息】", errMsg)
        }
        printSplit("【问题原因】", reason)
        printSplit("【解决办法】", solution)
        logger.warn("详情参考>> <a target='_blank' href='$wiki'>$errMsg</a>")
        logger.warn("********************************************************************************************")
    }

    private fun printSplit(title: String, str: String) {
        str.split("\n").forEachIndexed { index, message ->
            if (index == 0) {
                logger.warn("$title：${message.trim()}")
            } else {
                logger.warn("           ${message.trim()}")
            }
        }
    }
}
