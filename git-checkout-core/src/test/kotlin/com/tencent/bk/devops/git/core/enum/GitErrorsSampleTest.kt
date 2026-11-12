package com.tencent.bk.devops.git.core.enum

import com.tencent.bk.devops.git.core.enums.GitErrors
import org.junit.Assert
import org.junit.Test

class GitErrorsSampleTest {

    @Test
    fun errorSamples() {
        val failures = readSamples("/git-errors/error-samples.txt").mapNotNull { sample ->
            val index = sample.indexOf('|')
            val expected = GitErrors.valueOf(sample.substring(0, index))
            val line = sample.substring(index + 1)
            val actual = GitErrors.matchError(line)
            if (actual != expected) "expected $expected but was $actual: $line" else null
        }
        Assert.assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun normalSamples() {
        val failures = readSamples("/git-errors/normal-samples.txt").mapNotNull { line ->
            val stdErr = GitErrors.matchError(line, fromStdOut = false)
            val stdOut = GitErrors.matchError(line, fromStdOut = true)
            if (stdErr != null || stdOut != null) "unexpected $stdErr/$stdOut: $line" else null
        }
        Assert.assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun everyErrorHasSample() {
        val covered = readSamples("/git-errors/error-samples.txt").map { it.substringBefore('|') }.toSet()
        val missing = GitErrors.values().map { it.name }.filterNot { covered.contains(it) }
        Assert.assertTrue("missing samples: $missing", missing.isEmpty())
    }

    @Test
    fun everyErrorHasText() {
        val missing = GitErrors.values().filter { it != GitErrors.MergeStashFail && it.title.isNullOrBlank() }
        Assert.assertTrue("missing title: $missing", missing.isEmpty())
    }

    @Test
    fun stdOutOnlyMatchDeclaredRules() {
        Assert.assertEquals(
            GitErrors.MergeConflicts,
            GitErrors.matchError("Automatic merge failed; fix conflicts and then commit the result.", true)
        )
        Assert.assertNull(GitErrors.matchError("fatal: Authentication failed for 'http://example.com/'", true))
    }

    @Test
    fun sameLineMatchHighestPriority() {
        Assert.assertEquals(
            GitErrors.AuthenticationFailed,
            GitErrors.matchError("error: RPC failed; HTTP 401 curl 22 The requested URL returned error: 401")
        )
    }

    private fun readSamples(path: String): List<String> {
        val stream = GitErrorsSampleTest::class.java.getResourceAsStream(path)
            ?: throw IllegalStateException("sample file $path not found")
        return stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
        }
    }
}
