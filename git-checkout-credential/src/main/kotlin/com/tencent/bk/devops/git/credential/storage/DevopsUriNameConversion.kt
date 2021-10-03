package com.tencent.bk.devops.git.credential.storage

import com.microsoft.alm.secret.Secret
import com.tencent.bk.devops.git.credential.Constants
import java.net.URI

class DevopsUriNameConversion : Secret.IUriNameConversion {
    override fun convert(targetUri: URI, namespace: String): String {
        val builder = StringBuilder(namespace)
        val buildId = System.getenv(Constants.BK_CI_PIPELINE_ID)
        val vmSeqId = System.getenv(Constants.BK_CI_BUILD_JOB_ID)
        if (!buildId.isNullOrBlank()) {
            builder.append(":").append(buildId)
        }
        if (!vmSeqId.isNullOrBlank()) {
            builder.append(":").append(vmSeqId)
        }
        return Secret.uriToName(targetUri, builder.toString())
    }
}
