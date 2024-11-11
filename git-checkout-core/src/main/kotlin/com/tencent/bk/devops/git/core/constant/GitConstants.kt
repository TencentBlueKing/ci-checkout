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

package com.tencent.bk.devops.git.core.constant

import com.tencent.bk.devops.git.core.service.helper.VersionHelper

@SuppressWarnings("MagicNumber")
object GitConstants {

    // error code
    const val DEFAULT_ERROR = 2199001 // 插件默认异常
    const val CONFIG_ERROR = 800001 // 用户配置有误
    const val DEPEND_ERROR = 2199003 // 插件依赖异常
    const val EXEC_FAILED = 2199004 // 用户任务执行失败
    const val GIT_ERROR = 2190001 // 工蜂服务异常

    // system
    const val BK_CI_BUILD_ID = "BK_CI_BUILD_ID"
    const val BK_CI_PROJECT_ID = "BK_CI_PROJECT_ID"
    const val BK_CI_BUILD_JOB_ID = "BK_CI_BUILD_JOB_ID"
    const val BK_CI_PIPELINE_ID = "BK_CI_PIPELINE_ID"
    const val BK_CI_ATOM_CODE = "BK_CI_ATOM_CODE"
    const val BK_CI_HOOK_BRANCH = "BK_CI_HOOK_BRANCH"
    const val BK_CI_REPO_WEBHOOK_REPO_URL = "BK_CI_REPO_WEBHOOK_REPO_URL"
    const val BK_CI_HOOK_REVISION = "BK_CI_HOOK_REVISION"
    const val BK_CI_REPO_GIT_WEBHOOK_EVENT_TYPE = "BK_CI_REPO_GIT_WEBHOOK_EVENT_TYPE"
    const val BK_REPO_GIT_WEBHOOK_MR_MERGE_COMMIT_SHA = "BK_REPO_GIT_WEBHOOK_MR_MERGE_COMMIT_SHA"
    const val JOB_POOL = "JOB_POOL"
    const val BUILD_TYPE = "build.type"

    const val ORIGIN_REMOTE_NAME = "origin"
    const val DEVOPS_VIRTUAL_REMOTE_NAME = "devops-virtual-origin"
    const val DEVOPS_VIRTUAL_BRANCH = "devops-virtual-branch"
    const val FETCH_HEAD = "FETCH_HEAD"
    const val CI_EVENT = "ci.event"
    const val HOME = "HOME"

    // git env
    const val GIT_TERMINAL_PROMPT = "GIT_TERMINAL_PROMPT"
    const val GCM_INTERACTIVE = "GCM_INTERACTIVE"
    const val GIT_LFS_SKIP_SMUDGE = "GIT_LFS_SKIP_SMUDGE"
    const val GIT_LFS_FORCE_PROGRESS = "GIT_LFS_FORCE_PROGRESS"
    const val GIT_HTTP_USER_AGENT = "GIT_HTTP_USER_AGENT"
    const val GIT_CREDENTIAL_HELPER = "credential.helper"
    const val GIT_CREDENTIAL_USERNAME = "credential.username"
    const val GIT_CREDENTIAL_TASKID = "credential.taskId"
    const val GIT_CREDENTIAL_INSTEADOF_KEY = "credential.insteadOfKey"
    const val GIT_CREDENTIAL_HELPER_VALUE_REGEX = "git-checkout.sh"
    const val GIT_CHECKOUT_CREDENTIAL_VALUE_REGEX = "git-checkout-credential.sh"
    const val GIT_TRACE = "GIT_TRACE"
    const val GIT_CREDENTIAL_COMPATIBLEHOST = "credential.compatibleHost"
    const val GIT_CREDENTIAL_AUTH_HELPER = "credential.authHelper"
    const val GIT_CREDENTIAL_USEHTTPPATH = "credential.useHttpPath"
    const val GIT_REPO_PATH = "GIT_REPO_PATH"
    const val BK_CI_GIT_PROJECT_ID = "BK_CI_GIT_PROJECT_ID"
    const val BK_CI_REPO_WEB_HOOK_HASHID = "BK_CI_REPO_WEB_HOOK_HASHID"
    const val BK_CI_REPO_GIT_WEBHOOK_TAG_NAME = "BK_CI_REPO_GIT_WEBHOOK_TAG_NAME"
    const val BK_CI_HOOK_TARGET_BRANCH = "BK_CI_HOOK_TARGET_BRANCH"
    const val BK_CI_HOOK_SOURCE_BRANCH = "BK_CI_HOOK_SOURCE_BRANCH"
    const val BK_CI_START_TYPE = "BK_CI_START_TYPE"

