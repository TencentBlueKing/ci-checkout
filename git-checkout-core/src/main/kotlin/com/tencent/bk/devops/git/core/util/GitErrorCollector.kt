package com.tencent.bk.devops.git.core.util

import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_SUBMODULE_UPDATING
import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_SUBMODULE_URL
import com.tencent.bk.devops.git.core.enums.GitErrors

/**
 * 收集一条git命令输出中的错误
 *
 * 标准输出和错误流由两个线程分别读取,所以方法需要同步
 */
class GitErrorCollector {

    private var gitErrors: GitErrors? = null
    private var errorLine: String? = null
    private val failedSubmoduleUrls = LinkedHashSet<String>()

    @Synchronized
    fun collect(line: String, fromStdOut: Boolean) {
        if (!fromStdOut && errorLine == null && ERROR_LINE_REGEX.containsMatchIn(line)) {
            errorLine = SensitiveLineParser.onParseLine(line.trim())
        }
        if (!fromStdOut) {
            SUBMODULE_CLONE_FAILED_REGEX.find(GitErrorNormalizer.normalize(line))?.let {
                failedSubmoduleUrls.add(URL_USER_INFO_REGEX.replace(it.groupValues[1], "$1"))
            }
        }
        val matched = GitErrors.matchError(line, fromStdOut) ?: return
        val current = gitErrors
        if (current == null || matched.priority > current.priority) {
            gitErrors = matched
        }
    }

    /**
     * 主仓库拉取成功后才会拉取子模块,所以拉取子模块时的仓库不存在、认证失败都是子模块仓库的错误
     *
     * 克隆失败的子模块地址在这里写入上下文供错误信息使用,collect在读取输出流的子线程中执行,不能在那里写
     */
    @Synchronized
    fun getGitErrors(): GitErrors? {
        if (failedSubmoduleUrls.isNotEmpty()) {
            EnvHelper.putContext(CONTEXT_SUBMODULE_URL, failedSubmoduleUrls.joinToString(", "))
        }
        val errors = gitErrors
        val submoduleUpdating = EnvHelper.getContext(CONTEXT_SUBMODULE_UPDATING) == "true"
        if (submoduleUpdating &&
            (errors == GitErrors.AuthenticationFailed || errors == GitErrors.RepositoryNotFoundFailed)
        ) {
            return GitErrors.SubmoduleRepositoryDoesNotExist
        }
        return errors
    }

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
        private val SUBMODULE_CLONE_FAILED_REGEX = Regex(
            pattern = "(?:clone of|无法克隆) '([^']+)' (?:into submodule path|到子模组路径) '",
            option = RegexOption.IGNORE_CASE
        )
        private val URL_USER_INFO_REGEX = Regex("^(https?://)[^/@]+@", RegexOption.IGNORE_CASE)
    }
}
