package com.tencent.bk.devops.git.core.service.helper

import java.util.jar.Attributes
import java.util.jar.Manifest

object VersionHelper {

    fun getCheckoutCoreVersion(): String {
        val manifest = javaClass.classLoader.getResourceAsStream("META-INF/MANIFEST.MF")?.use {
            Manifest(it)
        } ?: return ""
        val implVersion = manifest.mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION)
        val specVersion = manifest.mainAttributes.getValue(Attributes.Name.SPECIFICATION_VERSION)
        val version = StringBuffer()
        if (specVersion != null) {
            version.append(specVersion).append(".")
        }
        if (implVersion != null) {
            version.append(implVersion)
        }
        return version.toString()
    }
}
