package com.tencent.devops.enums

enum class CodePullStrategy constructor(val value: String) {
    FRESH_CHECKOUT("fresh_checkout"),
    INCREMENT_UPDATE("increment_update"),
    REVERT_UPDATE("revert_update");
}
