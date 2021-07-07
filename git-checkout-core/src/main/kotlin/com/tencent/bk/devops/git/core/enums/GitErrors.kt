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
import com.tencent.bk.devops.git.core.i18n.GitErrorsText
import com.tencent.bk.devops.plugin.pojo.ErrorType

enum class GitErrors(
    val regex: Regex,
    val description: String?,
    val errorType: ErrorType = ErrorType.USER,
    val errorCode: Int = GitConstants.CONFIG_ERROR
) {
    SshKeyAuditUnverified(
        regex = Regex(
            "fatal: Could not read from remote repository."
        ),
        description = GitErrorsText.get().sshAuthenticationFailed
    ),
    SshAuthenticationFailed(
        regex = Regex(
            "fatal: Authentication failed"
        ),
        description = GitErrorsText.get().sshAuthenticationFailed
    ),
    SshPermissionDenied(
        regex = Regex(
            "fatal: Could not read from remote repository."
        ),
        description = GitErrorsText.get().sshPermissionDenied
    ),
    HttpsAuthenticationFailed(
        regex = Regex(
            "(The requested URL returned error: 403)|" +
                "(fatal: could not read Username for '(.+)': terminal prompts disabled)|" +
                "fatal: Authentication failed for '(.+)'"
        ),
        description = GitErrorsText.get().httpsAuthenticationFailed
    ),
    RemoteDisconnection(
        regex = Regex(
            "(fatal: The remote end hung up unexpectedly)|" +
                "(fatal: unable to access '(.+)': The requested URL returned error: 502)"
        ),
        description = GitErrorsText.get().remoteDisconnection,
        errorType = ErrorType.THIRD_PARTY,
        errorCode = GitConstants.GIT_ERROR
    ),
    HostDown(
        regex = Regex(
            "fatal: unable to access '(.+)': Failed to connect to (.+): Host is down"
        ),
        description = GitErrorsText.get().hostDown,
        errorType = ErrorType.THIRD_PARTY,
        errorCode = GitConstants.GIT_ERROR
    ),
    RebaseConflicts(
        regex = Regex(
            "Resolve all conflicts manually, mark them as resolved with"
        ),
        description = GitErrorsText.get().rebaseConflicts
    ),
    MergeConflicts(
        regex = Regex(
            "(Merge conflict|Automatic merge failed; fix conflicts and then commit the result)"
        ),
        description = GitErrorsText.get().mergeConflicts
    ),
    HttpsRepositoryNotFound(
        regex = Regex(
            "fatal: repository '(.+)' not found"
        ),
        description = GitErrorsText.get().httpsRepositoryNotFound
    ),
    SshRepositoryNotFound(
        regex = Regex(
            "ERROR: Repository not found"
        ),
        description = GitErrorsText.get().sshRepositoryNotFound
    ),
    ProjectNotFound(
        regex = Regex(
            "fatal: remote error: Git:Project not found."
        ),
        description = GitErrorsText.get().projectNotFound
    ),
    BranchDeletionFailed(
        regex = Regex(
            "error: unable to delete '(.+)': remote ref does not exist"
        ),
        description = GitErrorsText.get().branchDeletionFailed
    ),
    RevertConflicts(
        regex = Regex(
            "error: could not revert .*"
        ),
        description = GitErrorsText.get().revertConflicts
    ),
    NoMatchingRemoteBranch(
        regex = Regex(
            "(There are no candidates for (rebasing|merging) among the refs that you just fetched.)|" +
                "(fatal: couldn't find remote ref (.+))"
        ),
        description = GitErrorsText.get().noMatchingRemoteBranch
    ),
    NoExistingRemoteBranch(
        regex = Regex(
            "(Your configuration specifies to merge with the ref '(.+)') ||" +
                "(fatal: '(.+)' is not a commit and a branch '(.+)' cannot be created from it)"
        ),
        description = GitErrorsText.get().noExistingRemoteBranch
    ),
    NoSubmoduleMapping(
        regex = Regex(
            "No submodule mapping found in .gitmodules for path '(.+)'"
        ),
        description = GitErrorsText.get().noSubmoduleMapping
    ),
    SubmoduleRepositoryDoesNotExist(
        regex = Regex(
            "fatal: clone of '.+' into submodule path '(.+)' failed"
        ),
        description = GitErrorsText.get().submoduleRepositoryDoesNotExist
    ),
    InvalidSubmoduleSHA(
        regex = Regex(
            "Fetched in submodule path '(.+)', but it did not contain (.+). " +
                "Direct fetching of that commit failed."
        ),
        description = GitErrorsText.get().invalidSubmoduleSHA
    ),
    InvalidMerge(
        regex = Regex(
            "merge: (.+) - not something we can merge"
        ),
        description = GitErrorsText.get().invalidMerge
    ),
    BranchAlreadyExists(
        regex = Regex(
            "fatal: A branch named '(.+)' already exists."
        ),
        description = GitErrorsText.get().branchAlreadyExists
    ),
    NoMatchingBranch(
        regex = Regex(
            "error: pathspec '(.+)' did not match any file(s) known to git."
        ),
        description = GitErrorsText.get().noMatchingBranch
    ),
    BadRevision(
        regex = Regex(
            "fatal: bad revision '(.*)'"
        ),
        description = GitErrorsText.get().badRevision
    ),
    NotAGitRepository(
        regex = Regex(
            "fatal: [Nn]ot a git repository \\(or any of the parent directories\\): (.*)'"
        ),
        description = GitErrorsText.get().notAGitRepository
    ),
    CannotMergeUnrelatedHistories(
        regex = Regex(
            "fatal: refusing to merge unrelated histories"
        ),
        description = GitErrorsText.get().cannotMergeUnrelatedHistories
    ),
    LfsAttributeDoesNotMatch(
        regex = Regex(
            "The .+ attribute should be .+ but is .+"
        ),
        description = GitErrorsText.get().lfsAttributeDoesNotMatch
    ),
    ErrorDownloadingObject(
        regex = Regex(
            "Error downloading object: .*"
        ),
        description = GitErrorsText.get().errorDownloadingObject
    ),
    InvalidObjectName(
        regex = Regex(
            "fatal: path '(.+)' does not exist .+"
        ),
        description = GitErrorsText.get().invalidObjectName
    ),
    LockFileAlreadyExists(
        regex = Regex(
            "Another git process seems to be running in this repository, e.g."
        ),
        description = GitErrorsText.get().lockFileAlreadyExists
    ),
    LocalChangesOverwritten(
        regex = Regex(
            "error: (?:Your local changes to the following|The following untracked working tree) files " +
                "would be overwritten by checkout:"
        ),
        description = GitErrorsText.get().localChangesOverwritten
    ),
    UnresolvedConflicts(
        regex = Regex(
            "mark them as resolved using git add|fatal: Exiting because of an unresolved conflict"
        ),
        description = GitErrorsText.get().unresolvedConflicts
    ),
    ConfigLockFileAlreadyExists(
        regex = Regex(
            "error: could not lock config file (.+): File exists"
        ),
        description = GitErrorsText.get().configLockFileAlreadyExists
    ),
    RemoteAlreadyExists(
        regex = Regex(
            "fatal: remote (.+) already exists."
        ),
        description = GitErrorsText.get().remoteAlreadyExists
    ),
    TagAlreadyExists(
        regex = Regex(
            "fatal: tag '(.+)' already exists"
        ),
        description = GitErrorsText.get().tagAlreadyExists
    ),
    MergeWithLocalChanges(
        regex = Regex(
            "error: Your local changes to the following files would be overwritten by merge:"
        ),
        description = GitErrorsText.get().mergeWithLocalChanges
    ),
    NoInitializeBranch(
        regex = Regex(
            "fatal: You are on a branch yet to be born"
        ),
        description = GitErrorsText.get().noInitializeBranch
    )
}
