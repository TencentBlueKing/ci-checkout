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

import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.enums.CodeEventType
import com.tencent.bk.devops.git.core.exception.ParamInvalidException
import com.tencent.bk.devops.git.core.pojo.ServerInfo
import java.net.URLDecoder
import java.net.URLEncoder

@Suppress("ALL")
object GitUtil {

    private const val PRE_PUSH_BRANCH_NAME_PREFIX = "refs/for/"
    private val HTTP_URL_REGEX = Regex("(http[s]?://)(.*:.*@)?(([-.a-z0-9A-Z]+)(:[0-9]+)?)/(.*?)(\\.git)?$")
    private val GIT_URL_REGEX = Regex("(git@([-.a-z0-9A-Z]+)):(.*?)(\\.git)?$")
    private val NOT_GIT_PROTOCOL_URL_REGEX = Regex("([-.a-z0-9A-Z]+):(.*?)(\\.git)?$")

    fun urlDecode(s: String) = URLDecoder.decode(s, "UTF-8")

    fun urlEncode(s: String) = URLEncoder.encode(s, "UTF-8")

    fun isHttpProtocol(url: String): Boolean {
        return url.startsWith("http") || url.startsWith("https")
    }

    fun getServerInfo(url: String): ServerInfo {
        return when {
            HTTP_URL_REGEX.matches(url) -> {
                val groups = HTTP_URL_REGEX.find(url)!!.groups
                ServerInfo(
                    origin = "${groups[1]!!.value}${groups[3]!!.value}",
                    hostName = groups[3]!!.value,
                    repositoryName = groups[6]!!.value,
                    httpProtocol = true
                )
            }
            GIT_URL_REGEX.matches(url) -> {
                val groups = GIT_URL_REGEX.find(url)!!.groups
                ServerInfo(
                    origin = groups[1]!!.value,
                    hostName = groups[2]!!.value,
                    repositoryName = groups[3]!!.value,
                    httpProtocol = false
                )
            }
            NOT_GIT_PROTOCOL_URL_REGEX.matches(url) -> {
                val groups = NOT_GIT_PROTOCOL_URL_REGEX.find(url)!!.groups
                ServerInfo(
                    origin = "git@${groups[1]!!.value}",
                    hostName = groups[1]!!.value,
                    repositoryName = groups[2]!!.value,
                    httpProtocol = false
                )
            }
            else -> {
                throw ParamInvalidException(
                    errorMsg = "Invalid git url $url," +
                        "url format:http[s]://HOSTNAME[:PORT]//REPONAME[.git] or git@:HOSTNAME[:PORT]:REPONAME[.git]"
                )
            }
        }
    }

    /**
     * @param hostNameList 当同一个仓库有多个域名时，传入多个域名
     */
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

    /**
     * 开启pre-merge需要满足以下条件
     * 1. 插件启用pre-merge功能
     * 2. 触发方式是webhook触发
     * 3. 触发的url与插件配置的url要是同一个仓库
     * 4. 触发的事件类型必须是mr/pr
     */
    fun isEnablePreMerge(
        enableVirtualMergeBranch: Boolean,
        repositoryUrl: String,
        hookEventType: String?,
        hookTargetUrl: String?,
        compatibleHostList: List<String>?
    ): Boolean {
        val gitHookEventType = System.getenv(GitConstants.BK_CI_REPO_GIT_WEBHOOK_EVENT_TYPE)
        // 必须先验证事件类型，再判断仓库是否相同，不然验证仓库类型时解析url会异常
        return enableVirtualMergeBranch &&
            (
                hookEventType == CodeEventType.PULL_REQUEST.name ||
                    hookEventType == CodeEventType.MERGE_REQUEST.name
                ) &&
            gitHookEventType != CodeEventType.MERGE_REQUEST_ACCEPT.name &&
            isSameRepository(
                repositoryUrl = repositoryUrl,
                otherRepositoryUrl = hookTargetUrl,
                hostNameList = compatibleHostList
            )
    }

    fun isGitEvent(gitHookEventType: String?): Boolean {
        return gitHookEventType == CodeEventType.PUSH.name ||
            gitHookEventType == CodeEventType.TAG_PUSH.name ||
            gitHookEventType == CodeEventType.MERGE_REQUEST.name ||
            gitHookEventType == CodeEventType.MERGE_REQUEST_ACCEPT.name ||
            gitHookEventType == CodeEventType.PULL_REQUEST.name
    }
}
