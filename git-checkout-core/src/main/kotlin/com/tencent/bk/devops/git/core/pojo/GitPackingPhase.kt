package com.tencent.bk.devops.git.core.pojo

data class GitPackingPhase(
    val counting: String,
    val findingSources: String,
    val gettingSize: String,
    val writing: String,
    val transferRate: String,
    val totalSize: String
)
