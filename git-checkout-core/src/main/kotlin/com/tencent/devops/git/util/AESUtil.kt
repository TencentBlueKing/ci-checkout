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

package com.tencent.devops.git.util

import com.tencent.devops.git.exception.EncryptException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AESUtil {
    // 字符串编码
    private const val KEY_CHARSET = "UTF-8"

    // 算法方式
    private const val KEY_ALGORITHM = "AES"

    // 算法/模式/填充
    private const val CIPHER_ALGORITHM_CBC = "AES/CBC/PKCS5Padding"

    // 私钥大小128/192/256(bits)位 即：16/24/32bytes，暂时使用128，如果扩大需要更换java/jre里面的jar包
    private const val PRIVATE_KEY_SIZE_BIT = 128

    private const val PRIVATE_KEY_SIZE_BYTE = 16

    fun encrypt(secretKey: String, plainText: String): String {
        if (secretKey.length != PRIVATE_KEY_SIZE_BYTE) {
            throw EncryptException(errorMsg = "AESUtil:Invalid AES secretKey length (must be 16 bytes)")
        }
        // 加密模式初始化参数
        val cipher: Cipher = initParam(secretKey, Cipher.ENCRYPT_MODE)
        // 获取加密内容的字节数组
        val bytePlainText = plainText.toByteArray(charset(KEY_CHARSET))
        // 执行加密
        val byteCipherText = cipher.doFinal(bytePlainText)
        return Base64.getEncoder().encodeToString(byteCipherText)
    }

    fun decrypt(secretKey: String, cipherText: String): String {
        if (secretKey.length != PRIVATE_KEY_SIZE_BYTE) {
            throw EncryptException(errorMsg = "AESUtil:Invalid AES secretKey length (must be 16 bytes)")
        }

        val cipher = initParam(secretKey, Cipher.DECRYPT_MODE)
        // 将加密并编码后的内容解码成字节数组
        val byteCipherText = Base64.getDecoder().decode(cipherText)
        // 解密
        val bytePlainText = cipher.doFinal(byteCipherText)
        return String(bytePlainText, Charsets.UTF_8)
    }

    private fun initParam(secretKey: String, mode: Int): Cipher {
        return try {
            // 防止Linux下生成随机key
            val secureRandom = SecureRandom.getInstance("SHA1PRNG")
            secureRandom.setSeed(secretKey.toByteArray())
            // 获取key生成器
            val keygen = KeyGenerator.getInstance(KEY_ALGORITHM)
            keygen.init(PRIVATE_KEY_SIZE_BIT, secureRandom)

            // 获得原始对称密钥的字节数组
            val raw = secretKey.toByteArray()

            // 根据字节数组生成AES内部密钥
            val key = SecretKeySpec(raw, KEY_ALGORITHM)
            // 根据指定算法"AES/CBC/PKCS5Padding"实例化密码器
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM_CBC)
            val iv = IvParameterSpec(secretKey.toByteArray())

            // 初始化AES密码器
            cipher.init(mode, key, iv)
            cipher
        } catch (ignore: Exception) {
            throw EncryptException(errorMsg = "AESUtil:initParam fail!")
        }
    }
}
