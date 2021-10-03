package com.tencent.bk.devops.git.credential.storage

import com.microsoft.alm.helpers.SystemHelper
import com.microsoft.alm.secret.Credential
import com.microsoft.alm.storage.macosx.KeychainSecurityBackedCredentialStore
import com.microsoft.alm.storage.windows.CredManagerBackedCredentialStore
import java.net.URI

class CredentialStore: ICredentialStore {
    private var backingStore: ICredentialStore? = null

    override fun get(targetUri: URI): Credential? {
        ensureBackingStore()
        return backingStore?.get(targetUri)
    }

    override fun add(targetUri: URI, credential: Credential) {
        ensureBackingStore()
        backingStore?.add(targetUri, credential)
    }

    override fun delete(targetUri: URI) {
        ensureBackingStore()
        backingStore?.delete(targetUri)
    }

    private fun ensureBackingStore() {
        backingStore = when {
            SystemHelper.isLinux() ->
                CacheSecureStore()
            SystemHelper.isMac() ->
                SecureStoreAdapter(KeychainSecurityBackedCredentialStore())
            SystemHelper.isWindows() ->
                SecureStoreAdapter(CredManagerBackedCredentialStore())
            else ->
                null
        }
    }
}