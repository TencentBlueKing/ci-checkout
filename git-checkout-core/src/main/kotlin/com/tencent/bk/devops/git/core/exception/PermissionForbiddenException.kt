package com.tencent.bk.devops.git.core.exception

import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.plugin.pojo.ErrorType

class PermissionForbiddenException(
    override val errorType: ErrorType = ErrorType.USER,
    override val errorCode: Int = GitConstants.CONFIG_ERROR,
    override val errorMsg: String,
    override val reason: String = "",
    override val solution: String = "",
    override val wiki: String = ""
) : TaskExecuteException(errorType, errorCode, errorMsg)
