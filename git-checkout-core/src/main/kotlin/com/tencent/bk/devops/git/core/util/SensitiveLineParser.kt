package com.tencent.bk.devops.git.core.util

import java.util.regex.Pattern

object SensitiveLineParser {
    private val pattern = Pattern.compile("oauth2:(\\w+)@")
    private val patternPassword = Pattern.compile("http[s]?://.*:.*@")
    private val patternCredentialPassword = Pattern.compile("password=.*")

    @SuppressWarnings("ReturnCount")
    fun onParseLine(line: String): String {
        return when {
            line.contains("http://oauth2:") || line.contains("https://oauth2:") -> {
                return pattern.matcher(line).replaceAll("oauth2:***@")
            }
            line.contains("http://") || line.contains("https://") -> {
                patternPassword.matcher(line).replaceAll("http://***:***@")
            }
            line.contains("password=") -> {
                patternCredentialPassword.matcher(line).replaceAll("password=***")
            }
            else ->
                line
        }
    }
}
