package com.tencent.bk.devops.git.core.util

import java.util.regex.Pattern

object SensitiveLineParser {
    private val patternCredentialPassword = Pattern.compile("password=.*")

    @SuppressWarnings("ReturnCount")
    fun onParseLine(line: String): String {
        return when {
            line.contains("password=") -> {
                patternCredentialPassword.matcher(line).replaceAll("password=***")
            }
            else ->
                line.replace(Regex("(http[s]?://)(\\w+?):(.*?@)"), "$1$2:***@")
        }
    }
}
