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

package com.tencent.bk.devops.git.core.pojo.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.tencent.bk.devops.git.core.exception.ParamInvalidException
import java.net.URLEncoder

class RepositoryConfig(
    val repositoryHashId: String?,
    val repositoryName: String?,
    val repositoryType: RepositoryType
) {
    @JsonIgnore
    fun getRepositoryId(): String {
        return when (repositoryType) {
            RepositoryType.ID -> if (repositoryHashId.isNullOrBlank()) {
                throw ParamInvalidException(errorMsg = "代码库HashId为空")
            } else {
                repositoryHashId
            }
            RepositoryType.NAME, RepositoryType.URL -> if (repositoryName.isNullOrBlank()) {
                throw ParamInvalidException(errorMsg = "代码库名为空")
            } else {
                repositoryName
            }
        }
    }

    @JsonIgnore
    fun getURLEncodeRepositoryId(): String = URLEncoder.encode(getRepositoryId(), "UTF-8")

    override fun toString(): String {
        return "[repositoryHashId=$repositoryHashId, repositoryName=$repositoryName, repositoryType=$repositoryType]"
    }
}
