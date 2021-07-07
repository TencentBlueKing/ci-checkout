package com.tencent.bk.devops.git.core.service.helper

object VersionHelper {

    fun getCheckoutCoreVersion(): String {
        val pack = Package.getPackage("com.tencent.bk.devops.git.core")
        val implementationVersion = pack.implementationVersion
        val specificationVersion = pack.specificationVersion
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
