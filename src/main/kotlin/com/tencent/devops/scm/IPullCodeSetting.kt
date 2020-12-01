package com.tencent.devops.scm

import com.tencent.devops.enums.ticket.CredentialType
import com.tencent.devops.pojo.BK_CI_GIT_REPO_BRANCH
import com.tencent.devops.pojo.BK_CI_GIT_REPO_CODE_PATH
import com.tencent.devops.pojo.BK_CI_GIT_REPO_URL
import com.tencent.devops.pojo.GitCodeAtomParam
import com.tencent.devops.utils.shell.CredentialUtils

interface IPullCodeSetting {
    val params: GitCodeAtomParam

    fun pullCode(): Map<String, String>?

    fun getCredential(id: String?): Pair<List<String>, CredentialType>? {
        return if (id.isNullOrBlank()) null else CredentialUtils.getCredentialWithType(id!!)
    }

    fun getRemoteBranch(): String

    fun performTask(taskObj: GitUpdateTask): MutableMap<String, String> {
        val env = mutableMapOf<String, String>()
        val performEnv = taskObj.perform()
        if (null != performEnv) {
            env.putAll(performEnv)
        }
        env[BK_CI_GIT_REPO_URL] = params.repositoryUrl
        env[BK_CI_GIT_REPO_BRANCH] = params.refName
        env[BK_CI_GIT_REPO_CODE_PATH] = params.localPath ?: ""
        return env
    }
}
