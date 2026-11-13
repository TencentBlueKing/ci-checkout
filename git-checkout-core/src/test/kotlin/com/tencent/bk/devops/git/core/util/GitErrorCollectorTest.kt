package com.tencent.bk.devops.git.core.util

import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_SUBMODULE_UPDATING
import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_SUBMODULE_URL
import com.tencent.bk.devops.git.core.enums.GitErrors
import org.junit.Assert
import org.junit.Test

class GitErrorCollectorTest {

    @Test
    fun specificErrorNotOverriddenByGenericError() {
        val collector = GitErrorCollector()
        collector.collect("ssh: connect to host example.com port 22: Connection timed out", false)
        collector.collect("fatal: Could not read from remote repository.", false)
        collector.collect("", false)
        collector.collect("Please make sure you have the correct access rights", false)
        Assert.assertEquals(GitErrors.ConnectionTimeOut, collector.getGitErrors())

        val repositoryNotFound = GitErrorCollector()
        repositoryNotFound.collect("ERROR: Repository not found.", false)
        repositoryNotFound.collect("fatal: Could not read from remote repository.", false)
        Assert.assertEquals(GitErrors.RepositoryNotFoundFailed, repositoryNotFound.getGitErrors())
    }

    @Test
    fun genericErrorOverriddenBySpecificError() {
        val collector = GitErrorCollector()
        collector.collect("fatal: Could not read from remote repository.", false)
        collector.collect("fatal: Authentication failed for 'http://example.com/demo.git/'", false)
        Assert.assertEquals(GitErrors.AuthenticationFailed, collector.getGitErrors())
    }

    @Test
    fun remoteServerFailedKeepFirstWithOtherHighPriorityError() {
        val collector = GitErrorCollector()
        collector.collect("fatal: early EOF", false)
        collector.collect("fatal: Authentication failed for 'http://example.com/demo.git/'", false)
        Assert.assertEquals(GitErrors.RemoteServerFailed, collector.getGitErrors())
    }

    @Test
    fun samePriorityKeepFirst() {
        val collector = GitErrorCollector()
        collector.collect("error: RPC failed; curl 18 transfer closed with outstanding read data remaining", false)
        collector.collect("fatal: early EOF", false)
        collector.collect("fatal: Could not read from remote repository.", false)
        Assert.assertEquals(GitErrors.RemoteServerFailed, collector.getGitErrors())
    }

    @Test
    fun stdOutNotClearMatchedError() {
        val collector = GitErrorCollector()
        collector.collect("fatal: couldn't find remote ref refs/heads/aaa", false)
        collector.collect("some stdout line", true)
        Assert.assertEquals(GitErrors.NoMatchingBranch, collector.getGitErrors())
    }

    @Test
    fun stdOutMatchDeclaredRule() {
        val collector = GitErrorCollector()
        collector.collect("CONFLICT (content): Merge conflict in a.txt", true)
        collector.collect("Automatic merge failed; fix conflicts and then commit the result.", true)
        Assert.assertEquals(GitErrors.MergeConflicts, collector.getGitErrors())

        val ignored = GitErrorCollector()
        ignored.collect("fatal: refusing to merge unrelated histories", true)
        Assert.assertNull(ignored.getGitErrors())
    }

    @Test
    fun failedSubmoduleUrls() {
        EnvHelper.removeContext(CONTEXT_SUBMODULE_URL)
        val mainRepository = GitErrorCollector()
        mainRepository.collect("fatal: remote error: Git repository not found", false)
        mainRepository.collect("fatal: clone of 'git@git.example.com:sub/b.git' into submodule path 'b' failed", true)
        mainRepository.getGitErrors()
        Assert.assertNull(EnvHelper.getContext(CONTEXT_SUBMODULE_URL))

        val collector = GitErrorCollector()
        collector.collect("fatal: 远程错误：Git repository not found", false)
        collector.collect(
            "fatal: 无法克隆 'https://oauth2:token@git.example.com/sub/a.git' 到子模组路径 '/data/workspace/a'",
            false
        )
        collector.collect("fatal: remote error: Git repository not found", false)
        collector.collect("fatal: clone of 'git@git.example.com:sub/b.git' into submodule path 'b' failed", false)
        collector.collect("fatal: 无法克隆 'https://git.example.com/sub/a.git' 到子模组路径 '/data/workspace/c'", false)
        Assert.assertEquals(GitErrors.RepositoryNotFoundFailed, collector.getGitErrors())
        Assert.assertEquals(
            "https://git.example.com/sub/a.git, git@git.example.com:sub/b.git",
            EnvHelper.getContext(CONTEXT_SUBMODULE_URL)
        )
        EnvHelper.removeContext(CONTEXT_SUBMODULE_URL)
    }

    @Test
    fun submoduleUpdatingRepositoryError() {
        try {
            EnvHelper.putContext(CONTEXT_SUBMODULE_UPDATING, "true")
            val notFound = GitErrorCollector()
            notFound.collect("fatal: 远程错误：Git repository not found", false)
            notFound.collect("fatal: Unable to fetch in submodule path 'a'", false)
            Assert.assertEquals(GitErrors.SubmoduleRepositoryDoesNotExist, notFound.getGitErrors())

            val authFailed = GitErrorCollector()
            authFailed.collect("fatal: Authentication failed for 'https://git.example.com/sub/a.git/'", false)
            Assert.assertEquals(GitErrors.SubmoduleRepositoryDoesNotExist, authFailed.getGitErrors())

            val other = GitErrorCollector()
            other.collect("fatal: Unable to find current revision in submodule path 'a'", false)
            Assert.assertEquals(GitErrors.InvalidSubmoduleSHA, other.getGitErrors())
        } finally {
            EnvHelper.removeContext(CONTEXT_SUBMODULE_UPDATING)
        }
    }

    @Test
    fun errorLine() {
        val collector = GitErrorCollector()
        collector.collect("Cloning into 'demo'...", false)
        collector.collect("fatal: unable to access 'https://user:token@example.com/demo.git/': weird error", false)
        collector.collect("fatal: another error", false)
        Assert.assertNull(collector.getGitErrors())
        Assert.assertEquals(
            "fatal: unable to access 'https://user:***@example.com/demo.git/': weird error",
            collector.getErrorLine()
        )

        val chinese = GitErrorCollector()
        chinese.collect("正克隆到 'demo'...", false)
        chinese.collect("致命错误：某个未知错误", false)
        Assert.assertEquals("致命错误：某个未知错误", chinese.getErrorLine())
    }
}
