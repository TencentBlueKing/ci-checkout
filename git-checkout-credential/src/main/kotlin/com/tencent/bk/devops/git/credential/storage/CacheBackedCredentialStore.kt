package com.tencent.bk.devops.git.credential.storage

import com.microsoft.alm.secret.Credential
import com.microsoft.alm.storage.SecretStore

class CacheBackedCredentialStore: SecretStore<Credential> {
    override fun get(key: String): Credential? {
        TODO("Not yet implemented")
    }

    override fun delete(key: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun add(key: String, secret: Credential): Boolean {
        TODO("Not yet implemented")
    }

    override fun isSecure(): Boolean {
        return true
    }
}