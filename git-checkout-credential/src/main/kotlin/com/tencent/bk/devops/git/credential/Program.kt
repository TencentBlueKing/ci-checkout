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

import com.microsoft.alm.secret.Credential
import com.tencent.bk.devops.git.credential.Constants.GIT_CREDENTIAL_COMPATIBLEHOST
import com.tencent.bk.devops.git.credential.helper.GitHelper
import com.tencent.bk.devops.git.credential.storage.CredentialStore
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintStream
import java.net.URI
import java.util.TreeMap

class Program(
    private val standardIn: InputStream,
    private val standardOut: PrintStream
) {

    private val credentialStore = CredentialStore()

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

        with(credentialArguments) {
            credentialStore.add(
                targetUri,
                Credential(username, password)
            )
            compatible { compatibleUri ->
                credentialStore.add(
                    compatibleUri,
                    Credential(username, password)
                )
            }
        }
    }

    private fun CredentialArguments.compatible(action: (URI) -> Unit) {
        val compatibleHost = GitHelper.tryConfigGet(
            configKey = GIT_CREDENTIAL_COMPATIBLEHOST,
            configScope = ConfigScope.GLOBAL
        )
        // 同一服务多个域名时，需要保存不同域名的凭证
        if (!compatibleHost.isNullOrBlank() && compatibleHost.contains(host)) {
            compatibleHost.split(",").forEach { host ->
                listOf("https", "http").forEach { protocol ->
                    action.invoke(URI("$protocol://$host/"))
                }
            }
        }
    }

    private fun get() {
        with(readInput()) {
            var credential = credentialStore.get(targetUri)
            if (credential == null) {
                credential = Credential.Empty
            }
            standardOut.print(setCredentials(credential!!))
        }
    }

    /**
     * 只清理当前host
     */
    private fun erase() {
        with(readInput()) {
            credentialStore.delete(targetUri)
        }
    }

    /**
     * 清理所有适配的host
     */
    private fun devopsErase() {
        val credentialArguments = readInput()
        with(credentialArguments) {
            credentialStore.delete(targetUri)
            compatible { compatibleUri ->
                credentialStore.delete(compatibleUri)
            }
        }
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
            host = host!!,
            path = path,
            username = username,
            password = password
        )
    }
}
