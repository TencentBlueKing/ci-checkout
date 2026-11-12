package com.tencent.bk.devops.git.core.util

import org.junit.Assert
import org.junit.Test

class GitErrorNormalizerTest {

    @Test
    fun normalizePrefix() {
        Assert.assertEquals(
            "fatal: remote error: Git repository not found",
            GitErrorNormalizer.normalize("致命错误：远程错误：Git repository not found")
        )
        Assert.assertEquals(
            "fatal: remote error: Git repository not found",
            GitErrorNormalizer.normalize("fatal: 远程错误：Git repository not found")
        )
        Assert.assertEquals(
            "error: RPC 失败.curl 56 Recv failure",
            GitErrorNormalizer.normalize("错误：RPC 失败。curl 56 Recv failure")
        )
        Assert.assertEquals("remote: Repository not found.", GitErrorNormalizer.normalize("remote: Repository not found."))
    }

    @Test
    fun normalizePunctuation() {
        Assert.assertEquals(
            "fatal: 不是 Git 仓库(或者任何父目录):.git",
            GitErrorNormalizer.normalize("致命错误：不是 Git 仓库（或者任何父目录）：.git")
        )
        Assert.assertEquals(
            "fatal: 'origin/aaa' 不是一个提交,无法基于它创建分支 'aaa'",
            GitErrorNormalizer.normalize("致命错误：'origin/aaa' 不是一个提交，无法基于它创建分支 'aaa'")
        )
    }

    @Test
    fun normalizeSpaces() {
        Assert.assertEquals(
            "fatal: 不能同时更新路径并切换到分支'aaa'. 您是想要检出 'origin/aaa' 但其未能解析为提交么?",
            GitErrorNormalizer.normalize("  fatal: 不能同时更新路径并切换到分支'aaa'。\n您是想要检出 'origin/aaa' 但其未能解析为提交么？  ")
        )
    }
}
