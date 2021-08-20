package com.tencent.bk.devops.git.core.util

import org.junit.Assert
import org.junit.Test

class DateUtilTest {

    @Test
    fun timestampToZoneDate() {
        Assert.assertEquals(
            DateUtil.timestampToZoneDate(1629202335000L),
            "2021-08-17T20:12:15+0800"
        )
    }

    @Test
    fun addDay() {
        Assert.assertEquals(
            DateUtil.addDay(1629202335000L, -1),
            "2021-08-16T20:12:15+0800"
        )
    }
}
