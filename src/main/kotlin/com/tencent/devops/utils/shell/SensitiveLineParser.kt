package com.tencent.devops.utils.shell

import java.util.regex.Pattern
import org.slf4j.LoggerFactory

object SensitiveLineParser {
    private val pattern = Pattern.compile("oauth2:(\\w+)@")
    private val patternPassword = Pattern.compile("//.*:.*@")

    fun onParseLine(line: String): String {
        if (line.contains("//oauth2:")) {
            val matcher = pattern.matcher(line)
            val replace = matcher.replaceAll("oauth2:***@")
            logger.info("Parse the line from $line to $replace")
            return replace
        }
        if (line.contains("http://") ||
                line.contains("https://")) {
            return patternPassword.matcher(line).replaceAll("//***:***@")
        }
        return line
    }

    private val logger = LoggerFactory.getLogger(SensitiveLineParser::class.java)
}
