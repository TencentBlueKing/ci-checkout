package com.tencent.devops.utils.shell

import com.tencent.devops.enums.utils.OSType
import com.tencent.devops.exception.GitExecuteException
import com.tencent.devops.pojo.utils.AgentEnv
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.ExecuteException
import org.apache.commons.exec.LogOutputStream
import org.apache.commons.exec.PumpStreamHandler
import org.slf4j.LoggerFactory
import java.io.File

object CommandLineUtils {

    private val logger = LoggerFactory.getLogger(CommandLineUtils::class.java)

    private val specialChars = if (AgentEnv.getOS() == OSType.WINDOWS) {
        listOf('(', ')', '[', ']', '{', '}', '^', ';', '!', ',', '`', '~', '\'', '"')
    } else {
        listOf('|', ';', '&', '$', '>', '<', '`', '!', '\\', '"', '*', '?', '[', ']', '(', ')', '\'')
    }

    fun execute(command: String, workspace: File?, print2Logger: Boolean, prefix: String = "", printException: Boolean = false): String {

        val result = StringBuffer()
        val errorResult = StringBuilder()

        val cmdLine = CommandLine.parse(command)
        val executor = CommandLineExecutor()
        if (workspace != null) {
            executor.workingDirectory = workspace
        }

        val outputStream = object : LogOutputStream() {
            override fun processLine(line: String?, level: Int) {
                if (line == null)
                    return

                val tmpLine = SensitiveLineParser.onParseLine(prefix + line)
                if (print2Logger) {
                    println(tmpLine)
                }
                result.append(tmpLine).append("\n")
                errorResult.append(tmpLine).append("|")
            }
        }

        val errorStream = object : LogOutputStream() {
            override fun processLine(line: String?, level: Int) {
                if (line == null) {
                    return
                }

                val tmpLine = SensitiveLineParser.onParseLine(prefix + line)
                if (print2Logger) {
                    System.err.println(tmpLine)
                }
                result.append(tmpLine).append("\n")
                errorResult.append(tmpLine).append("|")
            }
        }
        executor.streamHandler = PumpStreamHandler(outputStream, errorStream)
        try {
            val exitCode = executor.execute(cmdLine)
            if (exitCode != 0) {
                throw RuntimeException("$prefix Script command execution failed with exit code($exitCode)")
            }
        } catch (t: ExecuteException) {
            if (printException) logger.warn("Fail to execute the command($command)", t)
            if (errorResult.isNotEmpty()) {
                val errorMessage = if (errorResult.length >= 256) {
                    errorResult.substring(0, 256)
                } else {
                    errorResult.toString()
                }
                throw GitExecuteException(errorMessage)
            }
            throw t
        } catch (t: Throwable) {
            if (printException) logger.warn("Fail to execute the command($command)", t)
            throw t
        }
        return result.toString()
    }

    fun solveSpecialChar(str: String): String {
        val solveStr = StringBuilder()
        val isWindows = AgentEnv.getOS() == OSType.WINDOWS
        val encodeChar = if (isWindows) '^' else '\\'
        val charArr = str.toCharArray()
        charArr.forEach { ch ->
            if (ch in specialChars) {
                solveStr.append(encodeChar)
            }

            // windows的%还要特殊处理下
            if (isWindows && ch == '%') {
                solveStr.append('%')
            }

            solveStr.append(ch)
        }

        return solveStr.toString()
    }
}