    // auth env
    const val AUTH_SOCKET_VAR = "SSH_AUTH_SOCK"
    const val AGENT_PID_VAR = "SSH_AGENT_PID"
    const val AUTH_SOCKET_VAR2 = "SSH2_AUTH_SOCK"
    const val AGENT_PID_VAR2 = "SSH2_AGENT_PID"
    const val CREDENTIAL_JAVA_PATH = "credential_java_path"
    const val CREDENTIAL_JAR_PATH = "credential_jar_path"
    const val CREDENTIAL_COMPATIBLE_HOST = "credential_compatible_host"
    const val XDG_CONFIG_HOME = "XDG_CONFIG_HOME"
    const val GIT_ASKPASS = "GIT_ASKPASS"
    const val CORE_ASKPASS = "core.askpass"
    const val GIT_USERNAME_KEY = "GIT_USERNAME"
    const val GIT_PASSWORD_KEY = "GIT_PASSWORD"
    const val GIT_SSH_COMMAND = "GIT_SSH_COMMAND"
    const val GIT_SSH_COMMAND_VALUE = "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no"
    const val OAUTH2 = "oauth2"

    const val PARAM_SEPARATOR = ","

    // 历史变量
    const val DEVOPS_GIT_URLS = "DEVOPS_GIT_URLS"
    const val DEVOPS_GIT_HEAD_COMMITS = "DEVOPS_GIT_HEAD_COMMITS"
    const val DEVOPS_GIT_BRANCHES = "DEVOPS_GIT_BRANCHES"
    const val DEVOPS_GIT_CODE_PATHS = "DEVOPS_GIT_CODE_PATHS"

    const val DEVOPS_GIT_REPO_URL = "DEVOPS_GIT_REPO_URL"
    const val DEVOPS_GIT_REPO_NAME = "DEVOPS_GIT_REPO_NAME"
    const val DEVOPS_GIT_REPO_ALIAS_NAME = "DEVOPS_GIT_REPO_ALIAS_NAME"
    const val DEVOPS_GIT_REPO_BRANCH = "DEVOPS_GIT_REPO_BRANCH"
    const val DEVOPS_GIT_REPO_CODE_PATH = "DEVOPS_GIT_REPO_CODE_PATH"
    const val DEVOPS_GIT_REPO_LAST_COMMIT_ID = "DEVOPS_GIT_REPO_LAST_COMMIT_ID"
    const val DEVOPS_GIT_REPO_HEAD_COMMIT_ID = "DEVOPS_GIT_REPO_HEAD_COMMIT_ID"
    const val DEVOPS_GIT_REPO_HEAD_COMMIT_COMMENT = "DEVOPS_GIT_REPO_HEAD_COMMIT_COMMENT"
    const val DEVOPS_GIT_REPO_CUR_COMMITS = "DEVOPS_GIT_REPO_COMMITS"
    const val BK_CI_GIT_REPO_INCLUDE_PATH = "BK_CI_GIT_REPO_INCLUDE_PATH"
    const val BK_CI_GIT_REPO_EXCLUDE_PATH = "BK_CI_GIT_REPO_EXCLUDE_PATH"

    // use this
    const val BK_CI_GIT_URLS = "BK_CI_GIT_URLS"
    const val BK_CI_GIT_HEAD_COMMITS = "BK_CI_GIT_HEAD_COMMITS"
    const val BK_CI_GIT_BRANCHES = "BK_CI_GIT_BRANCHES"
    const val BK_CI_GIT_CODE_PATHS = "BK_CI_GIT_CODE_PATHS"

    const val BK_CI_GIT_REPO_URL = "BK_CI_GIT_REPO_URL"
    const val BK_CI_GIT_REPO_NAME = "BK_CI_GIT_REPO_NAME"
    const val BK_CI_GIT_REPO_ALIAS_NAME = "BK_CI_GIT_REPO_ALIAS_NAME"
    const val BK_CI_GIT_REPO_ID = "BK_CI_GIT_REPO_ID"
    const val BK_CI_GIT_REPO_TYPE = "BK_CI_GIT_REPO_TYPE"
    const val BK_CI_GIT_REPO_REF = "BK_CI_GIT_REPO_REF"
    const val BK_CI_GIT_REPO_BRANCH = "BK_CI_GIT_REPO_BRANCH"
    const val BK_CI_GIT_REPO_TAG = "BK_CI_GIT_REPO_TAG"
    const val BK_CI_GIT_REPO_CODE_PATH = "BK_CI_GIT_REPO_CODE_PATH"
    const val BK_CI_GIT_REPO_LAST_COMMIT_ID = "BK_CI_GIT_REPO_LAST_COMMIT_ID"
    const val BK_CI_GIT_REPO_HEAD_COMMIT_ID = "BK_CI_GIT_REPO_HEAD_COMMIT_ID"
    const val BK_CI_GIT_REPO_HEAD_COMMIT_COMMENT = "BK_CI_GIT_REPO_HEAD_COMMIT_COMMENT"
    const val BK_CI_GIT_REPO_HEAD_COMMIT_AUTHOR = "BK_CI_GIT_REPO_HEAD_COMMIT_AUTHOR"
    const val BK_CI_GIT_REPO_HEAD_COMMIT_COMMITTER = "BK_CI_GIT_REPO_HEAD_COMMIT_COMMITTER"
    const val BK_CI_GIT_REPO_CUR_COMMITS = "BK_CI_GIT_REPO_COMMITS"
    const val BK_CI_REPO_GITHUB_WEBHOOK_CREATE_REF_TYPE = "BK_CI_REPO_GITHUB_WEBHOOK_CREATE_REF_TYPE"
    const val BK_CI_REPO_GITHUB_WEBHOOK_CREATE_REF_NAME = "BK_CI_REPO_GITHUB_WEBHOOK_CREATE_REF_NAME"

