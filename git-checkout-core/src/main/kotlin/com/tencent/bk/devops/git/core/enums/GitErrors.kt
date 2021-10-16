/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.devops.git.core.enums

import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.constant.GitConstants.CONTEXT_GIT_PROTOCOL
import com.tencent.bk.devops.git.core.i18n.GitErrorsText
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.plugin.pojo.ErrorType

enum class GitErrors(
    val regex: Regex,
    val description: String?,
    val errorType: ErrorType = ErrorType.USER,
    val errorCode: Int = GitConstants.CONFIG_ERROR
) {
    // fetch命令错误
    AuthenticationFailed(
        regex = Regex(
            "(The requested URL returned error: 403)|" +
                "(fatal: could not read Username for '(.+)': terminal prompts disabled)|" +
                "(fatal: Authentication failed for '(.+)')|" +
                "(fatal: Could not read from remote repository.)|" +
                "(fatal: Authentication failed)|" +
                "(fatal: '(.+) 鉴权失败')|" +
                "(fatal: 无法读取远程仓库。)" +
                "(fatal: repository '(.+)' not found)|" +
                "(fatal: .* Git repository not found)|" +
                "(fatal: 远程错误：Git repository not found)|" +
                "(ERROR: Repository not found)|" +
                "(fatal: remote error: Git:Project not found.)|" +
                "(fatal: could not read Username for '(.+)': No such device or address)|"
        ),
        description = when (EnvHelper.getContext(CONTEXT_GIT_PROTOCOL)) {
            GitProtocolEnum.SSH.name -> GitErrorsText.get().sshAuthenticationFailed
            else -> GitErrorsText.get().httpAuthenticationFailed
        }
    ),
    RemoteServerFailed(
        regex = Regex(
            "(fatal: (the|The) remote end hung up unexpectedly)|" +
                "(fatal: unable to access '(.+)': The requested URL returned error: 502)|" +
                "(fatal: 远程错误：Internal server error)|" +
                "(fatal: 远端意外挂断了)|" +
                "(Git:Server is busy, please try again later)|" +
                "(fatal: unable to access '(.+)': Failed to connect to (.+): Host is down)"
        ),
        description = GitErrorsText.get().remoteServerFailed,
        errorType = ErrorType.THIRD_PARTY,
        errorCode = GitConstants.GIT_ERROR
    ),
    ConnectionTimeOut(
        regex = Regex(
            "ssh: connect to host (.+) port (\\d+): Connection timed out"
        ),
        description = GitErrorsText.get().connectionTimeOut
    ),

    // checkout命令错误
    NoMatchingBranch(
        regex = Regex(
            pattern = "(fatal: couldn't find remote ref .+)|" +
                "(fatal: 无法找到远程引用 .+)|" +
                "(Your configuration specifies to merge with the ref '(.+)')|" +
                "(您的配置中指定要合并远程的引用 '(.+)')|" +
                "(fatal: '(.+)' is not a commit and a branch '(.+)' cannot be created from it)|" +
                "(fatal: '.+' 不是一个提交，不能基于它创建分支 '.+')|" +
                "(error: pathspec '(.+)' did not match any file\\(s\\) known to git)|" +
                "(error: 路径规格 '(.+)' 未匹配任何 git 已知文件)|" +
                "(fatal: path '(.+)' does not exist .+)|" +
                "(fatal: 路径 '(.+)' 不存在)|" +
                "(fatal: 引用不是一个树：(.+))|" +
                "(fatal: reference is not a tree: (.+))",
            options = setOf(RegexOption.IGNORE_CASE)
        ),
        description = GitErrorsText.get().noMatchingBranch
    ),
    NoInitializeBranch(
        regex = Regex(
            "(fatal: You are on a branch yet to be born)|" +
                "(fatal: 您位于一个尚未初始化的分支)"
        ),
        description = GitErrorsText.get().noInitializeBranch
    ),
    SparseCheckoutLeavesNoEntry(
        regex = Regex(
            "(error: Sparse checkout leaves no entry on working directory)"
        ),
        description = GitErrorsText.get().sparseCheckoutLeavesNoEntry
    ),
    BranchOrPathNameConflicts(
        regex = Regex(
            "(fatal: '(.+)' 既可以是一个本地文件，也可以是一个跟踪分支。)"
        ),
        description = GitErrorsText.get().branchOrPathNameConflicts
    ),

    // merge命令错误
    MergeConflicts(
        regex = Regex(
            "(Automatic merge failed; fix conflicts and then commit the result.)|" +
                "(Resolve all conflicts manually, mark them as resolved with)|" +
                "(自动合并失败，修正冲突然后提交修正的结果。)"
        ),
        description = GitErrorsText.get().mergeConflicts
    ),
    InvalidMerge(
        regex = Regex(
            "(merge: (.+) - not something we can merge)|" +
                "(merge：.+ - 不能合并)"
        ),
        description = GitErrorsText.get().invalidMerge
    ),
    CannotMergeUnrelatedHistories(
        regex = Regex(
            "(fatal: refusing to merge unrelated histories)|" +
                "(fatal: 拒绝合并无关的历史)"
        ),
        description = GitErrorsText.get().cannotMergeUnrelatedHistories
    ),
    LocalChangesOverwritten(
        regex = Regex(
            "(error: Your local changes to the following files would be overwritten by merge:)|" +
                "(error: Your local changes to the following files would be overwritten by checkout:)|" +
                "(error: 您对下列文件的本地修改将被检出操作覆盖：)"
        ),
        description = GitErrorsText.get().localChangesOverwritten
    ),

    // submodule命令错误
    NoSubmoduleMapping(
        regex = Regex(
            "(fatal: No submodule mapping found in .gitmodules for path '(.+)')|" +
                "(fatal: 在 .gitmodules 中没有发现路径 '(.+)' 的子模组映射)|" +
                "(fatal: 在 .gitmodules 中未找到子模组 '(.+)' 的 url)|" +
                "(fatal: No url found for submodule path '(.+)' in .gitmodules)"
        ),
        description = GitErrorsText.get().noSubmoduleMapping
    ),
    SubmoduleRepositoryDoesNotExist(
        regex = Regex(
            pattern = "(clone of '.+' into submodule path '(.+)' failed)|" +
                "(fatal：无法克隆 '(.+)' 到子模组路径 '(.+)')",
            options = setOf(RegexOption.IGNORE_CASE)
        ),
        description = GitErrorsText.get().submoduleRepositoryDoesNotExist
    ),
    InvalidSubmoduleSHA(
        regex = Regex(
            "(Fetched in submodule path '(.+)', but it did not contain (.+). " +
                "Direct fetching of that commit failed.)|" +
                "(获取了子模组路径 '(.+)'，但是它没有包含 (.+)。直接获取该提交失败。)|" +
                "(无法在子模组路径 '(.+)' 中找到当前版本)|" +
                "(fatal: Needed a single revision)"
        ),
        description = GitErrorsText.get().invalidSubmoduleSHA
    ),

    // lfs命令错误
    LfsAttributeDoesNotMatch(
        regex = Regex(
            "The .+ attribute should be .+ but is .+"
        ),
        description = GitErrorsText.get().lfsAttributeDoesNotMatch
    ),
    ErrorDownloadingObject(
        regex = Regex(
            "(Error downloading object: .*)|" +
                "(LFS: Repository or object not found: .+)|"
        ),
        description = GitErrorsText.get().errorDownloadingObject
    ),
    LfsNotInstall(
        regex = Regex(
            "git: 'lfs' is not a git command. See 'git --help'."
        ),
        description = GitErrorsText.get().lfsNotInstall
    ),

    // 其他命令错误
    LockFileAlreadyExists(
        regex = Regex(
            "(Another git process seems to be running in this repository, e.g.)|" +
                "(error: could not lock config file (.+): File exists)"
        ),
        description = GitErrorsText.get().lockFileAlreadyExists
    ),
    BadRevision(
        regex = Regex(
            "fatal: bad revision '(.*)'"
        ),
        description = GitErrorsText.get().badRevision
    ),
    NotAGitRepository(
        regex = Regex(
            "fatal: [Nn]ot a git repository \\(or any of the parent directories\\): .git"
        ),
        description = GitErrorsText.get().notAGitRepository
    );

    companion object {
        fun matchError(message: String): GitErrors? {
            for (gitError in values()) {
                if (gitError.regex.matches(message)) {
                    return gitError
                }
            }
            return null
        }
    }
}
