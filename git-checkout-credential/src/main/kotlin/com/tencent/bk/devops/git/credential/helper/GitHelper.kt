/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.devops.git.credential.helper

import com.tencent.bk.devops.git.credential.ConfigScope
import com.tencent.bk.devops.git.credential.Constants.GIT_REPO_PATH
import java.io.File
import java.io.InputStream
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.LogOutputStream
import org.apache.commons.exec.PumpStreamHandler

object GitHelper {

    fun tryConfigGet(
        configKey: String,
        configValueRegex: String? = null,
        configScope: ConfigScope? = ConfigScope.LOCAL
    ): String? {
        val args = mutableListOf("config")
        if (configScope != null) {
            args.add(configScope.option)
        }
        args.add("--get")
        args.add(configKey)
        if (!configValueRegex.isNullOrBlank()) {
            args.add(configValueRegex)
        }
        val output = execGit(
            args = args,
            allowAllExitCodes = true
        )
        if (output.exitCode != 0) {
            return null
        }
        return output.stdOut
    }

    fun config(
        configKey: String,
        configValue: String,
        configScope: ConfigScope = ConfigScope.LOCAL
    ) {
        execGit(listOf("config", configScope.option, configKey, configValue))
    }

    fun configFileAdd(
        configKey: String,
        configValue: String,
        filePath: String,
        add: Boolean = false
    ) {
        val args = mutableListOf("config", "-f", filePath)
        if (add) {
            args.add("--add")
        }
        args.addAll(listOf(configKey, configValue))
        execGit(args = args)
    }

    fun configFileGet(
        configKey: String,
        configValueRegex: String? = null,
        filePath: String
    ): String? {
        val args = mutableListOf("config", "-f", filePath, "--get", configKey)
        if (!configValueRegex.isNullOrBlank()) {
            args.add(configValueRegex)
        }
        val output = execGit(
            args = args,
            allowAllExitCodes = true
        )
        if (output.exitCode != 0) {
            return null
        }
        return output.stdOut
    }

    fun invokeHelper(args: List<String>, inputStream: InputStream): GitOutput {
        return execGit(
            args = args,
            allowAllExitCodes = true,
            inputStream = inputStream
        )
    }

    private fun execGit(
        args: List<String>,
        allowAllExitCodes: Boolean = false,
        inputStream: InputStream? = null
    ): GitOutput {
        val executor = DefaultExecutor()
        val stdOuts = mutableListOf<String>()
        val errOuts = StringBuilder()
        val outputStream = object : LogOutputStream() {
            override fun processLine(line: String?, level: Int) {
                if (line == null) {
                    return
                }
                stdOuts.add(line)
                Trace.writeLine(line)
            }
        }

        val errorStream = object : LogOutputStream() {
            override fun processLine(line: String?, level: Int) {
                if (line == null) {
                    return
                }
                System.err.println(line)
                errOuts.append(line)
            }
        }

        executor.workingDirectory = File(System.getenv(GIT_REPO_PATH) ?: ".")
        executor.streamHandler = PumpStreamHandler(outputStream, errorStream, inputStream)

        if (allowAllExitCodes) {
            executor.setExitValues(null)
        }
        val command = CommandLine.parse("git").addArguments(args.toTypedArray(), false)
        Trace.writeLine(command.toStrings().joinToString(" "))
        try {
            val exitCode = executor.execute(command)
            return GitOutput(exitCode = exitCode, stdOuts = stdOuts)
        } finally {
            IOHelper.closeQuietly(outputStream, errorStream, inputStream)
        }
    }
}
