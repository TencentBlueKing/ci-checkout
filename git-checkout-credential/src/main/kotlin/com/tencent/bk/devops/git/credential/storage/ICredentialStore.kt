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

package com.tencent.bk.devops.git.credential.storage

import com.tencent.bk.devops.git.credential.helper.GitHelper
import com.tencent.bk.devops.git.credential.helper.GitOutput
import java.io.InputStream

interface ICredentialStore {
    /**
     * 获取凭证
     */
    @SuppressWarnings("ReturnCount")
    fun get(input: InputStream): String? {
        val credentialHelper = getCredentialHelper() ?: return null
        val stdOuts = GitHelper.invokeHelper(
            args = listOf("credential-$credentialHelper", "get"),
            inputStream = input
        ).stdOuts
        if (stdOuts.isEmpty()) {
            // 返回空的账号密码
            return "username=\"\"\npassword=\"\"\n"
        }
        return stdOuts.joinToString("\n")
    }

    /**
     * 删除凭证
     */
    fun erase(input: InputStream): Boolean {
        val credentialHelper = getCredentialHelper() ?: return false
        return GitHelper.invokeHelper(
            args = listOf("credential-$credentialHelper", "erase"),
            inputStream = input
        ).exitCode == 0
    }

    /**
     * 存储凭证
     */
    fun store(input: InputStream): Boolean {
        val credentialHelper = getCredentialHelper() ?: return false
        return invokeHelper(
            credentialHelper = credentialHelper,
            action = "store",
            input = input
        ).exitCode == 0
    }

    /**
     * 系统是否支持此类型的凭证
     */
    fun isSupport(): Boolean {
        return !getCredentialHelper().isNullOrBlank()
    }

    fun getCredentialHelper(): String?

    fun getCredentialOptions(): List<String> = emptyList()

    private fun invokeHelper(
        credentialHelper: String,
        action: String,
        input: InputStream
    ): GitOutput {
        val args = mutableListOf("credential-$credentialHelper")
        val options = getCredentialOptions()
        if (options.isNotEmpty()) {
            args.addAll(options)
        }
        args.add(action)
        return GitHelper.invokeHelper(
            args = args,
            inputStream = input
        )
    }
}
