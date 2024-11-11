package com.tencent.bk.devops.git.core.enum

import com.tencent.bk.devops.git.core.enums.GitErrors
import org.junit.Assert
import org.junit.Test

class GitErrorsTest {

    @Test
    fun authenticationFailed() {
        var gitError = GitErrors.matchError(
            "fatal: Authentication failed for 'http://example.com/demo.git/'"
        )
        Assert.assertEquals(gitError, GitErrors.AuthenticationFailed)

        gitError = GitErrors.matchError(
            "error: The requested URL returned error: 401 Unauthorized " +
                "while accessing https://github.com/mingshewhe/webhook_test.git/info/refs"
        )
        Assert.assertEquals(gitError, GitErrors.AuthenticationFailed)

        gitError = GitErrors.matchError(
            "fatal: unable to access 'http://example.com/demo.git/': The requested URL returned error: 403"
        )
        Assert.assertEquals(gitError, GitErrors.AuthenticationFailed)
        gitError = GitErrors.matchError(
            "error: The requested URL returned error: 401 while accessing http://example.com/demo.git/info/refs"
        )
        Assert.assertEquals(gitError, GitErrors.AuthenticationFailed)
        gitError = GitErrors.matchError(
            "致命错误：could not read Username for 'http://example.com': terminal prompts disabled"
        )
        Assert.assertEquals(gitError, GitErrors.AuthenticationFailed)
    }

    @Test
    fun repositoryNotFoundFailed() {
        var gitError = GitErrors.matchError(
            "fatal: 远程错误：Git repository not found"
        )
        Assert.assertEquals(gitError, GitErrors.RepositoryNotFoundFailed)

        gitError = GitErrors.matchError(
            "fatal: remote error: Git:Project not found."
        )
        Assert.assertEquals(gitError, GitErrors.RepositoryNotFoundFailed)

        gitError = GitErrors.matchError(
            "fatal: 远程错误：Git:Project not found."
        )
        Assert.assertEquals(gitError, GitErrors.RepositoryNotFoundFailed)
    }

    @Test
    fun sshAuthenticationFailed() {
        val gitError = GitErrors.matchError(
            "fatal: Could not read from remote repository."
        )
        Assert.assertEquals(gitError, GitErrors.SshAuthenticationFailed)
    }

    @Test
    fun remoteServerFailed() {
        var gitError = GitErrors.matchError(
            "fatal: 远程错误：Internal server error"
        )
        Assert.assertEquals(gitError, GitErrors.RemoteServerFailed)

        gitError = GitErrors.matchError(
            "fatal: 远端意外挂断了"
        )
        Assert.assertEquals(gitError, GitErrors.RemoteServerFailed)

        gitError = GitErrors.matchError(
            "fatal: index-pack failed"
        )
        Assert.assertEquals(gitError, GitErrors.RemoteServerFailed)

        gitError = GitErrors.matchError(
            "fatal: early EOF"
        )
        Assert.assertEquals(gitError, GitErrors.RemoteServerFailed)

        gitError = GitErrors.matchError(
            "fatal: remote error: too many request, your request was forbidden, strategy id is: [666]"
        )
        Assert.assertEquals(gitError, GitErrors.RemoteServerFailed)

        gitError = GitErrors.matchError(
            "fatal: 远程错误: too many request, your request was forbidden, strategy id is: [6644]"
        )
        Assert.assertEquals(gitError, GitErrors.RemoteServerFailed)

        gitError = GitErrors.matchError(
            "fatal: 远程错误: https://github/repo_group/repo_name.git: too many request, your request was forbidden, strategy id is: [6644]"
        )
        Assert.assertEquals(gitError, GitErrors.RemoteServerFailed)
    }

    @Test
    fun connectionTimeOut() {
        val gitError = GitErrors.matchError(
            "ssh: connect to host example.com port 8080: Connection timed out"
        )
        Assert.assertEquals(gitError, GitErrors.ConnectionTimeOut)
    }

