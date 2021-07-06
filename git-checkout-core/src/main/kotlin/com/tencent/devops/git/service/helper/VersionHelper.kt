package com.tencent.devops.git.service.helper

import java.util.jar.Attributes
import java.util.jar.JarInputStream

object VersionHelper {

    fun getCheckoutCoreVersion(): String {
        val manifest = javaClass.classLoader.getResourceAsStream("META-INF/MANIFEST.MF")?.use { mf ->
            JarInputStream(mf, false).use { jar ->
                jar.manifest
            }
        } ?: return ""
        val implVersion = manifest.mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION)
        val specVersion = manifest.mainAttributes.getValue(Attributes.Name.SPECIFICATION_VERSION)
        return "$implVersion.$specVersion"
    }
}
