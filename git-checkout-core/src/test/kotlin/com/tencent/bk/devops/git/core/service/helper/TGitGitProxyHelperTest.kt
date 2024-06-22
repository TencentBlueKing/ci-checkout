package com.tencent.bk.devops.git.core.service.helper

import com.tencent.bk.devops.git.core.enums.ScmType
import com.tencent.bk.devops.git.core.pojo.AuthInfo
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.nio.file.Files

@Ignore
class TGitGitProxyHelperTest {

    private var workspace: File = Files.createTempDirectory("git-checkout").toFile()
    private val settings = GitSourceSettings(
        bkWorkspace = workspace.absolutePath,
        pipelineId = "p-0dedf8a92a3147bcb5bed58dd10a667e",
        pipelineBuildId = "b-3bee3e2d0a1941fa9dc1845aa6c55dbc",
        pipelineTaskId = "e-702f4c9b54604da78f71b244b978c445",
        pipelineStartUserName = "mingshewhe",
        scmType = ScmType.CODE_GIT,
        repositoryUrl = "https://github.com/ci-plugins/git.git",
        repositoryPath = workspace.absolutePath,
        ref = "master",
        enableGitClean = true,
        fetchDepth = 1,
        lfs = true,
        preMerge = false,
        submodules = true,
        persistCredentials = true,
        authInfo = AuthInfo(
            username = "xiaoming",
            password = "123456"
        )
    )
    private val git = GitCommandManager(workspace)
    private val cacheHelper = TGitGitProxyHelper()

    @Test
    fun fetch() {
        cacheHelper.fetch(settings, git)
    }
}
