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

package com.tencent.devops.git.constant

object GitConstants {

    // error code
    const val DEFAULT_ERROR = 2199001 // 插件默认异常
    const val CONFIG_ERROR = 2199002 // 用户配置有误
    const val DEPEND_ERROR = 2199003 // 插件依赖异常
    const val EXEC_FAILED = 2199004 // 用户任务执行失败
    const val GIT_ERROR = 2190001 // 工蜂服务异常

    // system
    const val BK_CI_BUILD_ID = "BK_CI_BUILD_ID"
    const val VM_SEQ_ID = "VM_SEQ_ID"

    const val ORIGIN_REMOTE_NAME = "origin"
    const val DEVOPS_VIRTUAL_REMOTE_NAME = "devops-virtual-origin"
    const val DEVOPS_VIRTUAL_BRANCH = "devops-virtual-branch"
    const val FETCH_HEAD = "FETCH_HEAD"

    // git env
    const val GIT_TERMINAL_PROMPT = "GIT_TERMINAL_PROMPT"
    const val GCM_INTERACTIVE = "GCM_INTERACTIVE"
    const val GIT_LFS_SKIP_SMUDGE = "GIT_LFS_SKIP_SMUDGE"
    const val GIT_HTTP_USER_AGENT = "GIT_HTTP_USER_AGENT"
    const val GIT_CREDENTIAL_HELPER = "credential.helper"
    const val GIT_TRACE = "GIT_TRACE"
    const val GIT_CREDENTIAL_COMPATIBLEHOST = "credential.compatibleHost"

    // auth env
    const val XDG_CONFIG_HOME = "XDG_CONFIG_HOME"
    const val AUTH_SOCKET_VAR = "SSH_AUTH_SOCK"
    const val AGENT_PID_VAR = "SSH_AGENT_PID"
    const val AUTH_SOCKET_VAR2 = "SSH2_AUTH_SOCK"
    const val AGENT_PID_VAR2 = "SSH2_AGENT_PID"

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

    const val PIPELINE_MATERIAL_URL = "pipeline.material.url"
    const val PIPELINE_MATERIAL_BRANCHNAME = "pipeline.material.branchName"
    const val PIPELINE_MATERIAL_ALIASNAME = "pipeline.material.aliasName"
    const val PIPELINE_MATERIAL_NEW_COMMIT_ID = "pipeline.material.new.commit.id"
    const val PIPELINE_MATERIAL_NEW_COMMIT_COMMENT = "pipeline.material.new.commit.comment"
    const val PIPELINE_MATERIAL_NEW_COMMIT_TIMES = "pipeline.material.new.commit.times"

    const val GIT_LOG_FORMAT = "%H|%cn|%ct|%cd|%an|%s"
    const val GIT_LOG_MAX_COUNT = 50
}
