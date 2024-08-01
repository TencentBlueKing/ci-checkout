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

package com.tencent.bk.devops.git.core.i18n

import java.util.Locale

class GitErrorsText : TranslationBundle() {
    var httpAuthenticationFailed: String? = null
    var httpAuthenticationFailedCause: String? = null
    var httpAuthenticationFailedRepositorySolution: String? = null
    var httpAuthenticationFailedUrlSolution: String? = null
    var httpAuthenticationFailedWiki: String? = null
    var repositoryNotFoundFailed: String? = null
    var sshAuthenticationFailed: String? = null
    var sshAuthenticationFailedCause: String? = null
    var sshAuthenticationFailedSolution: String? = null
    var sshAuthenticationFailedWiki: String? = null
    var startUserIdAuthenticationFailed: String? = null
    var remoteServerFailed: String? = null
    var remoteServerFailedCause: String? = null
    var remoteServerFailedSolution: String? = null
    var remoteServerFailedWiki: String? = null
    var connectionTimeOut: String? = null
    var connectionTimeOutCause: String? = null
    var connectionTimeOutSolution: String? = null
    var connectionTimeOutWiki: String? = null
    var mergeConflicts: String? = null
    var mergeConflictsCause: String? = null
    var mergeConflictsSolution: String? = null
    var mergeConflictsWiki: String? = null
    var noSubmoduleMapping: String? = null
    var noSubmoduleMappingCause: String? = null
    var noSubmoduleMappingSolution: String? = null
    var noSubmoduleMappingWiki: String? = null
    var submoduleRepositoryDoesNotExist: String? = null
    var submoduleRepositoryDoesNotExistCause: String? = null
    var submoduleRepositoryDoesNotExistSolution: String? = null
    var submoduleRepositoryDoesNotExistWiki: String? = null
    var invalidSubmoduleSHA: String? = null
    var invalidSubmoduleSHACause: String? = null
    var invalidSubmoduleSHASolution: String? = null
    var invalidSubmoduleSHAWiki: String? = null
    var invalidMerge: String? = null
    var invalidMergeCause: String? = null
    var invalidMergeSolution: String? = null
    var invalidMergeWiki: String? = null
    var noMatchingBranch: String? = null
    var noMatchingBranchCause: String? = null
    var noMatchingBranchSolution: String? = null
    var noMatchingBranchWiki: String? = null
    var badRevision: String? = null
    var badRevisionCause: String? = null
    var badRevisionSolution: String? = null
    var badRevisionWiki: String? = null
    var notAGitRepository: String? = null
    var notAGitRepositoryCause: String? = null
    var notAGitRepositorySolution: String? = null
    var notAGitRepositoryWiki: String? = null
    var cannotMergeUnrelatedHistories: String? = null
    var cannotMergeUnrelatedHistoriesCause: String? = null
    var cannotMergeUnrelatedHistoriesSolution: String? = null
    var cannotMergeUnrelatedHistoriesWiki: String? = null
    var lfsAttributeDoesNotMatch: String? = null
    var lfsAttributeDoesNotMatchCause: String? = null
    var lfsAttributeDoesNotMatchSolution: String? = null
    var lfsAttributeDoesNotMatchWiki: String? = null
    var errorDownloadingObject: String? = null
    var errorDownloadingObjectCause: String? = null
    var errorDownloadingObjectSolution: String? = null
    var errorDownloadingObjectWiki: String? = null
    var lfsNotInstall: String? = null
    var lfsNotInstallCause: String? = null
    var lfsNotInstallSolution: String? = null
    var lfsNotInstallWiki: String? = null
    var lockFileAlreadyExists: String? = null
    var lockFileAlreadyExistsCause: String? = null
    var lockFileAlreadyExistsSolution: String? = null
    var lockFileAlreadyExistsWiki: String? = null
    var localChangesOverwritten: String? = null
    var localChangesOverwrittenCause: String? = null
    var localChangesOverwrittenSolution: String? = null
    var localChangesOverwrittenWiki: String? = null
    var emptyBranch: String? = null
    var emptyBranchCause: String? = null
    var emptyBranchSolution: String? = null
    var emptyBranchWiki: String? = null
    var sparseCheckoutLeavesNoEntry: String? = null
    var sparseCheckoutLeavesNoEntryCause: String? = null
    var sparseCheckoutLeavesNoEntrySolution: String? = null
    var sparseCheckoutLeavesNoEntryWiki: String? = null
    var branchOrPathNameConflicts: String? = null
    var branchOrPathNameConflictsCause: String? = null
    var branchOrPathNameConflictsSolution: String? = null
    var branchOrPathNameConflictsWiki: String? = null
    var emptyAccessToken: String? = null
    var libcurlNotSupportHttps: String? = null
    var libcurlNotSupportHttpsCause: String? = null
    var libcurlNotSupportHttpsSolution: String? = null
    var libcurlNotSupportHttpsWiki: String? = null
    var gitNotInstall: String? = null
    var gitNotInstallCause: String? = null
    var gitNotInstallSolution: String? = null
    var gitNotInstallWiki: String? = null
    var notFoundGitRemoteHttps: String? = null
    var notFoundGitRemoteHttpsCause: String? = null
    var notFoundGitRemoteHttpsSolution: String? = null
    var notFoundGitRemoteHttpsWiki: String? = null
    var invalidRefSpec: String? = null
    var invalidRefSpecCause: String? = null
    var invalidRefSpecSolution: String? = null
    var invalidRefSpecWiki: String? = null
    var notPermissionGetOauthToken: String? = null
    var notPermissionGetOauthTokenCause: String? = null
    var notPermissionGetOauthTokenSolution: String? = null
    var notPermissionGetOauthTokenWiki: String? = null
    var notExistCredential: String? = null
    var notExistCredentialCause: String? = null
    var notExistCredentialSolution: String? = null
    var notExistCredentialWiki: String? = null
    var notExistRepository:String?=null
    var notExistRepositoryCause: String? = null
    var notExistRepositorySolution: String? = null
    var notExistRepositoryWiki: String? = null

    companion object {
        fun get(): GitErrorsText {
            return lookupBundle(Locale.SIMPLIFIED_CHINESE, GitErrorsText::class.java)
        }
    }
}
