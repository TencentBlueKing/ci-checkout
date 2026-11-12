package com.tencent.bk.devops.git.core.util

import com.tencent.bk.devops.git.core.enums.GitErrors

/**
 * 收集一条git命令输出中的错误
 *
 * 标准输出和错误流由两个线程分别读取,所以方法需要同步
 */
class GitErrorCollector {

    private var gitErrors: GitErrors? = null
    private var errorLine: String? = null

    @Synchronized
    fun collect(line: String, fromStdOut: Boolean) {
        if (!fromStdOut && errorLine == null && ERROR_LINE_REGEX.containsMatchIn(line)) {
            errorLine = SensitiveLineParser.onParseLine(line.trim())
        }
        val matched = GitErrors.matchError(line, fromStdOut) ?: return
        val current = gitErrors
        if (current == null || matched.priority > current.priority) {
            gitErrors = matched
        }
    }

    @Synchronized
    fun getGitErrors(): GitErrors? = gitErrors

    /**
     * 错误流中第一条以fatal/error开头的行,git一般先输出根因,后面的行多是连带的报错
     */
    @Synchronized
    fun getErrorLine(): String? = errorLine

    companion object {
        private val ERROR_LINE_REGEX = Regex(
            pattern = "^\\s*(fatal|error|致命错误|错误)\\s*[:：]",
            option = RegexOption.IGNORE_CASE
        )
    }
}
