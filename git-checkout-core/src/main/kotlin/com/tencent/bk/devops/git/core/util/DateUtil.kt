package com.tencent.bk.devops.git.core.util

import org.apache.commons.lang3.time.DateUtils
import java.text.SimpleDateFormat
import java.util.Date

object DateUtil {

    private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")

    fun format(timestamp: Long, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        return SimpleDateFormat(pattern).format(Date(timestamp))
    }

    fun addDay(timestamp: Long, amount: Int): String {
        return formatter.format(DateUtils.addDays(Date(timestamp), amount))
    }
}
