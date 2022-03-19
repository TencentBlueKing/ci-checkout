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
    const val CONFIG_ERROR = 2199002 // 用户配置有误
    const val DEPEND_ERROR = 2199003 // 插件依赖异常
    const val EXEC_FAILED = 2199004 // 用户任务执行失败
    const val GIT_ERROR = 2190001 // 工蜂服务异常

    // system
    const val BK_CI_BUILD_ID = "BK_CI_BUILD_ID"
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
    const val GIT_CREDENTIAL_HELPER_VALUE_REGEX = "git-checkout-credential.sh"
    const val GIT_TRACE = "GIT_TRACE"
    const val GIT_CREDENTIAL_COMPATIBLEHOST = "credential.compatibleHost"
    const val GIT_REPO_PATH = "GIT_REPO_PATH"

    // auth env
    const val AUTH_SOCKET_VAR = "SSH_AUTH_SOCK"
    const val AGENT_PID_VAR = "SSH_AGENT_PID"
    const val AUTH_SOCKET_VAR2 = "SSH2_AUTH_SOCK"
    const val AGENT_PID_VAR2 = "SSH2_AGENT_PID"
    const val CREDENTIAL_JAVA_PATH = "credential_java_path"
    const val XDG_CONFIG_HOME = "XDG_CONFIG_HOME"
    const val GIT_ASKPASS = "GIT_ASKPASS"
    const val GIT_USERNAME_KEY = "GIT_USERNAME"
    const val GIT_PASSWORD_KEY = "GIT_PASSWORD"
    const val GIT_SSH_COMMAND = "GIT_SSH_COMMAND"
    const val GIT_SSH_COMMAND_VALUE = "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no"

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

    // mr专用
    const val BK_CI_GIT_REPO_MR_TARGET_HEAD_COMMIT_ID = "BK_CI_GIT_REPO_MR_TARGET_HEAD_COMMIT_ID"
    const val BK_REPO_GIT_WEBHOOK_MR_BASE_COMMIT = "BK_REPO_GIT_WEBHOOK_MR_BASE_COMMIT"
    const val BK_REPO_GIT_WEBHOOK_MR_SOURCE_COMMIT = "BK_REPO_GIT_WEBHOOK_MR_SOURCE_COMMIT"
    const val BK_REPO_GIT_WEBHOOK_MR_TARGET_COMMIT = "BK_REPO_GIT_WEBHOOK_MR_TARGET_COMMIT"
    const val BK_CI_GIT_REPO_MR_SOURCE_HEAD_COMMIT_ID = "BK_CI_GIT_REPO_MR_SOURCE_HEAD_COMMIT_ID"
    const val BK_CI_GIT_REPO_MR_SOURCE_HEAD_COMMIT_COMMENT = "BK_CI_GIT_REPO_MR_SOURCE_HEAD_COMMIT_COMMENT"

    const val PIPELINE_MATERIAL_URL = "pipeline.material.url"
    const val PIPELINE_MATERIAL_BRANCHNAME = "pipeline.material.branchName"
    const val PIPELINE_MATERIAL_ALIASNAME = "pipeline.material.aliasName"
    const val PIPELINE_MATERIAL_NEW_COMMIT_ID = "pipeline.material.new.commit.id"
    const val PIPELINE_MATERIAL_NEW_COMMIT_COMMENT = "pipeline.material.new.commit.comment"
    const val PIPELINE_MATERIAL_NEW_COMMIT_TIMES = "pipeline.material.new.commit.times"

    const val GIT_LOG_FORMAT = "%H|%cn|%ct|%cd|%an|%s"
    const val GIT_LOG_MAX_COUNT = 50

    val SUPPORT_PARTIAL_CLONE_GIT_VERSION = VersionHelper.computeVersionFromBits(2, 22, 0, 0)
    val SUPPORT_SHALLOW_SINCE_GIT_VERSION = VersionHelper.computeVersionFromBits(2, 18, 0, 0)
    val SUPPORT_CONFIG_LOCAL_GIT_VERSION = VersionHelper.computeVersionFromBits(1, 9, 0, 0)

    // context 上下文，在错误信息中使用 @see GitErrorsText
    const val CONTEXT_USER_ID = "context_user_id"
    const val CONTEXT_REPOSITORY_URL = "context_repository_url"
    const val CONTEXT_CREDENTIAL_ID = "context_credential_id"
    const val CONTEXT_GIT_PROTOCOL = "context_git_protocol"
    const val CONTEXT_FETCH_STRATEGY = "context_fetch_strategy" // 全量拉取
    const val CONTEXT_PREPARE_COST_TIME = "context_prepare_cost_time"
    const val CONTEXT_INIT_COST_TIME = "context_init_cost_time"
    const val CONTEXT_SUBMODULE_COST_TIME = "context_submodule_cost_time"
    const val CONTEXT_LFS_COST_TIME = "context_lfs_cost_time"
    const val CONTEXT_FETCH_COST_TIME = "context_fetch_cost_time"
    const val CONTEXT_CHECKOUT_COST_TIME = "context_checkout_cost_time"
    const val CONTEXT_LOG_COST_TIME = "context_log_cost_time"
    const val CONTEXT_AUTH_COST_TIME = "context_auth_cost_time"
    const val CONTEXT_BKREPO_DOWNLOAD_COST_TIME = "context_bkrepo_download_cost_time"
    const val CONTEXT_BKREPO_DOWNLOAD_RESULT = "context_bkrepo_download_result"
    const val CONTEXT_TRANSFER_RATE = "context_transfer_rate"
    const val CONTEXT_TOTAL_SIZE = "context_total_size"
    const val CONTEXT_ERROR_INFO = "context_error_info"

    const val wikiUrl = "https://github.com/TencentBlueKing/ci-git-checkout/wiki/" +
            "Git%E6%8F%92%E4%BB%B6%E5%B8%B8%E8%A7%81%E6%8A%A5%E9%94%99%E5%8F%8A%E8%A7%A3%E5%86%B3%E5%8A%9E%E6%B3%95"
}
