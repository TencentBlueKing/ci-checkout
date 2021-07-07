package com.tencent.bk.devops.git.core.service.helper

object VersionHelper {

    fun getCheckoutCoreVersion(): String {
        val implementationVersion = javaClass.`package`.implementationVersion
        val specificationVersion = javaClass.`package`.specificationVersion
        val version = StringBuffer()
        if (specificationVersion != null) {
            version.append(specificationVersion).append(".")
        }
        if (implementationVersion != null) {
            version.append(implementationVersion)
        }
        return version.toString()
    }
}
