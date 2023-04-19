package com.tencent.bk.devops.git.core.service.helper

import com.tencent.bk.devops.git.core.enums.ScmType

interface IGitTypeParseHelper {

    /**
     * 获取代码库类型
     */
    fun getScmType(hostName: String): ScmType
}
