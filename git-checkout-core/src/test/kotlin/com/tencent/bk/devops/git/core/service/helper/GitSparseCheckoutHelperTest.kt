package com.tencent.bk.devops.git.core.service.helper

import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.service.GitSourceProviderTest
import org.junit.Assert
import org.junit.Test

class GitSparseCheckoutHelperTest : GitSourceProviderTest() {
    val gitSparseCheckoutHelper = GitSparseCheckoutHelper(git = GitCommandManager(workspace), settings = settings)

    @Test
    fun extractRealPathTest() {
        var extractRealPath = gitSparseCheckoutHelper.extractRealPath("/config/secrets/*.template/batch")
        Assert.assertEquals("/config/secrets", extractRealPath)
        extractRealPath = gitSparseCheckoutHelper.extractRealPath("/config/secrets/template/backend_?/batch")
        Assert.assertEquals("/config/secrets/template", extractRealPath)
        extractRealPath = gitSparseCheckoutHelper.extractRealPath("/config/secrets/data_[1-9]")
        Assert.assertEquals("/config/secrets", extractRealPath)
        extractRealPath = gitSparseCheckoutHelper.extractRealPath("/config/secrets/service/?/batch")
        Assert.assertEquals("/config/secrets/service", extractRealPath)
        extractRealPath = gitSparseCheckoutHelper.extractRealPath("?/?_[1-2]")
        Assert.assertEquals("", extractRealPath)
    }
}