    @Test
    fun noMatchingBranchTest() {
        var gitError = GitErrors.matchError("fatal: Couldn't find remote ref refs/heads/aaaa")
        Assert.assertEquals(gitError, GitErrors.NoMatchingBranch)

        gitError = GitErrors.matchError("fatal: 无法找到远程引用 refs/heads/aaa")
        Assert.assertEquals(gitError, GitErrors.NoMatchingBranch)

        gitError = GitErrors.matchError("fatal: 'refs/remotes/origin/aaa' 不是一个提交，不能基于它创建分支 'aaa'")
        Assert.assertEquals(gitError, GitErrors.NoMatchingBranch)

        gitError = GitErrors.matchError(
            "fatal: 'refs/remotes/origin/aaa' is not a commit and a branch 'aaa' cannot be created from it"
        )
        Assert.assertEquals(gitError, GitErrors.NoMatchingBranch)

        gitError = GitErrors.matchError("error: 路径规格 'aaa' 未匹配任何 git 已知文件")
        Assert.assertEquals(gitError, GitErrors.NoMatchingBranch)

        gitError = GitErrors.matchError("error: pathspec 'aaaa' did not match any file(s) known to git.")
        Assert.assertEquals(gitError, GitErrors.NoMatchingBranch)

        gitError = GitErrors.matchError("fatal: 引用不是一个树：b5a4cf350acdfae7d0afd2817dd83e7930f23197")
        Assert.assertEquals(gitError, GitErrors.NoMatchingBranch)

        gitError = GitErrors.matchError("fatal: reference is not a tree: bfd3ca45e6902a885019d264d42b22212df328ae")
        Assert.assertEquals(gitError, GitErrors.NoMatchingBranch)

        gitError = GitErrors.matchError("您的配置中指定要合并远程的引用 'refs/heads/aaa'")
        Assert.assertEquals(gitError, GitErrors.NoMatchingBranch)

        gitError = GitErrors.matchError("Your configuration specifies to merge with the ref 'refs/heads/aaa'")
        Assert.assertEquals(gitError, GitErrors.NoMatchingBranch)

        gitError = GitErrors.matchError("fatal: 不能同时更新路径并切换到分支'pre-release'。" +
            "\n您是想要检出 'refs/remotes/origin/pre-release' 但其未能解析为提交么？")
        Assert.assertEquals(gitError, GitErrors.NoMatchingBranch)
    }

    @Test
    fun emptyBranch() {
        var gitError = GitErrors.matchError("fatal: 您位于一个尚未初始化的分支")
        Assert.assertEquals(gitError, GitErrors.EmptyBranch)

        gitError = GitErrors.matchError("fatal: You are on a branch yet to be born")
        Assert.assertEquals(gitError, GitErrors.EmptyBranch)
    }

    @Test
    fun sparseCheckoutLeavesNoEntry() {
        val gitError = GitErrors.matchError("error: Sparse checkout leaves no entry on working directory")
        Assert.assertEquals(gitError, GitErrors.SparseCheckoutLeavesNoEntry)
    }

    @Test
    fun branchOrPathNameConflicts() {
        val gitError = GitErrors.matchError("fatal: 'test' 既可以是一个本地文件，也可以是一个跟踪分支。")
        Assert.assertEquals(gitError, GitErrors.BranchOrPathNameConflicts)
    }

    @Test
    fun mergeConflicts() {
        var gitError = GitErrors.matchError("自动合并失败，修正冲突然后提交修正的结果。")
        Assert.assertEquals(gitError, GitErrors.MergeConflicts)

        gitError = GitErrors.matchError("Automatic merge failed; fix conflicts and then commit the result.")
        Assert.assertEquals(gitError, GitErrors.MergeConflicts)

        gitError = GitErrors.matchError("Automatic merge failed; fix conflicts and then commit the result.")
        Assert.assertEquals(gitError, GitErrors.MergeConflicts)
    }

    @Test
    fun invalidMergeTest() {
        var gitError = GitErrors.matchError("merge：aaa - 不能合并")
        Assert.assertEquals(gitError, GitErrors.InvalidMerge)

        gitError = GitErrors.matchError("fatal: origin/20221130 - not something we can merge")
        Assert.assertEquals(gitError, GitErrors.InvalidMerge)
    }

