package com.tencent.devops.enums

enum class ScmType {
    CODE_SVN,
    CODE_GIT,
    CODE_GITLAB,
    GITHUB;

    companion object {
        fun parse(type: ScmType): Short {
            return when (type) {
                CODE_SVN -> 1.toShort()
                CODE_GIT -> 2.toShort()
                CODE_GITLAB -> 3.toShort()
                GITHUB -> 4.toShort()
            }
        }
    }
}
