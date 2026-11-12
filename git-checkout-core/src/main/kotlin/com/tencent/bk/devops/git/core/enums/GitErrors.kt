/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.bk.devops.git.core.constant.ContextConstants.CONTEXT_REPOSITORY_TYPE
import com.tencent.bk.devops.git.core.i18n.GitErrorsText
import com.tencent.bk.devops.git.core.pojo.api.RepositoryType
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.GitErrorNormalizer
import com.tencent.bk.devops.plugin.pojo.ErrorType

/**
 * git错误规则
 *
 * patterns匹配的是GitErrorNormalizer归一化后的文本:全角标点已转半角,行首中文前缀已转成fatal:/error:/remote error:,
 * 所以只需要写错误独有的关键片段,不需要写前缀,也不需要区分全角半角。中文片段以git源码po/zh_CN.po为准。
 */
@SuppressWarnings("LongParameterList", "LargeClass")
enum class GitErrors(
    patterns: List<String>,
    private val titleText: () -> String?,
    private val causeText: () -> String?,
    private val solutionText: () -> String?,
    private val wikiText: () -> String?,
    val errorType: ErrorType = ErrorType.USER,
    val errorCode: Int = 800001,
    val priority: Int = GitErrorPriority.NORMAL,
    /**
     * 默认只匹配错误流,输出到标准输出的错误需要声明
     */
    val matchStdOut: Boolean = false
) {
    // fetch命令错误
    AuthenticationFailed(
        patterns = listOf(
            "The requested URL returned error: 40[13]\\b",
            "could not read (Username|Password) for",
            "Authentication failed",
            "鉴权失败"
        ),
        titleText = { GitErrorsText.get().httpAuthenticationFailed },
        causeText = { GitErrorsText.get().httpAuthenticationFailedCause },
        solutionText = { httpAuthenticationFailedSolution() },
        wikiText = { GitErrorsText.get().httpAuthenticationFailedWiki },
        errorCode = 800002,
        priority = GitErrorPriority.HIGH
    ),

    SshAuthenticationFailed(
        patterns = listOf(
            "Could not read from remote repository",
            "无法读取远程仓库",
            "Permission denied \\(publickey"
        ),
        titleText = { GitErrorsText.get().sshAuthenticationFailed },
        causeText = { GitErrorsText.get().sshAuthenticationFailedCause },
        solutionText = { GitErrorsText.get().sshAuthenticationFailedSolution },
        wikiText = { GitErrorsText.get().sshAuthenticationFailedWiki },
        errorCode = 800003,
        priority = GitErrorPriority.LOW
    ),

    RepositoryNotFoundFailed(
        patterns = listOf(
            "repository '.+' not found",
            "Repository not found",
            "Git:Project not found",
            "仓库 '.+' 未找到"
        ),
        titleText = { GitErrorsText.get().repositoryNotFoundFailed },
        causeText = { GitErrorsText.get().httpAuthenticationFailedCause },
        solutionText = { httpAuthenticationFailedSolution() },
        wikiText = { GitErrorsText.get().httpAuthenticationFailedWiki },
        errorCode = 800004,
        priority = GitErrorPriority.HIGH
    ),

    RemoteServerFailed(
        patterns = listOf(
            "the remote end hung up unexpectedly",
            "远端意外挂断了",
            "The requested URL returned error: (429|50[0-4])\\b",
            "remote error: Internal server error",
            "remote error: .*unavailable",
            "Server is busy, please try again later",
            "too many request, your request was forbidden",
            "Failed to connect to .+: Host is down",
            "RPC (failed|失败)",
            "early EOF",
            "过早的文件结束符",
            "index-pack (failed|失败|died|终止)",
            "invalid index-pack output",
            "无效的 index-pack 输出",
            "bad pack header",
            "(无效|坏)的包头"
        ),
        titleText = { GitErrorsText.get().remoteServerFailed },
        causeText = { GitErrorsText.get().remoteServerFailedCause },
        solutionText = { GitErrorsText.get().remoteServerFailedSolution },
        wikiText = { GitErrorsText.get().remoteServerFailedWiki },
        errorType = ErrorType.THIRD_PARTY,
        errorCode = 800005,
        priority = GitErrorPriority.HIGH
    ),

    ConnectionTimeOut(
        patterns = listOf(
            "ssh: connect to host .+ port \\d+: (Connection|Operation) timed out",
            "Failed to connect to .+(Connection timed out|Operation timed out|Timed out)",
            "Connection timed out after \\d+ milliseconds",
            "Could not resolve (host|hostname)"
        ),
        titleText = { GitErrorsText.get().connectionTimeOut },
        causeText = { GitErrorsText.get().connectionTimeOutCause },
        solutionText = { GitErrorsText.get().connectionTimeOutSolution },
        wikiText = { GitErrorsText.get().connectionTimeOutWiki },
        errorCode = 800006,
        priority = GitErrorPriority.HIGH
    ),

    // checkout命令错误
    NoMatchingBranch(
        patterns = listOf(
            "couldn't find remote ref",
            "无法找到远程引用",
            "无法发现远程 HEAD 引用",
            "Your configuration specifies to merge with the ref",
            "您的配置中指定要合并远程的引用",
            "is not a commit and a branch '.+' cannot be created from it",
            "不是一个提交,(不能|无法)基于它创建分支",
            "pathspec '.+' did not match any file\\(s\\) known to git",
            "路径规格 '.+' 未匹配任何 git 已知文件",
            "path '.+' does not exist",
            "路径 '.+' 不存在",
            "reference is not a tree",
            "引用不是一个树",
            "unable to read tree \\(",
            "无法读取树\\(",
            "requested upstream branch '.+' does not exist",
            "请求的上游分支 '.+' 不存在",
            "Cannot update paths and switch to branch",
            "不能同时更新路径并切换到分支"
        ),
        titleText = { GitErrorsText.get().noMatchingBranch },
        causeText = { GitErrorsText.get().noMatchingBranchCause },
        solutionText = { GitErrorsText.get().noMatchingBranchSolution },
        wikiText = { GitErrorsText.get().noMatchingBranchWiki },
        errorCode = 800007
    ),

    EmptyBranch(
        patterns = listOf(
            "You are on a branch yet to be born",
            "您位于一个尚未初始化的分支"
        ),
        titleText = { GitErrorsText.get().emptyBranch },
        causeText = { GitErrorsText.get().emptyBranchCause },
        solutionText = { GitErrorsText.get().emptyBranchSolution },
        wikiText = { GitErrorsText.get().emptyBranchWiki },
        errorCode = 800008
    ),

    SparseCheckoutLeavesNoEntry(
        patterns = listOf(
            "Sparse checkout leaves no entry on working directory"
        ),
        titleText = { GitErrorsText.get().sparseCheckoutLeavesNoEntry },
        causeText = { GitErrorsText.get().sparseCheckoutLeavesNoEntryCause },
        solutionText = { GitErrorsText.get().sparseCheckoutLeavesNoEntrySolution },
        wikiText = { GitErrorsText.get().sparseCheckoutLeavesNoEntryWiki },
        errorCode = 800009
    ),

    BranchOrPathNameConflicts(
        patterns = listOf(
            "could be both a local file and a tracking branch",
            "既可以是一个本地文件,也可以是一个跟踪分支"
        ),
        titleText = { GitErrorsText.get().branchOrPathNameConflicts },
        causeText = { GitErrorsText.get().branchOrPathNameConflictsCause },
        solutionText = { GitErrorsText.get().branchOrPathNameConflictsSolution },
        wikiText = { GitErrorsText.get().branchOrPathNameConflictsWiki },
        errorCode = 800010
    ),

    // merge命令错误
    MergeConflicts(
        patterns = listOf(
            "Automatic merge failed; fix conflicts and then commit the result",
            "Automatischer Merge fehlgeschlagen; beheben Sie die Konflikte und committen Sie dann das Ergebnis",
            "Resolve all conflicts manually, mark them as resolved with",
            "自动合并失败,修正冲突然后提交修正的结果",
            "add_cacheinfo failed to refresh for path '.+'; merge aborting",
            "add_cacheinfo 无法刷新路径 '.+',合并终止"
        ),
        titleText = { GitErrorsText.get().mergeConflicts },
        causeText = { GitErrorsText.get().mergeConflictsCause },
        solutionText = { GitErrorsText.get().mergeConflictsSolution },
        wikiText = { GitErrorsText.get().mergeConflictsWiki },
        errorCode = 800011,
        matchStdOut = true
    ),

    InvalidMerge(
        patterns = listOf(
            "not something we can merge",
            "不是可以合并的东西",
            " - (不能|无法)被?合并"
        ),
        titleText = { GitErrorsText.get().invalidMerge },
        causeText = { GitErrorsText.get().invalidMergeCause },
        solutionText = { GitErrorsText.get().invalidMergeSolution },
        wikiText = { GitErrorsText.get().invalidMergeWiki },
        errorCode = 800012
    ),

    CannotMergeUnrelatedHistories(
        patterns = listOf(
            "refusing to merge unrelated histories",
            "拒绝合并无关的历史"
        ),
        titleText = { GitErrorsText.get().cannotMergeUnrelatedHistories },
        causeText = { GitErrorsText.get().cannotMergeUnrelatedHistoriesCause },
        solutionText = { GitErrorsText.get().cannotMergeUnrelatedHistoriesSolution },
        wikiText = { GitErrorsText.get().cannotMergeUnrelatedHistoriesWiki },
        errorCode = 800013
    ),

    LocalChangesOverwritten(
        patterns = listOf(
            "Your local changes to the following files would be overwritten by",
            "您对下列文件的本地修改将被.+覆盖",
            "untracked working tree files would be overwritten by",
            "工作区中下列未跟踪的文件将会因为.+而被覆盖"
        ),
        titleText = { GitErrorsText.get().localChangesOverwritten },
        causeText = { GitErrorsText.get().localChangesOverwrittenCause },
        solutionText = { GitErrorsText.get().localChangesOverwrittenSolution },
        wikiText = { GitErrorsText.get().localChangesOverwrittenWiki },
        errorCode = 800014
    ),

    // submodule命令错误
    NoSubmoduleMapping(
        patterns = listOf(
            "No submodule mapping found in \\.gitmodules for path",
            "No url found for submodule path '.+' in \\.gitmodules",
            "在 \\.gitmodules 中未找到子模组(路径)? '.+' 的 url",
            "在 \\.gitmodules 中没有发现路径 '.+' 的子模组映射"
        ),
        titleText = { GitErrorsText.get().noSubmoduleMapping },
        causeText = { GitErrorsText.get().noSubmoduleMappingCause },
        solutionText = { GitErrorsText.get().noSubmoduleMappingSolution },
        wikiText = { GitErrorsText.get().noSubmoduleMappingWiki },
        errorCode = 800015
    ),

    SubmoduleRepositoryDoesNotExist(
        patterns = listOf(
            "clone of '.+' into submodule path '.+' failed",
            "无法克隆 '.+' 到子模组路径"
        ),
        titleText = { GitErrorsText.get().submoduleRepositoryDoesNotExist },
        causeText = { GitErrorsText.get().submoduleRepositoryDoesNotExistCause },
        solutionText = { GitErrorsText.get().submoduleRepositoryDoesNotExistSolution },
        wikiText = { GitErrorsText.get().submoduleRepositoryDoesNotExistWiki },
        errorCode = 800016
    ),

    InvalidSubmoduleSHA(
        patterns = listOf(
            "Fetched in submodule path '.+', but it did not contain",
            "获取了子模组路径 '.+',但是它没有包含",
            "Unable to find current revision in submodule path",
            "无法在子模组路径 '.+' 中找到当前版本",
            "Needed a single revision",
            "需要一个单独的版本"
        ),
        titleText = { GitErrorsText.get().invalidSubmoduleSHA },
        causeText = { GitErrorsText.get().invalidSubmoduleSHACause },
        solutionText = { GitErrorsText.get().invalidSubmoduleSHASolution },
        wikiText = { GitErrorsText.get().invalidSubmoduleSHAWiki },
        errorCode = 800017
    ),

    // lfs命令错误
    LfsAttributeDoesNotMatch(
        patterns = listOf(
            "The .+ attribute should be .+ but is"
        ),
        titleText = { GitErrorsText.get().lfsAttributeDoesNotMatch },
        causeText = { GitErrorsText.get().lfsAttributeDoesNotMatchCause },
        solutionText = { GitErrorsText.get().lfsAttributeDoesNotMatchSolution },
        wikiText = { GitErrorsText.get().lfsAttributeDoesNotMatchWiki },
        errorCode = 800018
    ),

    ErrorDownloadingObject(
        patterns = listOf(
            "Error downloading object",
            "LFS: Repository or object not found",
            "batch response: Fatal error: We couldn't respond to your request in time"
        ),
        titleText = { GitErrorsText.get().errorDownloadingObject },
        causeText = { GitErrorsText.get().errorDownloadingObjectCause },
        solutionText = { GitErrorsText.get().errorDownloadingObjectSolution },
        wikiText = { GitErrorsText.get().errorDownloadingObjectWiki },
        errorCode = 800019
    ),

    LfsNotInstall(
        patterns = listOf(
            "'lfs' is not a git command",
            "'lfs' 不是一个 git 命令"
        ),
        titleText = { GitErrorsText.get().lfsNotInstall },
        causeText = { GitErrorsText.get().lfsNotInstallCause },
        solutionText = { GitErrorsText.get().lfsNotInstallSolution },
        wikiText = { GitErrorsText.get().lfsNotInstallWiki },
        errorCode = 800020
    ),

    // 其他命令错误
    LockFileAlreadyExists(
        patterns = listOf(
            "Another git process seems to be running in this repository",
            "另一 git 进程似乎正在此仓库中运行",
            "(could not lock config file|无法锁定配置文件) .+:\\s*(File exists|文件已存在)",
            "(Unable to create|无法创建) '.+':\\s*(File exists|文件已存在)"
        ),
        titleText = { GitErrorsText.get().lockFileAlreadyExists },
        causeText = { GitErrorsText.get().lockFileAlreadyExistsCause },
        solutionText = { GitErrorsText.get().lockFileAlreadyExistsSolution },
        wikiText = { GitErrorsText.get().lockFileAlreadyExistsWiki },
        errorCode = 800021
    ),

    BadRevision(
        patterns = listOf(
            "bad revision '",
            "(错误|坏)的版本 '"
        ),
        titleText = { GitErrorsText.get().badRevision },
        causeText = { GitErrorsText.get().badRevisionCause },
        solutionText = { GitErrorsText.get().badRevisionSolution },
        wikiText = { GitErrorsText.get().badRevisionWiki },
        errorCode = 800022
    ),

    NotAGitRepository(
        patterns = listOf(
            "not a git repository \\(or any (of the parent directories|parent up to mount point)",
            "不是 git 仓库\\(或者(任何父目录|直至挂载点)"
        ),
        titleText = { GitErrorsText.get().notAGitRepository },
        causeText = { GitErrorsText.get().notAGitRepositoryCause },
        solutionText = { GitErrorsText.get().notAGitRepositorySolution },
        wikiText = { GitErrorsText.get().notAGitRepositoryWiki },
        errorCode = 800023
    ),

    LibcurlNotSupportHttps(
        patterns = listOf(
            "Protocol \"?https\"? not supported"
        ),
        titleText = { GitErrorsText.get().libcurlNotSupportHttps },
        causeText = { GitErrorsText.get().libcurlNotSupportHttpsCause },
        solutionText = { GitErrorsText.get().libcurlNotSupportHttpsSolution },
        wikiText = { GitErrorsText.get().libcurlNotSupportHttpsWiki },
        errorCode = 800024
    ),

    GitNotInstall(
        patterns = listOf(
            "Cannot run program \"git\"",
            "'git' 不是内部或外部命令",
            "'git' is not recognized as an internal or external command"
        ),
        titleText = { GitErrorsText.get().gitNotInstall },
        causeText = { GitErrorsText.get().gitNotInstallCause },
        solutionText = { GitErrorsText.get().gitNotInstallSolution },
        wikiText = { GitErrorsText.get().gitNotInstallWiki },
        errorCode = 800025,
        priority = GitErrorPriority.HIGH
    ),

    NotFoundGitRemoteHttps(
        patterns = listOf(
            "unable to find remote helper for 'https?'",
            "无法为 'https?' 找到远程助手",
            "'remote-https?' is not a git command",
            "'remote-https?' 不是一个 git 命令"
        ),
        titleText = { GitErrorsText.get().notFoundGitRemoteHttps },
        causeText = { GitErrorsText.get().notFoundGitRemoteHttpsCause },
        solutionText = { GitErrorsText.get().notFoundGitRemoteHttpsSolution },
        wikiText = { GitErrorsText.get().notFoundGitRemoteHttpsWiki },
        errorCode = 800026
    ),

    InvalidRefSpec(
        patterns = listOf(
            "invalid refspec",
            "无效的引用规格"
        ),
        titleText = { GitErrorsText.get().invalidRefSpec },
        causeText = { GitErrorsText.get().invalidRefSpecCause },
        solutionText = { GitErrorsText.get().invalidRefSpecSolution },
        wikiText = { GitErrorsText.get().invalidRefSpecWiki },
        errorCode = 800027
    ),

    MergeStashFail(
        patterns = listOf(
            "stash failed",
            "(储藏|贮藏)失败"
        ),
        titleText = { null },
        causeText = { null },
        solutionText = { null },
        wikiText = { null },
        errorCode = 800028
    ),

    SslCertificateFailed(
        patterns = listOf(
            "SSL certificate problem",
            "server certificate verification failed",
            "no alternative certificate subject name matches",
            "SEC_E_UNTRUSTED_ROOT"
        ),
        titleText = { GitErrorsText.get().sslCertificateFailed },
        causeText = { GitErrorsText.get().sslCertificateFailedCause },
        solutionText = { GitErrorsText.get().sslCertificateFailedSolution },
        wikiText = { GitErrorsText.get().sslCertificateFailedWiki },
        errorCode = 800029,
        priority = GitErrorPriority.HIGH
    ),

    NoSpaceLeftOnDevice(
        patterns = listOf(
            "No space left on device",
            "There is not enough space on the disk",
            "设备上没有空间",
            "磁盘空间不足"
        ),
        titleText = { GitErrorsText.get().noSpaceLeftOnDevice },
        causeText = { GitErrorsText.get().noSpaceLeftOnDeviceCause },
        solutionText = { GitErrorsText.get().noSpaceLeftOnDeviceSolution },
        wikiText = { GitErrorsText.get().noSpaceLeftOnDeviceWiki },
        errorCode = 800030,
        priority = GitErrorPriority.HIGH
    );

    val regex = Regex(
        pattern = patterns.joinToString("|") { "(?:$it)" },
        option = RegexOption.IGNORE_CASE
    )

    val title: String?
        get() = titleText()

    val cause: String?
        get() = causeText()

    val solution: String?
        get() = solutionText()

    val wiki: String?
        get() = wikiText()

    companion object {
        private val ALL = values()

        /**
         * 匹配单行git输出,一行同时匹配多个规则时取优先级最高的,同级取先定义的
         *
         * @param fromStdOut 是否来自标准输出,标准输出只匹配声明了matchStdOut的规则
         */
        fun matchError(message: String, fromStdOut: Boolean = false): GitErrors? {
            val normalized = GitErrorNormalizer.normalize(message)
            if (normalized.isEmpty()) {
                return null
            }
            return ALL.filter { (!fromStdOut || it.matchStdOut) && it.regex.containsMatchIn(normalized) }
                .maxByOrNull { it.priority }
        }

        private fun httpAuthenticationFailedSolution(): String? {
            return if (EnvHelper.getContext(CONTEXT_REPOSITORY_TYPE) == RepositoryType.URL.name) {
                GitErrorsText.get().httpAuthenticationFailedUrlSolution
            } else {
                GitErrorsText.get().httpAuthenticationFailedRepositorySolution
            }
        }
    }
}