    @Test
    fun cannotMergeUnrelatedHistories() {
        var gitError = GitErrors.matchError("fatal: refusing to merge unrelated histories")
        Assert.assertEquals(gitError, GitErrors.CannotMergeUnrelatedHistories)

        gitError = GitErrors.matchError("fatal: 拒绝合并无关的历史")
        Assert.assertEquals(gitError, GitErrors.CannotMergeUnrelatedHistories)
    }

    @Test
    fun localChangesOverwritten() {
        var gitError = GitErrors.matchError(
            "error: Your local changes to the following files would be overwritten by checkout:"
        )
        Assert.assertEquals(gitError, GitErrors.LocalChangesOverwritten)

        gitError = GitErrors.matchError(
            "error: 您对下列文件的本地修改将被检出操作覆盖："
        )
        Assert.assertEquals(gitError, GitErrors.LocalChangesOverwritten)
    }

    @Test
    fun noSubmoduleMapping() {
        var gitError = GitErrors.matchError(
            "fatal: 在 .gitmodules 中未找到子模组 'aaaa' 的 url"
        )
        Assert.assertEquals(gitError, GitErrors.NoSubmoduleMapping)

        gitError = GitErrors.matchError(
            "fatal: No url found for submodule path 'aaaa' in .gitmodules"
        )
        Assert.assertEquals(gitError, GitErrors.NoSubmoduleMapping)

        gitError = GitErrors.matchError(
            "No url found for submodule path 'aaaa' in .gitmodules"
        )
        Assert.assertEquals(gitError, GitErrors.NoSubmoduleMapping)
    }

    @Test
    fun submoduleRepositoryDoesNotExist() {
        val gitError = GitErrors.matchError(
            "Clone of 'http://example.com/demo.git' into submodule path 'demo' failed"
        )
        Assert.assertEquals(gitError, GitErrors.SubmoduleRepositoryDoesNotExist)
    }

    @Test
    fun invalidSubmoduleSHA() {
        val gitError = GitErrors.matchError(
            "无法在子模组路径 'aaa' 中找到当前版本"
        )
        Assert.assertEquals(gitError, GitErrors.InvalidSubmoduleSHA)
    }

    @Test
    fun lfsNotInstall() {
        var gitError = GitErrors.matchError(
            "git: 'lfs' is not a git command. See 'git --help'."
        )
        Assert.assertEquals(gitError, GitErrors.LfsNotInstall)

        gitError = GitErrors.matchError(
            "git：'lfs' 不是一个 git 命令。参见 'git --help'。"
        )
        Assert.assertEquals(gitError, GitErrors.LfsNotInstall)
    }

    @Test
    fun lockFileAlreadyExists() {
        var gitError = GitErrors.matchError(
            "error: could not lock config file .git/config: File exists"
        )
        Assert.assertEquals(gitError, GitErrors.LockFileAlreadyExists)
        gitError = GitErrors.matchError(
            "error: could not lock config file /usr/local/app/.gitconfig: 文件已存在"
        )
        Assert.assertEquals(gitError, GitErrors.LockFileAlreadyExists)
    }

    @Test
    fun notAGitRepository() {
        val gitError = GitErrors.matchError(
            "fatal: not a git repository (or any of the parent directories): .git"
        )
        Assert.assertEquals(gitError, GitErrors.NotAGitRepository)
    }

    @Test
    fun gitNotInstall() {
        var gitError = GitErrors.matchError(
            "Cannot run program \"git\" (in directory \"C:\\work\"): CreateProcess error=2, 系统找不到指定的文件。"
        )
        Assert.assertEquals(gitError, GitErrors.GitNotInstall)
        gitError = GitErrors.matchError(
            "Cannot run program \"git\" (in directory \"/data/landun/workspace\"): error=2, 没有那个文件或目录"
        )
        Assert.assertEquals(gitError, GitErrors.GitNotInstall)
    }


    @Test
    fun invalidRefSpec() {
        val gitError = GitErrors.matchError(
            "fatal: 无效的引用规格：'+refs/heads/ master:refs/remotes/origin/ master'"
        )
        Assert.assertEquals(gitError, GitErrors.InvalidRefSpec)
    }
}
