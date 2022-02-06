package com.tencent.bk.devops.git.core.service.handler

import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import com.tencent.bk.devops.git.core.service.helper.RefHelper
import java.io.File

class GitSparseCheckoutHandler(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) : IGitHandler {

    private val refHelper = RefHelper(settings = settings)

    override fun doHandle() {
        with(settings) {
            initSparseCheckout()
        }
    }

    /**
     * sparse checkout
     */
    private fun GitSourceSettings.initSparseCheckout() {
        val sparseFile = File(repositoryPath, ".git/info/sparse-checkout")
        val content = StringBuilder()
        if (!excludeSubPath.isNullOrBlank()) {
            content.append("/*").append(System.lineSeparator())
            excludeSubPath!!.split(",").forEach {
                content.append("!").append(it.trim()).append(System.lineSeparator())
            }
        }
        if (!includeSubPath.isNullOrBlank()) {
            includeSubPath!!.split(",").forEach {
                content.append("/").append(it.trim().removePrefix("/")).append(System.lineSeparator())
            }
        }

        val checkInfo = refHelper.getCheckInfo()
        if (content.toString().isBlank()) {
            /*
                #24 如果由sparse checkout改成正常拉取,需要把内容设置为*, 不然执行`git checkout`文件内容不会发生改变.
                参考: https://ftp.mcs.anl.gov/pub/pdetools/nightlylogs/xsdk/xsdk-configuration
                -tester/packages/trilinos/sparse_checkout.sh
             */
            if (sparseFile.exists()) {
                sparseFile.writeText("*")
                git.config(configKey = "core.sparsecheckout", configValue = "true")
                git.readTree(options = listOf("--reset", "-u", checkInfo.startPoint))
                sparseFile.delete()
            }
            git.config(configKey = "core.sparsecheckout", configValue = "false")
        } else {
            if (!sparseFile.parentFile.exists()) sparseFile.parentFile.mkdirs()
            if (!sparseFile.exists()) sparseFile.createNewFile()
            sparseFile.writeText(content.toString())
            git.config(configKey = "core.sparsecheckout", configValue = "true")
            git.readTree(options = listOf("-m", "-u", checkInfo.startPoint))
        }
    }
}
