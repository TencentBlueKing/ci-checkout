package com.tencent.devops.utils.shell

import com.tencent.devops.enums.utils.OSType
import com.tencent.devops.pojo.utils.AgentEnv
import java.io.File

object CommonShellUtils {

    fun execute(
        script: String,
        dir: File? = null,
        runtimeVariables: Map<String, String> = mapOf(),
        failExit: Boolean = true
    ): String {
        println("[execute script]: ${SensitiveLineParser.onParseLine(script)}")
        return try {
            val isWindows = AgentEnv.getOS() == OSType.WINDOWS
            if (isWindows) BatScriptUtil.executeEnhance(script, runtimeVariables, dir)
            else ShellUtil.executeEnhance(script, runtimeVariables, dir)
        } catch (e: Exception) {
            if (failExit) throw e
            else e.message ?: ""
        }
    }
}
