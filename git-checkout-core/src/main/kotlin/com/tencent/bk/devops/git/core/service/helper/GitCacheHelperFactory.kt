package com.tencent.bk.devops.git.core.service.helper

import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import java.util.ServiceLoader

object GitCacheHelperFactory {
    fun getCacheHelper(settings: GitSourceSettings): IGitCacheHelper? {
        val cacheRepoHelpers = ServiceLoader.load(IGitCacheHelper::class.java)
        return cacheRepoHelpers.sortedBy { it.getOrder() }.find { it.support(settings) }
    }
}
