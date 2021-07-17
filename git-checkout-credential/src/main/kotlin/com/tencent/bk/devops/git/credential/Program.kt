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

package com.tencent.bk.devops.git.credential

import com.tencent.bk.devops.git.credential.Constants.BK_CI_BUILD_JOB_ID
import com.tencent.bk.devops.git.credential.Constants.BK_CI_PIPELINE_ID
import com.tencent.bk.devops.git.credential.Constants.GIT_CREDENTIAL_COMPATIBLEHOST
import com.tencent.bk.devops.git.credential.Constants.GIT_CREDENTIAL_HELPER
import com.tencent.bk.devops.git.credential.Constants.XDG_CONFIG_HOME
import com.tencent.bk.devops.git.credential.helper.GitHelper
import com.tencent.bk.devops.git.credential.storage.StorageProvider
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintStream
import java.nio.file.Paths
import java.util.TreeMap

class Program(
    private val standardIn: InputStream,
    private val standardOut: PrintStream
) {

    fun innerMain(args: Array<String>) {
        if (args.isEmpty() || args[0].contains("?")) {
            return
        }
        val actions = TreeMap<String, () -> Unit>()
        actions["devopsStore"] = { store() }
        actions["get"] = { get() }
        actions["fill"] = { get() }
        actions["erase"] = { erase() }
        actions["devopsErase"] = { devopsErase() }
        actions["reject"] = { erase() }

        args.forEach { arg ->
            if (actions.containsKey(arg)) {
                actions[arg]!!.invoke()
            }
        }
    }

    private fun store() {
        val credentialArguments = readInput()
        val credentialStore = StorageProvider.getCredentialStorage()
        val compatibleHost = GitHelper.tryConfigGet(
            configKey = GIT_CREDENTIAL_COMPATIBLEHOST,
            configScope = ConfigScope.GLOBAL
        )
        // 同一服务多个域名时，需要保存不同域名的凭证
        if (!compatibleHost.isNullOrBlank() && compatibleHost.contains(credentialArguments.originHost)) {
            compatibleHost.split(",").forEach { host ->
                listOf("https", "http").forEach { protocol ->
                    credentialStore.store(
                        CredentialArguments(
                            protocol = protocol,
                            originHost = host,
                            devopsHost = convertDevopsHost(host),
                            username = credentialArguments.username,
                            password = credentialArguments.password
                        ).convertInputStream()
                    )
                }
            }
        }
        credentialStore.store(credentialArguments.convertInputStream())
        install()
    }

    private fun get() {
        standardOut.print(StorageProvider.getCredentialStorage().get(readInput().convertInputStream()))
    }

    /**
     * 只清理当前host
     */
    private fun erase() {
        StorageProvider.getCredentialStorage().erase(readInput().convertInputStream())
    }

    /**
     * 清理所有适配的host
     */
    private fun devopsErase() {
        val credentialArguments = readInput()
        val credentialStore = StorageProvider.getCredentialStorage()
        credentialStore.erase(credentialArguments.convertInputStream())
        val compatibleHost = GitHelper.tryConfigGet(
            configKey = GIT_CREDENTIAL_COMPATIBLEHOST,
            configScope = ConfigScope.GLOBAL
        )
        // 同一服务多个域名时，需要保存不同域名的凭证
        if (!compatibleHost.isNullOrBlank() && compatibleHost.contains(credentialArguments.originHost)) {
            compatibleHost.split(",").forEach { host ->
                listOf("https", "http").forEach { protocol ->
                    credentialStore.erase(
                        CredentialArguments(
                            protocol = protocol,
                            originHost = host,
                            devopsHost = convertDevopsHost(host)
                        ).convertInputStream()
                    )
                }
            }
        }
    }

    private fun install() {
        val javaHome = System.getProperty("java.home")
        val javaExecutable = File(javaHome, "bin/java")
        val pathToJava = javaExecutable.absolutePath
        val pathToJar =
            File(Program::class.java.protectionDomain.codeSource.location.toURI().schemeSpecificPart).absolutePath

        configureGit(
            pathToJava = pathToJava,
            pathToJar = pathToJar
        )
    }

    private fun configureGit(pathToJava: String, pathToJar: String) {
        val xdgConfigHome = System.getenv(XDG_CONFIG_HOME) ?: return
        val xdgConfigPath = Paths.get(xdgConfigHome, "git", "config").normalize().toString()

        GitHelper.configFileAdd(
            configKey = GIT_CREDENTIAL_HELPER,
            configValue = "!'$pathToJava' -jar '$pathToJar'",
            filePath = xdgConfigPath
        )
    }

    private fun readInput(): CredentialArguments {
        val reader = BufferedReader(BufferedReader(InputStreamReader(standardIn)))
        var protocol: String? = null
        var host: String? = null
        var path: String? = null
        var username: String? = null
        var password: String? = null
        reader.useLines { lines ->
            for (line in lines) {
                if (line.isEmpty()) {
                    break
                }
                val pair = line.split("=", limit = 2)
                if (pair.size == 2) {
                    when (pair[0]) {
                        "protocol" -> protocol = pair[1]
                        "host" -> host = pair[1]
                        "path" -> path = pair[1]
                        "username" -> username = pair[1]
                        "password" -> password = pair[1]
                    }
                }
            }
        }
        if (protocol.isNullOrBlank()) {
            throw IllegalArgumentException("the protocol can't be empty")
        }
        if (host.isNullOrBlank()) {
            throw IllegalArgumentException("the protocol can't be empty")
        }
        return CredentialArguments(
            protocol = protocol!!,
            originHost = host!!,
            devopsHost = convertDevopsHost(host!!),
            path = path,
            username = username,
            password = password
        )
    }

    private fun convertDevopsHost(host: String): String {
        val buildId = System.getenv(BK_CI_PIPELINE_ID)
        val vmSeqId = System.getenv(BK_CI_BUILD_JOB_ID)
        val builder = StringBuilder()
        if (!buildId.isNullOrBlank()) {
            builder.append(buildId).append(".")
        }
        if (!vmSeqId.isNullOrBlank()) {
            builder.append(vmSeqId).append(".")
        }
        builder.append(host)
        return builder.toString()
    }
}
