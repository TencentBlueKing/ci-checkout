package com.tencent.devops.enums

enum class CodeEventType {
    // git event
    PUSH,
    TAG_PUSH,
    MERGE_REQUEST,

    // github event
    CREATE,
    PULL_REQUEST,

    // svn event
    POST_COMMIT,
    LOCK_COMMIT,
    PRE_COMMIT
}
