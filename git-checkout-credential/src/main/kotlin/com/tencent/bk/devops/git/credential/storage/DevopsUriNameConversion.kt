package com.tencent.bk.devops.git.credential.storage

import com.microsoft.alm.secret.Secret
import com.tencent.bk.devops.git.credential.Constants
import java.net.URI

class DevopsUriNameConversion : Secret.IUriNameConversion {
    override fun convert(targetUri: URI, namespace: String): String {
        val builder = StringBuilder(namespace)
        val pipelineId = System.getenv(Constants.BK_CI_PIPELINE_ID)
        val vmSeqId = System.getenv(Constants.BK_CI_BUILD_JOB_ID)
        if (!pipelineId.isNullOrBlank()) {
            builder.append(":").append(pipelineId)
        }
        if (!vmSeqId.isNullOrBlank()) {
            builder.append(":").append(vmSeqId)
        }
        return Secret.uriToName(targetUri, builder.toString())
    }
}
