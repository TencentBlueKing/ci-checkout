package com.tencent.devops.scm

import com.tencent.devops.enums.GitPullModeType
import com.tencent.devops.pojo.GitCodeAtomParam
import com.tencent.devops.utils.shell.CommonShellUtils
import java.io.File

class RevertCheckoutTask(
    override val atomParam: GitCodeAtomParam,
    override val credentialSetter: GitCredentialSetter
) : GitUpdateTask(
        atomParam,
        credentialSetter
) {

    override fun preUpdate() {
        checkLocalGitRepo()
        if (File(workspace, ".git").exists()) {
            CommonShellUtils.execute("git reset --hard", workspace)
            if (atomParam.enableGitClean) CommonShellUtils.execute("git clean -xdf", workspace)

            // 分支要reset到远程最新的
            if (atomParam.pullType == GitPullModeType.BRANCH) {
                gitFetch()
                CommonShellUtils.execute("git checkout ${atomParam.refName}", workspace)
                CommonShellUtils.execute("git reset --hard origin/${atomParam.refName}", workspace)
            }
        }
    }
}
