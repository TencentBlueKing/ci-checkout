package com.tencent.bk.devops.git.core.service.helper

import com.tencent.bk.devops.git.core.service.helper.PlaceholderResolver.Companion.defaultResolver
import org.junit.Assert
import org.junit.Test
import java.util.Properties

class PlaceholderResolverTest {

    @Test
    fun resolve() {
        var template = "category:\${}:product:\${}"
        Assert.assertEquals("category:1:product:2", defaultResolver.resolve(template, "1", "2"))
        template = "category:product"
        Assert.assertEquals("category:product", defaultResolver.resolve(template))
    }

    @Test
    fun resolveByMap() {
        var valuesMap = mapOf("categoryId" to "1", "productId" to "2")
        val template = "category:\${categoryId}:product:\${productId}"
        Assert.assertEquals("category:1:product:2", defaultResolver.resolveByMap(template, valuesMap))

        valuesMap = mapOf("categoryId" to "1")
        Assert.assertEquals("category:1:product:", defaultResolver.resolveByMap(template, valuesMap))
    }

    @Test
    fun resolveByProperties() {
        val properties = Properties()
        properties.setProperty("categoryId", "1")
        properties.setProperty("productId", "2")
        val template = "category:\${categoryId}:product:\${productId}"

        Assert.assertEquals("category:1:product:2", defaultResolver.resolveByProperties(template, properties))
    }
}
