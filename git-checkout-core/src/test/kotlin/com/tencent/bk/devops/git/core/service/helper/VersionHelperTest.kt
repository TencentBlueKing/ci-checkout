package com.tencent.bk.devops.git.core.service.helper

import org.junit.Assert
import org.junit.Test

internal class VersionHelperTest {

    @Test
    fun computeGitVersion() {
        Assert.assertEquals(VersionHelper.computeGitVersion("git version 2.30.2.windows.1"), 2300201L)
        Assert.assertEquals(VersionHelper.computeGitVersion("git version 2.30.0.155.g66e871b"), 2300155L)
        Assert.assertEquals(VersionHelper.computeGitVersion("git version 2.19.1"), 2190100L)
    }

    @Test
    fun computeVersionFromBits() {
        Assert.assertEquals(VersionHelper.computeVersionFromBits(2, 22, 0, 0), 2220000)
    }
}
