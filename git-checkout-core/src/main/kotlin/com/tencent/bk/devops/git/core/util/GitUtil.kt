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

import com.tencent.bk.devops.git.core.exception.ParamInvalidException
import com.tencent.bk.devops.git.core.pojo.ServerInfo
import java.net.URLDecoder
import java.net.URLEncoder

@Suppress("ALL")
object GitUtil {

    private const val PRE_PUSH_BRANCH_NAME_PREFIX = "refs/for/"

    fun urlDecode(s: String) = URLDecoder.decode(s, "UTF-8")

    fun urlEncode(s: String) = URLEncoder.encode(s, "UTF-8")

    fun isHttpProtocol(url: String): Boolean {
        return url.startsWith("http") || url.startsWith("https")
    }

    fun getServerInfo(url: String): ServerInfo {
        return if (isHttpProtocol(url)) {
            val groups = Regex("(http[s]?://)(.*:.*@)?(([-.a-z0-9A-Z]+)(:[0-9]+)?)/(.*).git").find(url)?.groups
                ?: throw ParamInvalidException(errorMsg = "Invalid git url $url")
            ServerInfo(
                origin = "${groups[1]!!.value}${groups[3]!!.value}",
                hostName = groups[3]!!.value,
                repositoryName = groups[6]!!.value,
                httpProtocol = true
            )
        } else {
            val groups = Regex("(git@([-.a-z0-9A-Z]+)):(.*).git").find(url)?.groups
                ?: throw ParamInvalidException(errorMsg = "Invalid git url $url")
            ServerInfo(
                origin = groups[1]!!.value,
                hostName = groups[2]!!.value,
                repositoryName = groups[3]!!.value,
                httpProtocol = false
            )
        }
    }

    fun isSameRepository(repositoryUrl: String, otherRepositoryUrl: String?, hostNameList: List<String>?): Boolean {
        if (otherRepositoryUrl.isNullOrBlank()) {
            return false
        }
        val serverUrl = getServerInfo(url = repositoryUrl)
        val sourceServerUrl = getServerInfo(url = otherRepositoryUrl)
        return isSameHostName(serverUrl.hostName, sourceServerUrl.hostName, hostNameList) &&
            serverUrl.repositoryName == sourceServerUrl.repositoryName
    }

    private fun isSameHostName(targetHostName: String, sourceHostName: String, hostNameList: List<String>?): Boolean {
        val isContains = hostNameList?.containsAll(setOf(targetHostName, sourceHostName)) ?: false
        return targetHostName == sourceHostName || isContains
    }

    // whether code_git enables pre-push
    fun isPrePushBranch(branchName: String?): Boolean {
        if (branchName == null) {
            return false
        }
        return branchName.startsWith(PRE_PUSH_BRANCH_NAME_PREFIX)
    }
}
