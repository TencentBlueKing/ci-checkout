package com.tencent.bk.devops.git.core.service.helper

import java.util.Properties

/**
 * 占位符解析器
 */
class PlaceholderResolver {
    /**
     * 占位符前缀
     */
    private var placeholderPrefix = DEFAULT_PLACEHOLDER_PREFIX

    /**
     * 占位符后缀
     */
    private var placeholderSuffix = DEFAULT_PLACEHOLDER_SUFFIX

    private constructor()
    private constructor(placeholderPrefix: String, placeholderSuffix: String) {
        this.placeholderPrefix = placeholderPrefix
        this.placeholderSuffix = placeholderSuffix
    }

    /**
     * 解析带有指定占位符的模板字符串，默认占位符为前缀：${  后缀：}<br></br><br></br>
     * 如：template = category:${}:product:${}<br></br>
     * values = {"1", "2"}<br></br>
     * 返回 category:1:product:2<br></br>
     *
     * @param content 要解析的带有占位符的模板字符串
     * @param values   按照模板占位符索引位置设置对应的值
     * @return
     */
    fun resolve(content: String, vararg values: String): String {
        var start = content.indexOf(placeholderPrefix)
        if (start == -1) {
            return content
        }
        // 值索引
        var valueIndex = 0
        val result = StringBuilder(content)
        while (start != -1) {
            val end = result.indexOf(placeholderSuffix)
            val replaceContent = values[valueIndex++]
            result.replace(start, end + placeholderSuffix.length, replaceContent)
            start = result.indexOf(placeholderPrefix, start + replaceContent.length)
        }
        return result.toString()
    }

    /**
     * 根据替换规则来替换指定模板中的占位符值
     * @param content  要解析的字符串
     * @param rule  解析规则回调
     * @return
     */
    fun resolveByRule(content: String, rule: (String) -> String): String {
        var start = content.indexOf(placeholderPrefix)
        if (start == -1) {
            return content
        }
        val result = StringBuilder(content)
        while (start != -1) {
            val end = result.indexOf(placeholderSuffix, start)
            // 获取占位符属性值，如${id}, 即获取id
            val placeholder = result.substring(start + placeholderPrefix.length, end)
            // 替换整个占位符内容，即将${id}值替换为替换规则回调中的内容
            val replaceContent = if (placeholder.trim { it <= ' ' }.isEmpty()) "" else rule.invoke(placeholder)
            result.replace(start, end + placeholderSuffix.length, replaceContent)
            start = result.indexOf(placeholderPrefix, start + replaceContent.length)
        }
        return result.toString()
    }

    /**
     * 替换模板中占位符内容，占位符的内容即为map key对应的值，key为占位符中的内容。<br></br><br></br>
     * 如：content = product:${id}:detail:${did}<br></br>
     * valueMap = id -> 1; pid -> 2<br></br>
     * 经过解析返回 product:1:detail:2<br></br>
     *
     * @param content  模板内容。
     * @param valueMap 值映射
     * @return 替换完成后的字符串。
     */
    fun resolveByMap(content: String, valueMap: Map<String, Any?>): String {
        return resolveByRule(content) { placeholderValue ->
            valueMap[placeholderValue]?.toString() ?: ""
        }
    }

    /**
     * 根据properties文件替换占位符内容
     * @param content
     * @param properties
     * @return
     */
    fun resolveByProperties(content: String, properties: Properties): String {
        return resolveByRule(content) { placeholderValue ->
            properties.getProperty(
                placeholderValue
            )
        }
    }

    companion object {
        /**
         * 默认前缀占位符
         */
        const val DEFAULT_PLACEHOLDER_PREFIX = "\${"

        /**
         * 默认后缀占位符
         */
        const val DEFAULT_PLACEHOLDER_SUFFIX = "}"
        /**
         * 获取默认的占位符解析器，即占位符前缀为"${", 后缀为"}"
         * @return
         */
        /**
         * 默认单例解析器
         */
        val defaultResolver = PlaceholderResolver()

        fun getResolver(placeholderPrefix: String, placeholderSuffix: String): PlaceholderResolver {
            return PlaceholderResolver(placeholderPrefix, placeholderSuffix)
        }
    }
}
