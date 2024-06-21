package com.tencent.bk.devops.git.core.enums

enum class FetchStrategy {
    // 全量拉
    FULL,
    // 构建机缓存
    VM_CACHE,
    // 制品库缓存
    BKREPO_CACHE,
    // TGIT缓存
    TGIT_CACHE
}
