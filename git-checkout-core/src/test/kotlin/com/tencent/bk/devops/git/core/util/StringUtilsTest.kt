package com.tencent.bk.devops.git.core.util

import org.junit.Assert
import org.junit.Test

class StringUtilsTest {

    @Test
    fun shortCommitId() {
        Assert.assertEquals("", StringUtils.shortCommitId(null))
        Assert.assertEquals("", StringUtils.shortCommitId(""))
        Assert.assertEquals("abc", StringUtils.shortCommitId("abc"))
        Assert.assertEquals(
            "a1b2c3d",
            StringUtils.shortCommitId("a1b2c3d4e5f6789012345678901234567890abcd")
        )
    }
}
