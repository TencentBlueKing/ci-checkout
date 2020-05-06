package com.tencent.devops.pojo.utils

import java.util.Arrays

/**
 * Created by Aaron Sheng on 2017/9/29.
 */
data class DHKeyPair(
    val publicKey: ByteArray,
    val privateKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DHKeyPair

        if (!Arrays.equals(publicKey, other.publicKey)) return false
        if (!Arrays.equals(privateKey, other.privateKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = Arrays.hashCode(publicKey)
        result = 31 * result + Arrays.hashCode(privateKey)
        return result
    }
}
