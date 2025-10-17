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
import com.tencent.bk.devops.git.core.enums.GitErrors
import com.tencent.bk.devops.git.core.enums.ScmType
import com.tencent.bk.devops.git.core.enums.TGitMrAction
import com.tencent.bk.devops.git.core.exception.GitExecuteException
import com.tencent.bk.devops.git.core.exception.ParamInvalidException
import com.tencent.bk.devops.git.core.pojo.AuthInfo
import com.tencent.bk.devops.git.core.pojo.ServerInfo
import com.tencent.bk.devops.git.core.service.helper.DefaultGitTypeParseHelper
import com.tencent.bk.devops.git.core.service.helper.IGitTypeParseHelper
import com.tencent.bk.devops.git.core.service.repository.GitScmService
import com.tencent.bk.devops.plugin.pojo.ErrorType
import org.slf4j.LoggerFactory
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.ServiceLoader

@Suppress("ALL")
object GitUtil {

    private const val PRE_PUSH_BRANCH_NAME_PREFIX = "refs/for/"
    private val HTTP_URL_REGEX = Regex("(http[s]?://)(.*:.*@)?(([-.a-z0-9A-Z]+)(:[0-9]+)?)/(.*?)(\\.git)?$")
    private val GIT_URL_REGEX = Regex("(git@([-.a-z0-9A-Z]+)):(.*?)(\\.git)?$")
    private val GIT_IP_SSH_URL_REGEX = Regex("ssh://git@(([0-9]{1,3}\\.){3}[0-9]{1,3})/(.*?)(\\.git)?$")
    private val GIT_IP_PORT_SSH_URL_REGEX = Regex("ssh://git@(([0-9]{1,3}\\.){3}[0-9]{1,3}):([0-9]{1,9})/(.*?)(\\.git)?$")
    private val NOT_GIT_PROTOCOL_URL_REGEX = Regex("([-.a-z0-9A-Z]+):(.*?)(\\.git)?$")
    private val logger = LoggerFactory.getLogger(GitUtil::class.java)

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
                    scheme = groups[1]!!.value,
                    origin = "${groups[1]!!.value}${groups[3]!!.value}",
                    hostName = groups[3]!!.value,
                    repositoryName = groups[6]!!.value,
                    httpProtocol = true
                )
            }
            GIT_URL_REGEX.matches(url) -> {
                val groups = GIT_URL_REGEX.find(url)!!.groups
                ServerInfo(
                    scheme = "git@",
                    origin = groups[1]!!.value,
                    hostName = groups[2]!!.value,
                    repositoryName = groups[3]!!.value,
                    httpProtocol = false
                )
            }
            GIT_IP_SSH_URL_REGEX.matches(url) -> {
                val groups = GIT_IP_SSH_URL_REGEX.find(url)!!.groups
                ServerInfo(
                    scheme = "git@",
                    origin = "git@${groups[1]!!.value}",
                    hostName = groups[1]!!.value,
                    repositoryName = groups[3]!!.value,
                    httpProtocol = false
                )
            }
            GIT_IP_PORT_SSH_URL_REGEX.matches(url) -> {
                val groups = GIT_IP_PORT_SSH_URL_REGEX.find(url)!!.groups
                ServerInfo(
                    scheme = "git@",
                    origin = "git@${groups[1]!!.value}:${groups[3]!!.value}",
                    hostName = "${groups[1]!!.value}:${groups[3]!!.value}",
                    repositoryName = groups[4]!!.value,
                    httpProtocol = false
                )
            }
            NOT_GIT_PROTOCOL_URL_REGEX.matches(url) -> {
                val groups = NOT_GIT_PROTOCOL_URL_REGEX.find(url)!!.groups
                ServerInfo(
                    scheme = "git@",
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
        // 如果存在hookTargetUrl异常则直接返回false，不进行pre-merge
        if (!checkUrl(otherRepositoryUrl)) {
            logger.debug("fail to parse repo url, repositoryUrl[$otherRepositoryUrl]")
            return false
        }
        val serverUrl = getServerInfo(url = repositoryUrl)
        val sourceServerUrl = getServerInfo(url = otherRepositoryUrl!!)
        return isSameHostName(serverUrl.hostName, sourceServerUrl.hostName, hostNameList) &&
            serverUrl.repositoryName == sourceServerUrl.repositoryName
    }

    fun isSameHostName(targetHostName: String, sourceHostName: String, hostNameList: List<String>?): Boolean {
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
        compatibleHostList: List<String>?,
        scmType: ScmType
    ): Boolean {
        // 必须先验证事件类型，再判断仓库是否相同，不然验证仓库类型时解析url会异常
        return enableVirtualMergeBranch &&
            (
                hookEventType == CodeEventType.PULL_REQUEST.name ||
                    hookEventType == CodeEventType.MERGE_REQUEST.name ||
                        hookEventType == CodeEventType.PARENT_PIPELINE.name
                ) &&
            !isMergeRequestEvent(scmType, hookEventType) &&
            isSameRepository(
                repositoryUrl = repositoryUrl,
                otherRepositoryUrl = hookTargetUrl,
                hostNameList = compatibleHostList
            )
    }

    /**
     * 是否为merge request 的merge事件
     */
    fun isMergeRequestEvent(scmType: ScmType, hookEventType: String?): Boolean {
        val gitHookEventType = System.getenv(GitConstants.BK_CI_REPO_GIT_WEBHOOK_EVENT_TYPE)
        // 兼容stream数据，github pr动作类型存的[BK_CI_REPO_GIT_WEBHOOK_MR_ACTION]变量
        val mergeAction = when (scmType) {
            ScmType.GITHUB -> System.getenv(GitConstants.BK_CI_REPO_GIT_WEBHOOK_MR_ACTION)
            else -> System.getenv(GitConstants.GIT_CI_MR_ACTION)
        }

        return gitHookEventType == CodeEventType.MERGE_REQUEST_ACCEPT.name ||
                (hookEventType in setOf(CodeEventType.MERGE_REQUEST.name, CodeEventType.PULL_REQUEST.name) &&
                        TGitMrAction.parse(mergeAction) == TGitMrAction.MERGE)
    }

    fun isGitEvent(gitHookEventType: String?): Boolean {
        return gitHookEventType == CodeEventType.PUSH.name ||
            gitHookEventType == CodeEventType.TAG_PUSH.name ||
            gitHookEventType == CodeEventType.MERGE_REQUEST.name ||
            gitHookEventType == CodeEventType.MERGE_REQUEST_ACCEPT.name ||
            gitHookEventType == CodeEventType.PULL_REQUEST.name
    }

    fun getScmType(hostName: String): ScmType {
        val gitTypeParseHelper = ServiceLoader.load(IGitTypeParseHelper::class.java).firstOrNull()
            ?: DefaultGitTypeParseHelper()
        return gitTypeParseHelper.getScmType(hostName)
    }

    private fun checkUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) {
            return false
        }
        return try {
            getServerInfo(url)
            true
        } catch (e: ParamInvalidException) {
            false
        }
    }

    fun enableCacheByStrategy(
        repositoryUrl: String,
        tGitCacheGrayWhiteProject: String?,
        tGitCacheGrayProject: String?,
        tGitCacheGrayWeight: String?
    ): Boolean {
        val gitProjectName = getServerInfo(url = repositoryUrl).repositoryName
        val hash = (gitProjectName.hashCode() and Int.MAX_VALUE) % 100
        val cacheStrategy = when {
            tGitCacheGrayWhiteProject?.split(",")?.contains(gitProjectName) ?: false -> false
            tGitCacheGrayProject?.split(",")?.contains(gitProjectName) ?: false -> true
            hash <= (tGitCacheGrayWeight?.toInt() ?: -1) -> true
            else -> false
        }
        logger.debug("enable cache by strategy: $cacheStrategy")
        return cacheStrategy
    }


    @SuppressWarnings("LongParameterList")
    fun getServerPreMerge(
        scmType: ScmType,
        repositoryUrl: String,
        authInfo: AuthInfo,
        preMerge: Boolean,
        mrIid: Int?,
        enableServerPreMerge: Boolean?
    ): Pair<Boolean, String> {
        return if (preMerge && enableServerPreMerge == true && scmType == ScmType.CODE_GIT && mrIid != null ) {
            logger.info("Creating pre-merge commit for MR#$mrIid")
            GitScmService(
                scmType = scmType,
                repositoryUrl = repositoryUrl,
                authInfo = authInfo
            ).createPreMerge(mrIid)?.let {
                // 合并冲突
                if (it.conflict) {
                    throw GitExecuteException(
                        errorType = ErrorType.USER,
                        errorCode = GitErrors.MergeConflicts.errorCode,
                        errorMsg = GitErrors.MergeConflicts.title ?: "",
                        reason = GitErrors.MergeConflicts.cause?.let {
                            PlaceholderResolver.defaultResolver.resolveByMap(it, EnvHelper.getContextMap())
                        } ?: "",
                        solution = GitErrors.MergeConflicts.solution ?: "",
                        wiki = GitErrors.MergeConflicts.wiki ?: ""
                    )
                }
                logger.debug("created pre-merge commit point [${it.id}]")
                true to it.id
            } ?: (false to "")
        } else {
            false to ""
        }
    }
}
