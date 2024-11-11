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
import com.tencent.bk.devops.git.credential.Constants.CREDENTIAL_COMPATIBLE_HOST
import com.tencent.bk.devops.git.credential.helper.LockHelper
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
    private var taskId: String? = null

    private fun getTaskUri(targetUri: URI, taskId: String? = this.taskId): URI {
        return with(targetUri) {
            URI("$scheme://$taskId.$host")
        }
    }

    fun innerMain(args: Array<String>) {
        if (args.isEmpty() || args[0].contains("?")) {
            return
        }
        val actions = TreeMap<String, () -> Unit>()
        actions["devopsStore"] = { store() }
        actions["get"] = { get() }
        actions["fill"] = { get() }
        actions["devopsErase"] = { devopsErase() }

        if (args.size >= 2) {
            taskId = args[0]
        }

        args.forEach { arg ->
            if (actions.containsKey(arg)) {
                actions[arg]!!.invoke()
            }
        }
    }

    private fun store() {
        val credentialArguments = readInput()

        with(credentialArguments) {
            // 仅主库写入此凭证，fork库不写入，避免覆盖主库凭证
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
            // 保存插件的凭证,为了解决当出现`拉代码1->拉代码2-bash:git push 代码1`,
            // 如果拉仓库2的身份没有仓库1的权限，那么bash就会报错,因为凭证会被拉代码2插件给覆盖
            if (!taskId.isNullOrBlank()) {
                // 同时保存http/https两种凭证
                compatibleTask(getTaskUri(targetUri)) {
                    credentialStore.add(
                        it,
                        Credential(username, password)
                    )
                }
            }
            // 保存fork库凭证
            if (!forkUsername.isNullOrBlank() && !forkPassword.isNullOrBlank()) {
                credentialStore.add(
                    getTaskUri(forkTargetUri, "$taskId-fork"),
                    Credential(forkUsername, forkPassword)
                )
            }
        }
        // 凭证写入成功后,把buildId写入到锁文件，防止在第三方构建机同一条流水线同时运行时,前面执行的构建把后面执行的构建凭证删除
        LockHelper.lock()
    }

    @SuppressWarnings("NestedBlockDepth")
    private fun CredentialArguments.compatible(action: (URI) -> Unit) {
        val compatibleHost = System.getenv(CREDENTIAL_COMPATIBLE_HOST)
        // 同一服务多个域名时，需要保存不同域名的凭证
        if (!compatibleHost.isNullOrBlank() && compatibleHost.contains(host)) {
            compatibleHost.split(",").forEach host@{ cHost ->
                listOf("https", "http")
                    .forEach protocol@{ cProtocol ->
                        if (cProtocol == protocol && cHost == host) return@protocol
                        action.invoke(URI("$cProtocol://$cHost/"))
                    }
            }
        }
    }

    private fun get() {
        with(readInput()) {
            var credential: Credential? = null
            if (!taskId.isNullOrBlank()) {
                credential = credentialStore.get(getTaskUri(targetUri))
            }
            if (credential == null || credential == Credential.Empty) {
                credential = credentialStore.get(targetUri)
            }
            if (credential == null || credential == Credential.Empty) {
                return
            }
            standardOut.print(setCredentials(credential))
        }
    }

    /**
     * 清理所有适配的host
     */
    private fun devopsErase() {
        // 如果当前凭证不是此构建Id写入的,就不删除
        if (!LockHelper.unlock()) {
            return
        }
        val credentialArguments = readInput()
        with(credentialArguments) {
            credentialStore.delete(targetUri)
            compatible { compatibleUri ->
                credentialStore.delete(compatibleUri)
            }
            if (!taskId.isNullOrBlank()) {
                // 卸载主库凭证
                compatibleTask(getTaskUri(targetUri)) {
                    credentialStore.delete(it)
                }
                // 存在fork库凭证，卸载fork库凭证
                if (!forkProtocol.isNullOrBlank() && !forkHost.isNullOrBlank()) {
                    credentialStore.delete(getTaskUri(forkTargetUri, "$taskId-fork"))
                }
            }
        }
    }

    @SuppressWarnings("ComplexMethod")
    private fun readInput(): CredentialArguments {
        val reader = BufferedReader(BufferedReader(InputStreamReader(standardIn)))
        var protocol: String? = null
        var host: String? = null
        var path: String? = null
        var username: String? = null
        var password: String? = null
        // fork库相关凭证信息
        var forkProtocol: String? = null
        var forkHost: String? = null
        var forkUsername: String? = null
        var forkPassword: String? = null
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
                        "forkProtocol" -> forkProtocol = pair[1]
                        "forkHost" -> forkHost = pair[1]
                        "forkUsername" -> forkUsername = pair[1]
                        "forkPassword" -> forkPassword = pair[1]
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
            password = password,
            forkProtocol = forkProtocol,
            forkHost = forkHost,
            forkUsername = forkUsername,
            forkPassword = forkPassword
        )
    }

    //  服务端可能会有302跳转，将http转换成https，所以task也应该兼容https和http
    private fun compatibleTask(taskUri: URI, action: (URI) -> Unit) {
        listOf("https", "http")
            .forEach protocol@{ cProtocol ->
                action.invoke(URI("$cProtocol://${taskUri.host}/"))
            }
    }
}
