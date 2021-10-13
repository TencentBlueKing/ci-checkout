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

import com.tencent.bk.devops.git.core.constant.GitConstants.AGENT_PID_VAR
import com.tencent.bk.devops.git.core.constant.GitConstants.AGENT_PID_VAR2
import com.tencent.bk.devops.git.core.constant.GitConstants.AUTH_SOCKET_VAR
import com.tencent.bk.devops.git.core.constant.GitConstants.AUTH_SOCKET_VAR2
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_CUR_COMMITS
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_HEAD_COMMIT_AUTHOR
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_HEAD_COMMIT_COMMENT
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_HEAD_COMMIT_COMMITTER
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_HEAD_COMMIT_ID
import com.tencent.bk.devops.git.core.constant.GitConstants.BK_CI_GIT_REPO_LAST_COMMIT_ID
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_GIT_REPO_CUR_COMMITS
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_GIT_REPO_HEAD_COMMIT_COMMENT
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_GIT_REPO_HEAD_COMMIT_ID
import com.tencent.bk.devops.git.core.constant.GitConstants.DEVOPS_GIT_REPO_LAST_COMMIT_ID
import com.tencent.bk.devops.git.core.constant.GitConstants.PARAM_SEPARATOR
import com.tencent.bk.devops.git.core.pojo.api.CommitMaterial

object EnvHelper {

    private val env = mutableMapOf<String, String>()

    private val copyOnThreadLocal = object : ThreadLocal<MutableMap<String, String>>() {
        override fun initialValue(): MutableMap<String, String> {
            return mutableMapOf()
        }
    }

    fun addSshAgent(agentEnv: Map<String, String>) {
        env.putAll(agentEnv)
    }

    fun addLogEnv(
        projectName: String,
        commitMaterial: CommitMaterial
    ) {
        val envProjectName = projectName.replace("/", ".")
        if (commitMaterial.lastCommitId != null) {
            env["git.$envProjectName.last.commit"] = commitMaterial.lastCommitId
        }
        if (commitMaterial.newCommitId != null) {
            env["git.$envProjectName.new.commit"] = commitMaterial.newCommitId
        }

        env[DEVOPS_GIT_REPO_CUR_COMMITS] = commitMaterial.commitIds.joinToString(PARAM_SEPARATOR)
        env[DEVOPS_GIT_REPO_LAST_COMMIT_ID] = commitMaterial.lastCommitId ?: ""
        env[DEVOPS_GIT_REPO_HEAD_COMMIT_ID] = commitMaterial.newCommitId ?: ""
        env[DEVOPS_GIT_REPO_HEAD_COMMIT_COMMENT] = commitMaterial.newCommitComment ?: ""

        env[BK_CI_GIT_REPO_CUR_COMMITS] = commitMaterial.commitIds.joinToString(PARAM_SEPARATOR)
        env[BK_CI_GIT_REPO_LAST_COMMIT_ID] = commitMaterial.lastCommitId ?: ""
        env[BK_CI_GIT_REPO_HEAD_COMMIT_ID] = commitMaterial.newCommitId ?: ""
        env[BK_CI_GIT_REPO_HEAD_COMMIT_COMMENT] = commitMaterial.newCommitComment ?: ""
        env[BK_CI_GIT_REPO_HEAD_COMMIT_AUTHOR] = commitMaterial.newCommitAuthor ?: ""
        env[BK_CI_GIT_REPO_HEAD_COMMIT_COMMITTER] = commitMaterial.newCommitAuthor ?: ""
    }

    fun getAuthEnv(): Map<String, String> {
        return listOf(
            AUTH_SOCKET_VAR, AGENT_PID_VAR, AUTH_SOCKET_VAR2, AGENT_PID_VAR2
        ).filter { env[it] != null }.associateBy({ it }, { env[it]!! })
    }

    fun getEnvVariable(key: String): String? {
        return env[key]
    }

    fun addEnvVariable(key: String, value: String) {
        env[key] = value
    }

    fun getEnvVariables(): Map<String, String> {
        return env
    }

    fun putContext(key: String, value: String) {
        copyOnThreadLocal.get()[key] = value
    }

    fun remove(key: String) {
        copyOnThreadLocal.get().remove(key)
    }

    fun clear() {
        copyOnThreadLocal.remove()
    }

    fun getContextMap(): Map<String, String> {
        return copyOnThreadLocal.get()
    }
}
