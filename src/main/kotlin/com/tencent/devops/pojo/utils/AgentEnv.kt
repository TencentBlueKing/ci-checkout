package com.tencent.devops.pojo.utils

import com.tencent.devops.enums.utils.OSType
import java.util.Locale
import org.slf4j.LoggerFactory

object AgentEnv {

    private val logger = LoggerFactory.getLogger(AgentEnv::class.java)

    private var os: OSType? = null

    fun getOS(): OSType {
        if (os == null) {
            synchronized(this) {
                if (os == null) {
                    val OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH)
                    logger.info("Get the os name - ($OS)")
                    os = if (OS.indexOf(string = "mac") >= 0 || OS.indexOf("darwin") >= 0) {
                        OSType.MAC_OS
                    } else if (OS.indexOf("win") >= 0) {
                        OSType.WINDOWS
                    } else if (OS.indexOf("nux") >= 0) {
                        OSType.LINUX
                    } else {
                        OSType.OTHER
                    }
                }
            }
        }
        return os!!
    }
}
