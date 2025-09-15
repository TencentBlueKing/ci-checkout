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

import com.tencent.bk.devops.git.core.pojo.CommitLogInfo
import org.junit.Assert
import org.junit.Test

class RegexUtilTest {

    @Test
    fun parseLog() {
        var expected = CommitLogInfo(
            commitId = "1fe7bc8b1350d22c3a26281a438edc3edb2bb170",
            committerName = "mingshewhe",
            commitTime = 1619403958,
            authorName = "mingshewhe",
            commitMessage = "mr 2"
        )
        Assert.assertEquals(
            expected,
            RegexUtil.parseLog(
                "1fe7bc8b1350d22c3a26281a438edc3edb2bb170" +
                    "|mingshewhe" +
                    "|1619403958" +
                    "|2021-04-26 02:25:58" +
                    "|mingshewhe" +
                    "|mr 2"
            )
        )

        expected = CommitLogInfo(
            commitId = "1fe7bc8b1350d22c3a26281a438edc3edb2bb170",
            committerName = "mingshewhe(小明)",
            commitTime = 1619403958,
            authorName = "mingshewhe(小明)",
            commitMessage = "mr 2"
        )
        Assert.assertEquals(
            expected,
            RegexUtil.parseLog(
                "1fe7bc8b1350d22c3a26281a438edc3edb2bb170" +
                    "|mingshewhe(小明)" +
                    "|1619403958" +
                    "|2021-04-26 02:25:58" +
                    "|mingshewhe(小明)" +
                    "|mr 2"
            )
        )

        expected = CommitLogInfo(
            commitId = "1fe7bc8b1350d22c3a26281a438edc3edb2bb170",
            committerName = "mingshewhe(小明)",
            commitTime = 1619403958,
            authorName = "mingshewhe(小明)",
            commitMessage = "mr 2|\"--测试|\""
        )
        Assert.assertEquals(
            expected,
            RegexUtil.parseLog(
                "1fe7bc8b1350d22c3a26281a438edc3edb2bb170" +
                    "|mingshewhe(小明)" +
                    "|1619403958" +
                    "|2021-04-26 02:25:58" +
                    "|mingshewhe(小明)" +
                    "|mr 2|\"--测试|\""
            )
        )
    }

    @Test
    fun testCheckSha() {
        Assert.assertTrue(RegexUtil.checkSha("c43845b2015e1ba38682b763b0030bfd47bcb361"))
        Assert.assertTrue(RegexUtil.checkSha("c43845b2"))
        Assert.assertFalse(RegexUtil.checkSha("master"))
        Assert.assertTrue(RegexUtil.checkSha("c43845b2015e1"))
    }
}
