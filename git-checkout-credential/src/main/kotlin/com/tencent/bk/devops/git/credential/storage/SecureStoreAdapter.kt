package com.tencent.bk.devops.git.credential.storage

import com.microsoft.alm.secret.Credential
import com.microsoft.alm.storage.SecretStore
import java.net.URI

class SecureStoreAdapter(
    private val backingStore: SecretStore<Credential>
) : ICredentialStore {

    companion object {
        private const val CREDENTIAL_NAMESPACE = "devops"
    }

    private val uriNameConversion = DevopsUriNameConversion()

    override fun get(targetUri: URI): Credential? {
        val targetName = uriNameConversion.convert(targetUri, CREDENTIAL_NAMESPACE)
        return backingStore.get(targetName)
    }

    override fun add(targetUri: URI, credential: Credential) {
        val targetName = uriNameConversion.convert(targetUri, CREDENTIAL_NAMESPACE)
        backingStore.add(targetName, credential)
    }

    override fun delete(targetUri: URI) {
        val targetName = uriNameConversion.convert(targetUri, CREDENTIAL_NAMESPACE)
        backingStore.delete(targetName)
    }
}
