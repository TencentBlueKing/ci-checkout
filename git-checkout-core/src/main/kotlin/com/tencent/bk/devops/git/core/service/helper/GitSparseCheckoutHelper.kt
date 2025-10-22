package com.tencent.bk.devops.git.core.service.helper

import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.enums.GitConfigScope
import com.tencent.bk.devops.git.core.pojo.CheckoutInfo
import com.tencent.bk.devops.git.core.pojo.GitSourceSettings
import com.tencent.bk.devops.git.core.service.GitCommandManager
import org.slf4j.LoggerFactory
import java.io.File

class GitSparseCheckoutHelper constructor(
    private val settings: GitSourceSettings,
    private val git: GitCommandManager
) {
    fun init(checkoutInfo: CheckoutInfo) {
        if (useConeMode()) {
            initSparseCone()
        } else {
            initSparseCheckout(checkoutInfo)
        }
    }
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
        git.config(
            configKey = SPARSE_CHECKOUT_CONE_CONFIG_KEY,
            configValue = "false",
            configScope = GitConfigScope.WORKTREE
        )
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


    /**
     * 初始化 sparse cone
     */
    private fun initSparseCone() {
        val includePaths = settings.includeSubPath?.split(",") ?: listOf()
        val conePaths = if (includePaths.isEmpty()) {
            // 拉取路径为空，则默认拉取根目录除目录以外的内容（若submodule在根目录，则拉取根目录submodule）
            listOf()
        } else {
            includePaths.map {
                // 包含通配符，尝试获取通配符的前缀路径
                // 例：/a/*_1/b/*_2 → /a
                if (it.contains(Regex("[?*\\[\\]]"))) {
                    extractRealPath(it)
                } else {
                    it
                }
            }.map {
                it.removePrefix("/")
            }.let {
                integratePaths(it)
            }.filter {
                it.isNotBlank()
            }
        }
        git.sparseCheckoutInit(true)
        git.sparseCheckoutSet(conePaths)
        git.sparseCheckoutList()
    }

    /**
     * 提取父级目录
     */
    private fun integratePaths(paths: List<String>): List<String> {
        // 标准化路径，确保统一以斜杠结尾，便于比较
        val normalizedPaths = paths
                .map {
                    if (it.contains('*') ||it.contains('?')) {
                        extractRealPath(it)
                    } else {
                        it
                    }
                }
                .map { path -> if (path.endsWith("/")) path else "$path/" }
                .toSet() // 去重
        // 筛选出所有不是其他路径子路径的路径
        val filtered = normalizedPaths.filter { path ->
            normalizedPaths.none { otherPath ->
                // 排除自身比较，检查当前路径是否是其他路径的子路径
                path != otherPath && path.startsWith(otherPath)
            }
        }
        // 还原路径格式（去掉我们添加的斜杠，除非原路径就有）
        return filtered.map { filteredPath ->
            paths.find { original ->
                // 处理原始路径是否以斜杠结尾的两种情况
                if (original.endsWith("/")) {
                    filteredPath == original
                } else {
                    filteredPath == "$original/"
                }
            } ?: filteredPath.removeSuffix("/")
        }.sorted() // 排序便于查看
    }

    /**
     * 提取模糊路径中通配符(*)之前的真实基础路径
     */
    fun extractRealPath(patternPath: String): String {
        // 提取通配符之前的部分（无通配符则取全部）
        val prefix = patternPath.indexOfFirst { SPARSE_CHECKOUT_WILDCARDS.contains(it) }
        // 截取到最后一个分隔符并确保以斜杠结尾
        val preFixPath = patternPath.substring(0, prefix)
                .takeIf { it.isNotEmpty() } ?: ""
        return if (preFixPath.isNotBlank()) {
            val separatorIndex = preFixPath.lastIndexOf("/")
            preFixPath.substring(0, separatorIndex)
        } else ""
    }

    fun useConeMode() = settings.enableSparseCone == true &&
            git.isAtLeastVersion(GitConstants.SUPPORT_SPARSE_CHECKOUT_GIT_VERSION)

    companion object {
        private val logger = LoggerFactory.getLogger(GitSparseCheckoutHelper::class.java)
        private const val SPARSE_CHECKOUT_CONFIG_KEY = "core.sparsecheckout"
        private const val SPARSE_CHECKOUT_CONE_CONFIG_KEY = "core.sparseCheckoutCone"
        private const val SPARSE_CHECKOUT_CONFIG_FILE_PATH = ".git/info/sparse-checkout"
        private val SPARSE_CHECKOUT_WILDCARDS = listOf('*', '?', '[')
    }
}
