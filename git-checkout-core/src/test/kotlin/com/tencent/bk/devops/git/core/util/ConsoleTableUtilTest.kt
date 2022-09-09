package com.tencent.bk.devops.git.core.util

import org.junit.Test

class ConsoleTableUtilTest {

    @Test
    fun printAsTable() {
        ConsoleTableUtil.printAsTable(
            errMsg = "1.请检查凭证【dfdfd】配置的公私钥是否正确\n" +
                "  2.如果私钥正确，则检查ssh网络是否能通\n" +
                "  3.检查私钥身份是否正确,在流水线增加bash插件，执行ssh -T host(如git@github.com)判断ssh私钥身份",
            cause = "1.请检查凭证【dfdfd】配置的公私钥是否正确\n" +
                "  2.如果私钥正确，则检查ssh网络是否能通\n" +
                "  3.检查私钥身份是否正确,在流水线增加bash插件，执行ssh -T host(如git@github.com)判断ssh私钥身份",
            solution = "1. 如果是变量没有设置，设置变量重新执行\n" +
                "  2. 如果配置的是事件触发的变量，需要兼容手动触发的情况，需要把事件触发的变量设置成流水线启动变量，" +
                "这样手动触发直接指定变量就可以。",
            wiki = ""
        )
    }
}
