package com.tencent.bk.devops.git.core.service.helper

import com.tencent.bk.devops.git.core.pojo.CheckoutInfo
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import org.slf4j.LoggerFactory
import java.io.File

class GitSparseCheckoutHelper constructor(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) {
    /**
     * sparse checkout
     */
    @SuppressWarnings("NestedBlockDepth")
    fun initSparseCheckout(checkoutInfo: CheckoutInfo) = with(settings) {
        val sparseFile = File(repositoryPath, SPARSE_CHECKOUT_CONFIG_FILE_PATH)
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
        logger.debug("$SPARSE_CHECKOUT_CONFIG_FILE_PATH content: $content")

        if (content.toString().isBlank()) {
            /*
                #24 如果由sparse checkout改成正常拉取,需要把内容设置为*, 不然执行`git checkout`文件内容不会发生改变.
                参考: https://ftp.mcs.anl.gov/pub/pdetools/nightlylogs/xsdk/xsdk-configuration
                -tester/packages/trilinos/sparse_checkout.sh
             */
            if (sparseFile.exists()) {
                sparseFile.writeText("*")
                git.config(configKey = SPARSE_CHECKOUT_CONFIG_KEY, configValue = "true")
                if (checkoutInfo.startPoint.isBlank()) {
                    git.readTree(options = listOf("--reset", "-u", checkoutInfo.ref))
                } else {
                    git.readTree(options = listOf("--reset", "-u", checkoutInfo.startPoint))
                }

                sparseFile.delete()
            }
            git.config(configKey = SPARSE_CHECKOUT_CONFIG_KEY, configValue = "false")
        } else {
            if (!sparseFile.parentFile.exists()) sparseFile.parentFile.mkdirs()
            if (!sparseFile.exists()) sparseFile.createNewFile()
            sparseFile.writeText(content.toString())
            git.config(configKey = SPARSE_CHECKOUT_CONFIG_KEY, configValue = "true")
            if (checkoutInfo.startPoint.isBlank()) {
                git.readTree(options = listOf("-m", "-u", checkoutInfo.ref))
            } else {
                git.readTree(options = listOf("-m", "-u", checkoutInfo.startPoint))
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GitSparseCheckoutHelper::class.java)
        private const val SPARSE_CHECKOUT_CONFIG_KEY = "core.sparsecheckout"
        private const val SPARSE_CHECKOUT_CONFIG_FILE_PATH = ".git/info/sparse-checkout"
    }
}
