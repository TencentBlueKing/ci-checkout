package com.tencent.bk.devops.git.core.service.helper

import com.tencent.bk.devops.git.core.pojo.GitSourceSettings

class DefaultGitUserConfigHelper : IGitUserConfigHelper {

    override fun getUserConfig(settings: GitSourceSettings): Pair<String?, String?> {
        return Pair(settings.usernameConfig, settings.userEmailConfig)
    }
}
