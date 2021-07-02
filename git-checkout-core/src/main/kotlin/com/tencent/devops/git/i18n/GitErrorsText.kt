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

package com.tencent.devops.git.i18n

import java.util.Locale

class GitErrorsText : TranslationBundle() {
    var sshKeyAuditUnverified: String? = null
    var sshAuthenticationFailed: String? = null
    var sshPermissionDenied: String? = null
    var httpsAuthenticationFailed: String? = null
    var remoteDisconnection: String? = null
    var hostDown: String? = null
    var rebaseConflicts: String? = null
    var mergeConflicts: String? = null
    var httpsRepositoryNotFound: String? = null
    var sshRepositoryNotFound: String? = null
    var projectNotFound: String? = null
    var branchDeletionFailed: String? = null
    var revertConflicts: String? = null
    var noMatchingRemoteBranch: String? = null
    var noExistingRemoteBranch: String? = null
    var noSubmoduleMapping: String? = null
    var submoduleRepositoryDoesNotExist: String? = null
    var invalidSubmoduleSHA: String? = null
    var invalidMerge: String? = null
    var branchAlreadyExists: String? = null
    var noMatchingBranch: String? = null
    var badRevision: String? = null
    var notAGitRepository: String? = null
    var cannotMergeUnrelatedHistories: String? = null
    var lfsAttributeDoesNotMatch: String? = null
    var errorDownloadingObject: String? = null
    var invalidObjectName: String? = null
    var lockFileAlreadyExists: String? = null
    var localChangesOverwritten: String? = null
    var unresolvedConflicts: String? = null
    var configLockFileAlreadyExists: String? = null
    var remoteAlreadyExists: String? = null
    var tagAlreadyExists: String? = null
    var mergeWithLocalChanges: String? = null
    var noInitializeBranch: String? = null
    var emptyAccessToken: String? = null
    var emptyRepositoryHashId: String? = null
    var emptyRepositoryName: String? = null

    companion object {
        fun get(): GitErrorsText {
            return lookupBundle(Locale.getDefault(), GitErrorsText::class.java)
        }
    }
}
