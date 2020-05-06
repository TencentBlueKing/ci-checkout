package com.tencent.devops.utils.shell

import com.tencent.devops.api.CredentialResourceApi
import com.tencent.devops.enums.ticket.CredentialType
import com.tencent.devops.exception.ClientException
import com.tencent.devops.utils.DHUtil
import java.util.Base64
import kotlin.collections.ArrayList
import org.slf4j.LoggerFactory

/**
 * This util is to get the credential from core
 * It use DH encrypt and decrypt
 */
object CredentialUtils {

    fun getCredentialWithType(credentialId: String, showErrorLog: Boolean = true): Pair<List<String>, CredentialType> {
        if (credentialId.trim().isEmpty()) {
            throw RuntimeException("The credential Id is empty")
        }
        try {
            val pair = DHUtil.initKey()
            val encoder = Base64.getEncoder()
            logger.info("Start to get the credential($credentialId)")
            val result = CredentialResourceApi().get(credentialId, encoder.encodeToString(pair.publicKey))

            if (result.status != 0 || result.data == null) {
                logger.error("Fail to get the credential($credentialId) because of ${result.message}")
                throw ClientException(result.message!!)
            }

            val credential = result.data!!
            val list = ArrayList<String>()

            list.add(decode(credential.v1, credential.publicKey, pair.privateKey))
            if (!credential.v2.isNullOrEmpty()) {
                list.add(decode(credential.v2!!, credential.publicKey, pair.privateKey))
                if (!credential.v3.isNullOrEmpty()) {
                    list.add(decode(credential.v3!!, credential.publicKey, pair.privateKey))
                    if (!credential.v4.isNullOrEmpty()) {
                        list.add(decode(credential.v4!!, credential.publicKey, pair.privateKey))
                    }
                }
            }
            return Pair(list, credential.credentialType)
        } catch (e: Exception) {
            logger.warn("Fail to get the credential($credentialId)", e)
            if (showErrorLog) {
                logger.error("获取凭证（$credentialId）失败， 原因：${e.message}")
            }
            throw e
        }
    }

    private fun decode(encode: String, publicKey: String, privateKey: ByteArray): String {
        val decoder = Base64.getDecoder()
        return String(DHUtil.decrypt(decoder.decode(encode), decoder.decode(publicKey), privateKey))
    }

    private val logger = LoggerFactory.getLogger(CredentialUtils::class.java)
}
