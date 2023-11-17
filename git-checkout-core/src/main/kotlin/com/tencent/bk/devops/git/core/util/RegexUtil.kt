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
import com.tencent.bk.devops.git.core.pojo.GitPackingPhase
import java.util.regex.Pattern

object RegexUtil {

    private val LOG_PATTERN = Pattern.compile(
        "(?<commitId>[0-9a-f]{40})\\|" +
                "(?<committerName>.+?)\\|" +
                "(?<commitTime>\\w+?)\\|[\\S\\s]+?\\|" +
                "(?<authorName>.+?)\\|" +
                "(?<commitMessage>.*)"
    )

    private val REPORT_TRANSFER_RATE_AND_TOTIL_SIZE = Pattern.compile(
        "remote: Total [0-9]*\\.?[0-9]*s " +
                "\\(counting objects (?<counting>[0-9]*\\.?[0-9]*)s " +
                "finding sources (?<findingSources>[0-9]*\\.?[0-9]*)s " +
                "getting size (?<gettingSize>[0-9]*\\.?[0-9]*)s " +
                "writing (?<writing>[0-9]*\\.?[0-9]*)s\\), " +
                "transfer rate (?<transferRate>[0-9]*\\.?[0-9]*) M/s " +
                "\\(total size (?<totalSize>[0-9]*\\.?[0-9]*)M\\)"
    )

    private val SHA_PATTERN = Pattern.compile("([0-9a-f]{40})|([0-9a-f]{6,8})")

    fun parseLog(log: String): CommitLogInfo? {
        val matcher = LOG_PATTERN.matcher(log)
        if (matcher.find()) {
            return CommitLogInfo(
                commitId = matcher.group("commitId"),
                committerName = matcher.group("committerName"),
                commitTime = matcher.group("commitTime").toLong(),
                authorName = matcher.group("authorName"),
                commitMessage = matcher.group("commitMessage")
            )
        }
        return null
    }

    fun parseReport(message: String): GitPackingPhase? {
        val matcher = REPORT_TRANSFER_RATE_AND_TOTIL_SIZE.matcher(message)
        if (matcher.find()) {
            return GitPackingPhase(
                counting = matcher.group("counting"),
                findingSources = matcher.group("findingSources"),
                gettingSize = matcher.group("gettingSize"),
                writing = matcher.group("writing"),
                transferRate = matcher.group("transferRate"),
                totalSize = matcher.group("totalSize")
            )
        }
        return null
    }

    /**
     * 是否为IP地址
     */
    fun isIPAddress(input: String): Boolean {
        val pattern = Regex("^(([0-9]{1,3}\\.){3}[0-9]{1,3})(:[0-9]{1,5})?$")
        return pattern.matches(input)
    }

    fun checkSha(commitId: String): Boolean = SHA_PATTERN.matcher(commitId).matches()
}
