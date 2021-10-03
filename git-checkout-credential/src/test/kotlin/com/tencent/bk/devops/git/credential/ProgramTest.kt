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

import com.tencent.bk.devops.git.credential.helper.GitHelper
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

@Ignore
class ProgramTest {

    @Test
    fun innerMain() {
        GitHelper.config(
            configKey = Constants.GIT_CREDENTIAL_COMPATIBLEHOST,
            configValue = "git.example.com,git.example2.com",
            configScope = ConfigScope.GLOBAL
        )
        val storeBuilder = StringBuilder().append("protocol=https\n").append("host=git.example.com\n")
            .append("path=\n").append("username=ming2\n").append("password=derafd\n")
        val storeInputStream = ByteArrayInputStream(storeBuilder.toString().toByteArray())
        val storeOutputStream = ByteArrayOutputStream()
        val storeProgram = Program(standardIn = storeInputStream, standardOut = PrintStream(storeOutputStream))
        storeProgram.innerMain(arrayOf("devopsStore"))

        val getBuilder = StringBuilder().append("protocol=https\n").append("host=git.example.com\n").append("path=\n")
        val getInputStream = ByteArrayInputStream(getBuilder.toString().toByteArray())
        val getOutputStream = ByteArrayOutputStream()
        val getProgram = Program(standardIn = getInputStream, standardOut = PrintStream(getOutputStream))
        getProgram.innerMain(arrayOf("get"))
        val result = getOutputStream.toString("utf-8")
        Assert.assertTrue(result.contains("username=ming2"))
        Assert.assertTrue(result.contains("password=derafd"))

        val get2Builder = StringBuilder().append("protocol=https\n").append("host=git.example.com\n").append("path=\n")
        val get2InputStream = ByteArrayInputStream(get2Builder.toString().toByteArray())
        val get2OutputStream = ByteArrayOutputStream()
        val get2Program = Program(standardIn = get2InputStream, standardOut = PrintStream(get2OutputStream))
        get2Program.innerMain(arrayOf("get"))
        val result2 = getOutputStream.toString("utf-8")
        Assert.assertTrue(result2.contains("username=ming2"))
        Assert.assertTrue(result2.contains("password=derafd"))

        val eraseBuilder = StringBuilder().append("protocol=https\n").append("host=git.example.com\n").append("path=\n")
        val eraseInputStream = ByteArrayInputStream(eraseBuilder.toString().toByteArray())
        val eraseOutputStream = ByteArrayOutputStream()
        val eraseProgram = Program(standardIn = eraseInputStream, standardOut = PrintStream(eraseOutputStream))
        eraseProgram.innerMain(arrayOf("devopsErase"))
    }
}
