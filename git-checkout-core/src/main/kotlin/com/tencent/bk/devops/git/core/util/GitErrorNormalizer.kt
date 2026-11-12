package com.tencent.bk.devops.git.core.util

/**
 * 把git输出归一化后再做错误匹配,只用于匹配,不影响日志输出
 *
 * 1. 全角标点转半角
 * 2. 合并连续空白
 * 3. 行首的中文前缀(致命错误、错误、远程错误)统一转成英文
 */
object GitErrorNormalizer {

    private val PUNCTUATION = mapOf(
        '：' to ':',
        '（' to '(',
        '）' to ')',
        '。' to '.',
        '，' to ',',
        '；' to ';',
        '！' to '!',
        '？' to '?',
        '‘' to '\'',
        '’' to '\'',
        '“' to '"',
        '”' to '"',
        '\u3000' to ' '
    )

    private val PREFIX_ALIASES = mapOf(
        "致命错误" to "fatal",
        "错误" to "error",
        "远程错误" to "remote error"
    )

    private val PREFIX_REGEX = Regex(
        pattern = "^(fatal|error|remote error|remote|致命错误|错误|远程错误)\\s*:\\s*",
        option = RegexOption.IGNORE_CASE
    )

    private val SPACES_REGEX = Regex("\\s+")

    fun normalize(line: String): String {
        val replaced = buildString(line.length) {
            line.forEach { append(PUNCTUATION[it] ?: it) }
        }
        var rest = SPACES_REGEX.replace(replaced, " ").trim()
        val prefixes = StringBuilder()
        while (true) {
            val matchResult = PREFIX_REGEX.find(rest) ?: break
            val prefix = matchResult.groupValues[1].lowercase()
            prefixes.append(PREFIX_ALIASES[prefix] ?: prefix).append(": ")
            rest = rest.substring(matchResult.range.last + 1)
        }
        return prefixes.append(rest).toString()
    }
}
