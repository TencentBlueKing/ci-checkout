package com.tencent.bk.devops.git.credential.storage

import com.microsoft.alm.secret.Secret
import com.tencent.bk.devops.git.credential.Constants
import java.net.URI

class DevopsUriNameConversion : Secret.IUriNameConversion {
    override fun convert(targetUri: URI, namespace: String): String {
        val builder = StringBuilder(namespace)
        val pipelineId = System.getenv(Constants.BK_CI_PIPELINE_ID)
        val vmSeqId = System.getenv(Constants.BK_CI_BUILD_JOB_ID)
        val finalPath = targetUri.getFinalPath()
        if (!pipelineId.isNullOrBlank()) {
            builder.append(":").append(pipelineId)
        }
        if (!vmSeqId.isNullOrBlank()) {
            builder.append(":").append(vmSeqId)
        }
        if (!finalPath.isNullOrBlank()) {
            builder.append(":").append(finalPath)
        }
        return Secret.uriToName(targetUri, builder.toString())
    }

    /**
     * 获取有效路径
     */
    private fun URI.getFinalPath() = if (!path.isNullOrBlank() && path != "/") {
        path
    } else {
        ""
    }
}
