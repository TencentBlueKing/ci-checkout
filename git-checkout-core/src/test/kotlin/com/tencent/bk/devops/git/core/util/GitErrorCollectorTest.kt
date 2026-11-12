package com.tencent.bk.devops.git.core.util

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
