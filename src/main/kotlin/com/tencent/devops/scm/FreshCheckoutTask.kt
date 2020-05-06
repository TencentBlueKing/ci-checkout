package com.tencent.devops.scm

import com.tencent.devops.pojo.GitCodeAtomParam

class FreshCheckoutTask(
    override val atomParam: GitCodeAtomParam,
    override val credentialSetter: GitCredentialSetter
) : GitUpdateTask(
    atomParam,
    credentialSetter
) {

    override fun preUpdate() {
        cleanupWorkspace()
    }
}
