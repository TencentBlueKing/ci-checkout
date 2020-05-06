package com.tencent.devops.enums.ticket

enum class CredentialType {
    PASSWORD, // v1 = password, v2=v3=v4=null
    ACCESSTOKEN, // v1 = access_token
    USERNAME_PASSWORD, // v1 = username, v2 = password, other = null
    SECRETKEY, // v1 = secretKey, other = null
    APPID_SECRETKEY, // v1 = appId, v2 = secretKey, other = null
    SSH_PRIVATEKEY, // v1 = privateKey, v2=passphrase?
    TOKEN_SSH_PRIVATEKEY, // v1 = token, v2 = privateKey, v3=passphrase?
    TOKEN_USERNAME_PASSWORD, // v1 = token, v2 = username, v3=password
    COS_APPID_SECRETID_SECRETKEY_REGION // v1 = cosappId, v2 = secretId, v3 = secretKey, v4 = region
}
