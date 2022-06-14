package com.tencent.bk.devops.git.core.service.helper

import com.tencent.bk.devops.git.core.pojo.GitSourceSettings

interface IGitUserConfigHelper {

    /**
     * 获取git config user.name和user.email的值
     */
    fun getUserConfig(settings: GitSourceSettings): Pair<String?, String?>
}
