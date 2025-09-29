package com.tencent.bk.devops.git.core.pojo

/**
 * Git合并请求数据类
 * 用于表示Git合并请求的详细信息，包括冲突处理
 */
data class PreMergeCommit(
    /**
     * 合并请求的唯一标识符
     */
    val id: String,

    /**
     * 合并请求的描述信息
     */
    val message: String,

    /**
     * 是否存在冲突
     */
    val conflict: Boolean,

    /**
     * 冲突文件列表，当conflict为true时包含具体的冲突文件路径
     */
    val conflictFiles: List<String>
)