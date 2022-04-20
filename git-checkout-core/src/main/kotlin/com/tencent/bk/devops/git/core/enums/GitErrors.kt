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
import com.tencent.bk.devops.git.core.constant.GitConstants.wikiUrl
import com.tencent.bk.devops.git.core.i18n.GitErrorsText
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.plugin.pojo.ErrorType

enum class GitErrors(
    val regex: Regex,
    val title: String?,
    val description: String? = null,
    val errorType: ErrorType = ErrorType.USER,
    val errorCode: Int = GitConstants.CONFIG_ERROR,
    val internalErrorCode: Int = 0
) {
    // fetch命令错误
    AuthenticationFailed(
        regex = Regex(
            "(The requested URL returned error: 403)|" +
                "(fatal: could not read Username for '(.+)': terminal prompts disabled)|" +
                "(fatal: Authentication failed for '(.+)')|" +
                "(fatal: Could not read from remote repository.)|" +
                "(fatal: Authentication failed)|" +
                "(fatal: '(.+)' 鉴权失败)|" +
                "(fatal: 无法读取远程仓库。)" +
                "(fatal: repository '(.+)' not found)|" +
                "(fatal: .* Git repository not found)|" +
                "(fatal: 远程错误：Git repository not found)|" +
                "(ERROR: Repository not found)|" +
                "(fatal: remote error: Git:Project not found.)|" +
                "(fatal: could not read Username for '(.+)': No such device or address)|" +
                "(error: The requested URL returned error: 401 Unauthorized while accessing .*)"
        ),
        title = when (EnvHelper.getContext(CONTEXT_GIT_PROTOCOL)) {
            GitProtocolEnum.SSH.name -> GitErrorsText.get().sshAuthenticationFailed
            else -> GitErrorsText.get().httpAuthenticationFailed
        },
        description = "$wikiUrl#%E4%B8%80%E6%8E%88%E6%9D%83%E5%A4%B1%E8%B4%A5",
        internalErrorCode = 1
    ),
    RemoteServerFailed(
        regex = Regex(
            "(fatal: (the|The) remote end hung up unexpectedly)|" +
                "(fatal: unable to access '(.+)': The requested URL returned error: 502)|" +
                "(fatal: 远程错误：Internal server error)|" +
                "(fatal: 远端意外挂断了)|" +
                "(Git:Server is busy, please try again later)|" +
                "(fatal: unable to access '(.+)': Failed to connect to (.+): Host is down)|" +
                "(error: RPC failed; curl 56 Recv failure: Connection reset by peer)|" +
                "(fatal: 过早的文件结束符（EOF）)|" +
                "(fatal: index-pack 失败)|" +
                "(fatal: early EOF)|" +
                "(fatal: index-pack failed)"
        ),
        title = GitErrorsText.get().remoteServerFailed,
        errorType = ErrorType.THIRD_PARTY,
        errorCode = GitConstants.GIT_ERROR,
        description = "$wikiUrl#%E4%BA%8C%E8%BF%9C%E7%A8%8B%E6%9C%8D%E5%8A%A1%E7%AB%AF%E5%BC%82%E5%B8%B8",
        internalErrorCode = 2
    ),
    ConnectionTimeOut(
        regex = Regex(
            "ssh: connect to host (.+) port (\\d+): Connection timed out"
        ),
        title = GitErrorsText.get().connectionTimeOut,
        internalErrorCode = 3
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
        title = GitErrorsText.get().noMatchingBranch,
        description = "$wikiUrl#%E4%B8%89%E5%88%86%E6%94%AF%E6%88%96commit%E4%B8%8D%E5%AD%98%E5%9C%A8",
        internalErrorCode = 4
    ),
    NoInitializeBranch(
        regex = Regex(
            "(fatal: You are on a branch yet to be born)|" +
                "(fatal: 您位于一个尚未初始化的分支)"
        ),
        title = GitErrorsText.get().noInitializeBranch,
        description = "$wikiUrl#%E5%9B%9Bcheckout%E7%9A%84%E5%88%86%E6%94%AF%E5%90%8D%E4%B8%BA%E7%A9%BA",
        internalErrorCode = 5
    ),
    SparseCheckoutLeavesNoEntry(
        regex = Regex(
            "(error: Sparse checkout leaves no entry on working directory)"
        ),
        title = GitErrorsText.get().sparseCheckoutLeavesNoEntry,
        description = "$wikiUrl#%E4%BA%94%E9%83%A8%E5%88%86%E6%A3%80%E5%87%BA%E9%94%99%E8%AF%AF%E8%AF%B7%E6%A3%80%E6" +
            "%9F%A5%E9%83%A8%E5%88%86%E6%A3%80%E5%87%BA%E7%9A%84%E6%96%87%E4%BB%B6%E6%98%AF%E5%90%A6%E5%AD%98%E5%9C%A8",
        internalErrorCode = 6
    ),
    BranchOrPathNameConflicts(
        regex = Regex(
            "(fatal: '(.+)' 既可以是一个本地文件，也可以是一个跟踪分支。)"
        ),
        title = GitErrorsText.get().branchOrPathNameConflicts,
        description = "$wikiUrl#%E5%85%AD%E5%88%86%E6%94%AF%E6%88%96%E8%B7%AF%E5%BE%84%E5%90%8D%E5%86%B2%E7%AA%81",
        internalErrorCode = 7
    ),

    // merge命令错误
    MergeConflicts(
        regex = Regex(
            "(Automatic merge failed; fix conflicts and then commit the result.)|" +
                "(Resolve all conflicts manually, mark them as resolved with)|" +
                "(自动合并失败，修正冲突然后提交修正的结果。)"
        ),
        title = GitErrorsText.get().mergeConflicts,
        description = "$wikiUrl#%E4%B8%83merge%E5%86%B2%E7%AA%81%E8%AF%B7%E5%85%88%E8%A7%A3%E5%86%B3%E5%86%B2%E7" +
            "%AA%81%E7%84%B6%E5%90%8E%E5%86%8D%E6%9E%84%E5%BB%BA",
        internalErrorCode = 8
    ),
    InvalidMerge(
        regex = Regex(
            "(merge: (.+) - not something we can merge)|" +
                "(merge：.+ - 不能合并)"
        ),
        title = GitErrorsText.get().invalidMerge,
        description = "$wikiUrl#%E5%85%AB%E5%90%88%E5%B9%B6%E5%A4%B1%E8%B4%A5%E5%8F%AF%E8%83%BD%E6%98%AF%E5%9B%A0" +
            "%E4%B8%BA%E6%BA%90%E5%88%86%E6%94%AF%E8%A2%AB%E5%88%A0%E9%99%A4%E5%AF%BC%E8%87%B4",
        internalErrorCode = 9
    ),
    CannotMergeUnrelatedHistories(
        regex = Regex(
            "(fatal: refusing to merge unrelated histories)|" +
                "(fatal: 拒绝合并无关的历史)"
        ),
        title = GitErrorsText.get().cannotMergeUnrelatedHistories,
        description = "$wikiUrl#%E5%85%AB%E5%90%88%E5%B9%B6%E5%A4%B1%E8%B4%A5%E5%8F%AF%E8%83%BD%E6%98%AF%E5%9B%A0%E4" +
            "%B8%BA%E6%BA%90%E5%88%86%E6%94%AF%E8%A2%AB%E5%88%A0%E9%99%A4%E5%AF%BC%E8%87%B4",
        internalErrorCode = 10
    ),
    LocalChangesOverwritten(
        regex = Regex(
            "(error: Your local changes to the following files would be overwritten by merge:)|" +
                "(error: Your local changes to the following files would be overwritten by checkout:)|" +
                "(error: 您对下列文件的本地修改将被检出操作覆盖：)"
        ),
        title = GitErrorsText.get().localChangesOverwritten,
        description = "$wikiUrl#%E5%8D%81%E5%88%87%E6%8D%A2%E5%88%86%E6%94%AF%E5%A4%B1%E8%B4%A5%E6%9C%AC%E5%9C%B0%E4" +
            "%BF%AE%E6%94%B9%E7%9A%84%E6%96%87%E4%BB%B6%E5%B0%86%E8%A2%AB%E8%A6%86%E7%9B%96",
        internalErrorCode = 11
    ),

    // submodule命令错误
    NoSubmoduleMapping(
        regex = Regex(
            "(fatal: No submodule mapping found in .gitmodules for path '(.+)')|" +
                "(fatal: 在 .gitmodules 中没有发现路径 '(.+)' 的子模组映射)|" +
                "(fatal: 在 .gitmodules 中未找到子模组 '(.+)' 的 url)|" +
                "(fatal: No url found for submodule path '(.+)' in .gitmodules)"
        ),
        title = GitErrorsText.get().noSubmoduleMapping,
        description = "$wikiUrl#%E5%8D%81%E4%B8%80%E5%AD%90%E6%A8%A1%E5%9D%97%E6%9B%B4%E6%96%B0%E5%A4%B1%E8%B4%A5",
        internalErrorCode = 12
    ),
    SubmoduleRepositoryDoesNotExist(
        regex = Regex(
            pattern = "(clone of '.+' into submodule path '(.+)' failed)|" +
                "(fatal：无法克隆 '(.+)' 到子模组路径 '(.+)')",
            options = setOf(RegexOption.IGNORE_CASE)
        ),
        title = GitErrorsText.get().submoduleRepositoryDoesNotExist,
        description = "$wikiUrl#%E5%8D%81%E4%BA%8C%E5%AD%90%E6%A8%A1%E5%9D%97%E9%85%8D%E7%BD%AE%E7%9A%84%E4" +
            "%BB%93%E5%BA%93%E4%B8%8D%E5%AD%98%E5%9C%A8",
        internalErrorCode = 13
    ),
    InvalidSubmoduleSHA(
        regex = Regex(
            "(Fetched in submodule path '(.+)', but it did not contain (.+). " +
                "Direct fetching of that commit failed.)|" +
                "(获取了子模组路径 '(.+)'，但是它没有包含 (.+)。直接获取该提交失败。)|" +
                "(无法在子模组路径 '(.+)' 中找到当前版本)|" +
                "(fatal: Needed a single revision)"
        ),
        title = GitErrorsText.get().invalidSubmoduleSHA,
        description = "$wikiUrl#%E5%8D%81%E4%B8%89%E5%AD%90%E6%A8%A1%E5%9D%97%E6%8C%87%E5%90%91%E7%9A" +
            "%84commit%E4%B8%8D%E5%AD%98%E5%9C%A8",
        internalErrorCode = 14
    ),

    // lfs命令错误
    LfsAttributeDoesNotMatch(
        regex = Regex(
            "The .+ attribute should be .+ but is .+"
        ),
        title = GitErrorsText.get().lfsAttributeDoesNotMatch,
        internalErrorCode = 15
    ),
    ErrorDownloadingObject(
        regex = Regex(
            "(Error downloading object: .*)|" +
                "(LFS: Repository or object not found: .+)"
        ),
        title = GitErrorsText.get().errorDownloadingObject,
        description = "$wikiUrl#%E5%8D%81%E5%9B%9Bgit-lfs%E6%96%87%E4%BB%B6%E4%B8%8B%E8%BD%BD%E9%94%99%E8%AF%AF",
        internalErrorCode = 16
    ),
    LfsNotInstall(
        regex = Regex(
            "(git: 'lfs' is not a git command. See 'git --help'.)|" +
                "(git：'lfs' 不是一个 git 命令。参见 'git --help'。)"
        ),
        title = GitErrorsText.get().lfsNotInstall,
        description = "$wikiUrl#%E5%8D%81%E4%BA%94lfs%E7%A8%8B%E5%BA%8F%E6%B2%A1%E6%9C%89%E5%AE%89%E8%A3%85",
        internalErrorCode = 17
    ),

    // 其他命令错误
    LockFileAlreadyExists(
        regex = Regex(
            "(Another git process seems to be running in this repository, e.g.)|" +
                "(error: could not lock config file (.+): File exists)"
        ),
        title = GitErrorsText.get().lockFileAlreadyExists,
        internalErrorCode = 18
    ),
    BadRevision(
        regex = Regex(
            "fatal: bad revision '(.*)'"
        ),
        title = GitErrorsText.get().badRevision,
        internalErrorCode = 19
    ),
    NotAGitRepository(
        regex = Regex(
            "fatal: [Nn]ot a git repository \\(or any of the parent directories\\): .git"
        ),
        title = GitErrorsText.get().notAGitRepository,
        internalErrorCode = 20
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
