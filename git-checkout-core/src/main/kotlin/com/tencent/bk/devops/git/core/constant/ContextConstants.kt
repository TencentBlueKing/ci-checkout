package com.tencent.bk.devops.git.core.constant

object ContextConstants {
    // context 上下文，在错误信息中使用 @see GitErrorsText
    const val CONTEXT_USER_ID = "context_user_id"
    const val CONTEXT_REPOSITORY_URL = "context_repository_url"
    const val CONTEXT_REPOSITORY_HTTP_URL = "context_repository_http_url"
    const val CONTEXT_CREDENTIAL_ID = "context_credential_id"
    const val CONTEXT_GIT_PROTOCOL = "context_git_protocol"
    const val CONTEXT_FETCH_STRATEGY = "context_fetch_strategy" // 全量拉取
    const val CONTEXT_CHECKOUT_REF = "context_checkout_ref"
    const val CONTEXT_MERGE_SOURCE_REF = "context_merge_source_ref"
    const val CONTEXT_MERGE_TARGET_REF = "context_merge_target_ref"
    const val CONTEXT_REPOSITORY_TYPE = "context_repository_type"
    const val CONTEXT_REPOSITORY_ALIAS_NAME = "context_repository_alias_name"
    const val CONTEXT_REPOSITORY_HASH_ID = "context_repository_hash_id"

    // 度量
    const val CONTEXT_PREPARE_COST_TIME = "context_prepare_cost_time"
    const val CONTEXT_INIT_COST_TIME = "context_init_cost_time"
    const val CONTEXT_SUBMODULE_COST_TIME = "context_submodule_cost_time"
    const val CONTEXT_LFS_COST_TIME = "context_lfs_cost_time"
    const val CONTEXT_FETCH_COST_TIME = "context_fetch_cost_time"
    const val CONTEXT_CHECKOUT_COST_TIME = "context_checkout_cost_time"
    const val CONTEXT_LOG_COST_TIME = "context_log_cost_time"
    const val CONTEXT_AUTH_COST_TIME = "context_auth_cost_time"
    const val CONTEXT_CACHE_DOWNLOAD_COST_TIME = "context_cache_download_cost_time"
    const val CONTEXT_CACHE_DOWNLOAD_RESULT = "context_cache_download_result"
    const val CONTEXT_CACHE_SIZE = "context_cache_size"
    const val CONTEXT_TRANSFER_RATE = "context_transfer_rate"
    const val CONTEXT_TOTAL_SIZE = "context_total_size"
    const val CONTEXT_ERROR_INFO = "context_error_info"
    const val CONTEXT_GIT_VERSION = "context_git_version"
    const val CONTEXT_INVALID_REF = "context_invalid_ref"
    const val CONTEXT_WOA_PROXY = "context_woa_proxy"
    const val CONTEXT_BACKUP_INSTEADOF = "context_backup_insteadOf"
    // 构建机是否有代码库
    const val CONTEXT_VM_EXIST_REPO = "context_vm_exist_repo"
}
