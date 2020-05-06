package com.tencent.devops.pojo.utils

import com.tencent.devops.enums.ticket.CredentialType

data class CredentialInfo(
    val publicKey: String,
    val credentialType: CredentialType,
    val v1: String,
    val v2: String? = null,
    val v3: String? = null,
    val v4: String? = null
)
