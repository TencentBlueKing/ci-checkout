package com.tencent.bk.devops.git.core.pojo

data class GitPackingPhase(
    var counting: String,
    var findingSources: String,
    var gettingSize: String,
    var writing: String,
    var transferRate: String,
    var totalSize: String
)
