package com.tencent.bk.devops.git.core.enums

/**
 * 一条命令匹配到多个错误时,高优先级覆盖低优先级,同级保留先出现的
 */
object GitErrorPriority {
    /**
     * 只是其他错误的收尾输出,本身说明不了原因,如Could not read from remote repository
     */
    const val LOW = 0

    const val NORMAL = 50

    /**
     * 能明确指出原因的错误,如连接超时、仓库不存在、认证失败、远程服务端异常
     */
    const val HIGH = 100
}
