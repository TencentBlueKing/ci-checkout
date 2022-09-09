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

package com.tencent.bk.devops.git.core.util

import org.junit.Assert
import org.junit.Test

class SensitiveLineParserTest {

    @Test
    fun onParseLine() {
        var line = "http://abc:123456@git.example.com/example.git"
        Assert.assertEquals(
            "http://abc:***@git.example.com/example.git",
            SensitiveLineParser.onParseLine(line)
        )
        line = "git config --global --add url.http://abc:123456@git.example.com/.insteadOf git@git.example.com:"
        Assert.assertEquals(
            "git config --global --add url.http://abc:***@git.example.com/.insteadOf git@git.example.com:",
            SensitiveLineParser.onParseLine(line)
        )
        line = "git config --global --add url.https://abc:123456@git.example.com/.insteadOf http://git.example.com/"
        Assert.assertEquals(
            "git config --global --add url.https://abc:***@git.example.com/.insteadOf http://git.example.com/",
            SensitiveLineParser.onParseLine(line)
        )
        line = "url.https://oauth2:123456@git.example.com/.insteadOf http://git.example.com/"
        Assert.assertEquals(
            "url.https://oauth2:***@git.example.com/.insteadOf http://git.example.com/",
            SensitiveLineParser.onParseLine(line)
        )
        line = "http://git.example.com/?password=12345"
        Assert.assertEquals(
            "http://git.example.com/?password=***",
            SensitiveLineParser.onParseLine(line)
        )
        line = "git submodule foreach --recursive ' " +
            "git config --unset-all url.https://git.example.com/.insteadOf ;  " +
            "git config --add url.https://git.example.com/.insteadOf git@git.example.com: ; " +
            "git config --add url.https://git.example.com/.insteadOf git@git.example.com: ; " +
            "git config --add url.https://git.example.com/.insteadOf git@test.git.example.com:  || true'"
        Assert.assertEquals(
            line,
            SensitiveLineParser.onParseLine(line)
        )
        line = "git submodule foreach --recursive " +
            "git config core.insteadOfKey " +
            "url.https://oauth2:123456@git.example.com/.insteadOf ; " +
            "git config --add " +
            "url.https://oauth2:123456@git.example.com/.insteadOf git@git.example.com:"
        Assert.assertEquals(
            "git submodule foreach --recursive " +
                "git config core.insteadOfKey url.https://oauth2:***@git.example.com/.insteadOf ; " +
                "git config --add url.https://oauth2:***@git.example.com/.insteadOf git@git.example.com:",
            SensitiveLineParser.onParseLine(line)
        )
    }
}
