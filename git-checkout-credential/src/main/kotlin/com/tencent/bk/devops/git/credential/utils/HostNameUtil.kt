package com.tencent.bk.devops.git.credential.utils

object HostNameUtil {
    /**
     * 是否为IP地址
     */
    fun isIPAddress(input: String): Boolean {
        val pattern = Regex("^([0-9]{1,3}\\.){3}[0-9]{1,3}$")
        return pattern.matches(input)
    }
}