package com.tencent.bk.devops.git.core.service.helper

@SuppressWarnings("MagicNumber")
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

    fun computeGitVersion(version: String): Long {
        var gitMajorVersion = 0
        var gitMinorVersion = 0
        var gitRevVersion = 0
        var gitBugfixVersion = 0
        try {
            /*
             * msysgit adds one more term to the version number. So
             * instead of Major.Minor.Rev.Bugfix, it displays
             * something like Major.Minor.Rev.msysgit.BugFix. This
             * removes the inserted term from the version string
             * before parsing.
             * git 2.5.0 for windows adds a similar component with
             * the string "windows".  Remove it as well
             */
            val fields = version.split(" ".toRegex()).toTypedArray()[2].replace("msysgit.", "")
                .replace("windows.", "")
                .split("\\.".toRegex()).toTypedArray()
            gitMajorVersion = fields[0].toInt()
            gitMinorVersion = if (fields.size > 1) fields[1].toInt() else 0
            gitRevVersion = if (fields.size > 2) fields[2].toInt() else 0
            gitBugfixVersion = if (fields.size > 3) fields[3].toInt() else 0
        } catch (ignore: Throwable) {
            /* Oh well */
        }
        return computeVersionFromBits(gitMajorVersion, gitMinorVersion, gitRevVersion, gitBugfixVersion)
    }

    // AABBCCDD where AA=major, BB=minor, CC=rev, DD=bugfix
    private fun computeVersionFromBits(major: Int, minor: Int, rev: Int, bugfix: Int): Long {
        return major * 1000000L + minor * 10000L + rev * 100L + bugfix
    }

    fun isAtLeastVersion(gitVersion: Long, requestedVersion: Long): Boolean {
        return gitVersion >= requestedVersion
    }
}
