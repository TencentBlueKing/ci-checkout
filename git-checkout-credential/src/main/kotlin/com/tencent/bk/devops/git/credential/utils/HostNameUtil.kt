package com.tencent.bk.devops.git.credential.utils

object HostNameUtil {
    private val CHART_CONVERT_MAP = mapOf(
        "0" to "a",
        "1" to "b",
        "2" to "c",
        "3" to "d",
        "4" to "e",
        "5" to "f",
        "6" to "g",
        "7" to "h",
        "8" to "i",
        "9" to "j",
        "." to "."
    )

    /**
     * 将IP地址转成域名类型
     */
    fun convertIpToHostName(ip: String): String {
        val builder = StringBuilder()
        ip.forEach {
            builder.append(CHART_CONVERT_MAP[it.toString()])
        }
        return builder.toString()
    }

    /**
     * 是否为IP地址
     */
    fun isIPAddress(input: String): Boolean {
        val pattern = Regex("^([0-9]{1,3}\\.){3}[0-9]{1,3}$")
        return pattern.matches(input)
    }
}