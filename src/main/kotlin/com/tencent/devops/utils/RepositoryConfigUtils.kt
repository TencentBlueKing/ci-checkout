package com.tencent.devops.utils

import com.tencent.devops.enums.RepositoryConfig
import com.tencent.devops.enums.RepositoryType

object RepositoryConfigUtils {

    fun buildConfig(repositoryId: String, repositoryType: RepositoryType?) =
        if (repositoryType == null || repositoryType == RepositoryType.ID) {
            RepositoryConfig(repositoryId, null, RepositoryType.ID)
        } else {
            RepositoryConfig(null, repositoryId, RepositoryType.NAME)
        }
}
