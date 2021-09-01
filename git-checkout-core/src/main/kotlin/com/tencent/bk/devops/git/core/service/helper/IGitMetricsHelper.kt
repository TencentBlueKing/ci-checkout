package com.tencent.bk.devops.git.core.service.helper

import com.tencent.bk.devops.git.core.pojo.GitMetricsInfo

interface IGitMetricsHelper {
    fun reportMetrics(atomCode: String, metricsInfo: GitMetricsInfo)
}
