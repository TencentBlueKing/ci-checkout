/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.devops.git.core.service.auth

import com.tencent.bk.devops.git.core.api.DevopsApi
import com.tencent.bk.devops.git.core.constant.GitConstants.CONTEXT_CREDENTIAL_ID
import com.tencent.bk.devops.git.core.exception.ApiException
import com.tencent.bk.devops.git.core.exception.ParamInvalidException
import com.tencent.bk.devops.git.core.pojo.AuthInfo
import com.tencent.bk.devops.git.core.pojo.api.CredentialInfo
import com.tencent.bk.devops.git.core.pojo.api.CredentialType
import com.tencent.bk.devops.git.core.util.DHUtil
import com.tencent.bk.devops.git.core.util.EnvHelper
import org.slf4j.LoggerFactory
import java.util.Base64

class CredentialGitAuthProvider(
    private val credentialId: String?,
    private val devopsApi: DevopsApi
) : IGitAuthProvider {

    companion object {
        private val logger = LoggerFactory.getLogger(CredentialGitAuthProvider::class.java)
    }

    override fun getAuthInfo(): AuthInfo {
        val credentialInfo = getCredential()
        EnvHelper.putContext(CONTEXT_CREDENTIAL_ID, credentialId!!)
        val gitAuthProvider = when (credentialInfo.credentialType) {
            CredentialType.ACCESSTOKEN ->
                OauthGitAuthProvider(token = credentialInfo.v1, userId = "")
            CredentialType.USERNAME_PASSWORD -> {
                if (credentialInfo.v1.isEmpty()) {
                    throw ParamInvalidException(errorMsg = "the git credential username is empty")
                }
                if (credentialInfo.v2.isNullOrBlank()) {
                    throw ParamInvalidException(errorMsg = "the git credential password is empty")
                }
                UserNamePasswordGitAuthProvider(username = credentialInfo.v1, password = credentialInfo.v2)
            }
            CredentialType.TOKEN_USERNAME_PASSWORD -> {
                if (credentialInfo.v2.isNullOrBlank()) {
                    throw ParamInvalidException(errorMsg = "the git credential username is empty")
                }
                if (credentialInfo.v3.isNullOrBlank()) {
                    throw ParamInvalidException(errorMsg = "the git credential password is empty")
                }
                UserNamePasswordGitAuthProvider(username = credentialInfo.v2, password = credentialInfo.v3)
            }
            CredentialType.SSH_PRIVATEKEY -> {
                if (credentialInfo.v1.isEmpty()) {
                    throw ParamInvalidException(errorMsg = "the git credential username is empty")
                }
                SshGitAuthProvider(privateKey = credentialInfo.v1, passPhrase = credentialInfo.v2)
            }
            CredentialType.TOKEN_SSH_PRIVATEKEY -> {
                if (credentialInfo.v2.isNullOrBlank()) {
                    throw ParamInvalidException(errorMsg = "the git credential username is empty")
                }
                SshGitAuthProvider(privateKey = credentialInfo.v2, passPhrase = credentialInfo.v3)
            }
            else ->
                EmptyGitAuthProvider()
        }
        return gitAuthProvider.getAuthInfo()
    }

    private fun getCredential(): CredentialInfo {
        if (credentialId.isNullOrBlank()) {
            throw ParamInvalidException(errorMsg = "the credential Id is empty")
        }
        logger.info("Start to get the credential($credentialId)")
        val pair = DHUtil.initKey()
        val encoder = Base64.getEncoder()
        val result = devopsApi.getCredential(credentialId, encoder.encodeToString(pair.publicKey))
        if (result.isNotOk() || result.data == null) {
            logger.error("Fail to get the credential($credentialId) because of ${result.message}")
            throw ApiException(errorMsg = result.message!!)
        }
        return result.data!!.decode(pair.privateKey)
    }

    private fun CredentialInfo.decode(privateKey: ByteArray): CredentialInfo {
        return CredentialInfo(
            publicKey = publicKey,
            credentialType = credentialType,
            v1 = decode(v1, publicKey, privateKey)!!,
            v2 = decode(v2, publicKey, privateKey),
            v3 = decode(v3, publicKey, privateKey),
            v4 = decode(v4, publicKey, privateKey)
        )
    }

    private fun decode(encode: String?, publicKey: String, privateKey: ByteArray): String? {
        if (encode == null) {
            return null
        }
        val decoder = Base64.getDecoder()
        return String(DHUtil.decrypt(decoder.decode(encode), decoder.decode(publicKey), privateKey))
    }
}
