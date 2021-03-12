package com.tencent.devops

import com.tencent.bk.devops.atom.AtomContext
import com.tencent.bk.devops.atom.common.Status
import com.tencent.bk.devops.atom.pojo.StringData
import com.tencent.bk.devops.atom.spi.AtomService
import com.tencent.bk.devops.atom.spi.TaskAtom
import com.tencent.bk.devops.atom.utils.json.JsonUtil
import com.tencent.devops.pojo.GitCodeAtomParam
import com.tencent.devops.scm.CodeGitPullCodeSetting
import com.tencent.devops.scm.IPullCodeSetting
import com.tencent.devops.utils.shell.CommonShellUtils
import org.slf4j.LoggerFactory

@AtomService(paramClass = GitCodeAtomParam::class)
class GitCodeAtom : TaskAtom<GitCodeAtomParam> {

    override fun execute(atomContext: AtomContext<GitCodeAtomParam>) {
        try {
            showEnvVariable()
            val param = atomContext.param
            logger.info("context param: ${JsonUtil.toJson(param)}")
            val env = getPullCodeSetting(param).pullCode()
            env?.forEach { t, u -> atomContext.result.data[t] = StringData(u) }

            // 添加代码库信息支持codecc扫描
            if (param.noScmVariable == true) {
                return
            }

            atomContext.result.data["bk_repo_taskId_${param.pipelineTaskId}"] = StringData(param.pipelineTaskId ?: "")
            atomContext.result.data["bk_repo_type_${param.pipelineTaskId}"] = StringData("GIT")
            atomContext.result.data["bk_repo_local_path_${param.pipelineTaskId}"] = StringData(param.localPath ?: "")
            atomContext.result.data["bk_repo_code_url_${param.pipelineTaskId}"] = StringData(param.repositoryUrl)
            atomContext.result.data["bk_repo_auth_type_${param.pipelineTaskId}"] = StringData(getAuthType(param))
            atomContext.result.data["bk_repo_container_id_${param.pipelineTaskId}"] = StringData(
                atomContext.allParameters["pipeline.job.id"]?.toString() ?: ""
            )
        } catch (e: Exception) {
            atomContext.result.message = e.message
            atomContext.result.status = Status.failure
        }
    }

    private fun getAuthType(param: GitCodeAtomParam): String {
        if (!param.accessToken.isNullOrBlank()) return "OAUTH"
        if (!param.username.isNullOrBlank()) return "HTTP"
        if (param.repositoryUrl.toUpperCase().startsWith("HTTP")) return "HTTP"
        return "SSH"
    }

    private fun showEnvVariable() {
        CommonShellUtils.execute("whoami")
        CommonShellUtils.execute(script = "git version", failExit = true)
    }

    private fun getPullCodeSetting(
        params: GitCodeAtomParam
    ): IPullCodeSetting {
        return CodeGitPullCodeSetting(params)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GitCodeAtom::class.java)
    }
}
