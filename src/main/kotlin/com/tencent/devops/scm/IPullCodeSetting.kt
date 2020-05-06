package com.tencent.devops.scm

import com.tencent.devops.enums.ticket.CredentialType
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
        return env
    }
}
