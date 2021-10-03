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
import com.tencent.bk.devops.git.credential.helper.Trace
import java.io.ByteArrayInputStream
import java.net.URI

data class CredentialArguments(
    val protocol: String,
    val host: String,
    val path: String? = null,
    val username: String? = null,
    val password: String? = null
) {
    val targetUri get() = URI("$protocol://$host/")

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("protocol=").append(protocol).append("\n")
        builder.append("host=").append(host).append("\n")
        if (path != null) {
            builder.append("path=").append(path).append("\n")
        }
        if (username != null) {
            builder.append("username=").append(username).append("\n")
        }
        if (password != null) {
            builder.append("password=").append(password).append("\n")
        }
        Trace.writeLine("protocol:$protocol")
        Trace.writeLine("host:$host")
        Trace.writeLine("path:$path")
        return builder.toString()
    }

    fun copyArgs(protocol: String, host: String): CredentialArguments {
        return CredentialArguments(
            protocol = protocol,
            host = host,
            path = path,
            username = username,
            password = password
        )
    }

    fun setCredentials(credential: Credential): CredentialArguments {
        return CredentialArguments(
            protocol = protocol,
            host = host,
            path = path,
            username = credential.Username,
            password = credential.Password
        )
    }
}
