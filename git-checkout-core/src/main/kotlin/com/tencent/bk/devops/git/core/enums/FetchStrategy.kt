package com.tencent.bk.devops.git.core.enums

enum class FetchStrategy {
    // 全量拉取
    FULL,

    // 构建机缓存
    VM_CACHE,

    // 全量 + TGIT缓存
    TGIT_CACHE,

    // 增量 + TGIT缓存
    VM_TGIT_CACHE,

    // 制品库缓存
    BKREPO_CACHE
}
