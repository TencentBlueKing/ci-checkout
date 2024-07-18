package com.tencent.bk.devops.git.core.enums

enum class FetchStrategy {
    // 直连git服务器
    NO_CACHE,

    // TGIT缓存
    TGIT_CACHE,

    // 制品库缓存
    BKREPO_CACHE
}
