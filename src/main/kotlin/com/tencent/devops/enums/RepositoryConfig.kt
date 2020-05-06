package com.tencent.devops.enums

import com.fasterxml.jackson.annotation.JsonIgnore
import com.tencent.devops.exception.ParamBlankException
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
                throw ParamBlankException("代码库HashId为空")
            } else {
                repositoryHashId!!
            }
            RepositoryType.NAME -> if (repositoryName.isNullOrBlank()) {
                throw ParamBlankException("代码库名为空")
            } else {
                repositoryName!!
            }
        }
    }

    @JsonIgnore
    fun getURLEncodeRepositoryId(): String = URLEncoder.encode(getRepositoryId(), "UTF-8")

    override fun toString(): String {
        return "[repositoryHashId=$repositoryHashId, repositoryName=$repositoryName, repositoryType=$repositoryType]"
    }
}