    // mr专用
    const val BK_CI_GIT_REPO_MR_TARGET_HEAD_COMMIT_ID = "BK_CI_GIT_REPO_MR_TARGET_HEAD_COMMIT_ID"
    const val BK_REPO_GIT_WEBHOOK_MR_BASE_COMMIT = "BK_REPO_GIT_WEBHOOK_MR_BASE_COMMIT"
    const val BK_REPO_GIT_WEBHOOK_MR_SOURCE_COMMIT = "BK_REPO_GIT_WEBHOOK_MR_SOURCE_COMMIT"
    const val BK_REPO_GIT_WEBHOOK_MR_TARGET_COMMIT = "BK_REPO_GIT_WEBHOOK_MR_TARGET_COMMIT"
    const val BK_CI_GIT_REPO_MR_SOURCE_HEAD_COMMIT_ID = "BK_CI_GIT_REPO_MR_SOURCE_HEAD_COMMIT_ID"
    const val BK_CI_GIT_REPO_MR_SOURCE_HEAD_COMMIT_COMMENT = "BK_CI_GIT_REPO_MR_SOURCE_HEAD_COMMIT_COMMENT"
    const val BK_REPO_GIT_WEBHOOK_PUSH_BEFORE_COMMIT = "BK_REPO_GIT_WEBHOOK_PUSH_BEFORE_COMMIT"
    const val BK_REPO_GIT_WEBHOOK_PUSH_AFTER_COMMIT = "BK_REPO_GIT_WEBHOOK_PUSH_AFTER_COMMIT"

    const val GIT_LOG_FORMAT = "%H|%cn|%ct|%cd|%an|%s"
    const val GIT_LOG_MAX_COUNT = 50

    val SUPPORT_PARTIAL_CLONE_GIT_VERSION = VersionHelper.computeVersionFromBits(2, 22, 0, 0)
    val SUPPORT_SHALLOW_SINCE_GIT_VERSION = VersionHelper.computeVersionFromBits(2, 18, 0, 0)
    val SUPPORT_CONFIG_LOCAL_GIT_VERSION = VersionHelper.computeVersionFromBits(1, 9, 0, 0)
    val SUPPORT_RECURSE_SUBMODULES_VERSION = VersionHelper.computeVersionFromBits(1, 8, 0, 0)
    // 支持通过配置credential.helper= 禁用其他凭证管理的版本
    val SUPPORT_EMPTY_CRED_HELPER_GIT_VERSION =
        VersionHelper.computeVersionFromBits(2, 9, 0, 0)
    val SUPPORT_CRED_HELPER_GIT_VERSION =
        VersionHelper.computeVersionFromBits(1, 7, 10, 0)
    val SUPPORT_SUBMODULE_SYNC_RECURSIVE_GIT_VERSION =
        VersionHelper.computeVersionFromBits(1, 8, 1, 0)
    val SUPPORT_CHECKOUT_B_GIT_VERSION = VersionHelper.computeVersionFromBits(1, 7, 3, 0)
    val SUPPORT_SUBMODULE_UPDATE_FORCE_GIT_VERSION =
        VersionHelper.computeVersionFromBits(1, 8, 2, 0)
    val SUPPORT_MERGE_NO_VERIFY_GIT_VERSION =
        VersionHelper.computeVersionFromBits(2, 15, 0, 0)
    val SUPPORT_XDG_CONFIG_HOME_GIT_VERSION = VersionHelper.computeVersionFromBits(1, 7, 12, 0)
    val SUPPORT_SET_UPSTREAM_TO_GIT_VERSION = VersionHelper.computeVersionFromBits(1, 8, 0, 0)
    // 支持协议版本2.0
    val SUPPORT_PROTOCOL_2_0_GIT_VERSION = VersionHelper.computeVersionFromBits(2, 18, 0, 0)

    const val USER_NEED_PROJECT_X_PERMISSION = 2115025

    const val TGIT_CACHE_GRAY_WHITE_PROJECT = "TGIT_CACHE_GRAY_WHITE_PROJECT"

    const val TGIT_CACHE_GRAY_PROJECT = "TGIT_CACHE_GRAY_PROJECT"

    const val TGIT_CACHE_GRAY_WEIGHT = "TGIT_CACHE_GRAY_WEIGHT"
}
