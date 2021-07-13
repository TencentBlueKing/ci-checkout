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

package com.tencent.bk.devops.git.core.service

import com.tencent.bk.devops.git.core.constant.GitConstants
import com.tencent.bk.devops.git.core.constant.GitConstants.GCM_INTERACTIVE
import com.tencent.bk.devops.git.core.constant.GitConstants.GIT_CREDENTIAL_HELPER
import com.tencent.bk.devops.git.core.constant.GitConstants.GIT_LFS_SKIP_SMUDGE
import com.tencent.bk.devops.git.core.constant.GitConstants.GIT_TERMINAL_PROMPT
import com.tencent.bk.devops.git.core.constant.GitConstants.GIT_TRACE
import com.tencent.bk.devops.git.core.enums.GitConfigScope
import com.tencent.bk.devops.git.core.enums.OSType
import com.tencent.bk.devops.git.core.exception.GitExecuteException
import com.tencent.bk.devops.git.core.exception.RetryException
import com.tencent.bk.devops.git.core.pojo.CommitLogInfo
import com.tencent.bk.devops.git.core.pojo.GitOutput
import com.tencent.bk.devops.git.core.service.helper.RetryHelper
import com.tencent.bk.devops.git.core.util.AgentEnv
import com.tencent.bk.devops.git.core.util.CommandUtil
import com.tencent.bk.devops.git.core.util.EnvHelper
import com.tencent.bk.devops.git.core.util.RegexUtil
import com.tencent.devops.git.log.LogType
import java.io.File
import org.slf4j.LoggerFactory

@Suppress("ALL")
class GitCommandManager(
    private val workingDirectory: File,
    lfs: Boolean = false
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GitCommandManager::class.java)
    }

    private val gitEnv = mutableMapOf(
        GIT_TERMINAL_PROMPT to "0",
        GCM_INTERACTIVE to "Never"
    )

    init {
        if (lfs) {
            gitEnv[GIT_LFS_SKIP_SMUDGE] = "1"
        }
    }

    fun getGitVersion(): String {
        return execGit(listOf("--version")).stdOut
    }

    fun tryGetFetchUrl(): String {
        val output = execGit(
            args = listOf("config", "--local", "--get", "remote.origin.url"),
            allowAllExitCodes = true
        )
        if (output.exitCode != 0) {
            return ""
        }
        val stdout = output.stdOut.trim()
        if (stdout.contains("\n")) {
            return ""
        }
        return stdout
    }

    fun tryClean(): Boolean {
        val output = execGit(args = listOf("clean", "-ffd"), allowAllExitCodes = true)
        return output.exitCode == 0
    }

    fun tryReset(commit: String): Boolean {
        val output = execGit(args = listOf("reset", "--hard", commit), allowAllExitCodes = true)
        return output.exitCode == 0
    }

    fun init() {
        execGit(args = listOf("init", workingDirectory.canonicalPath))
    }

    fun remoteAdd(remoteName: String, remoteUrl: String) {
        execGit(args = listOf("remote", "add", remoteName, remoteUrl))
    }

    fun remoteSetUrl(remoteName: String, remoteUrl: String) {
        execGit(args = listOf("remote", "set-url", remoteName, remoteUrl))
    }

    fun remoteRemove(remoteName: String) {
        execGit(args = listOf("remote", "remove", remoteName), allowAllExitCodes = true)
    }

    fun remoteList() {
        execGit(args = listOf("remote", "-v"))
    }

    fun configExists(
        configKey: String,
        configValueRegex: String? = null,
        configScope: GitConfigScope = GitConfigScope.LOCAL,
        configFile: String? = null
    ): Boolean {
        val output = execGit(
            args = configArgs(
                configKey = configKey,
                configValue = configValueRegex,
                configScope = configScope,
                configFile = configFile,
                action = "--get"
            ),
            allowAllExitCodes = true
        )
        return output.exitCode == 0
    }

    fun tryConfigGet(
        configKey: String,
        configValueRegex: String? = null,
        configScope: GitConfigScope = GitConfigScope.LOCAL,
        configFile: String? = null
    ): String {
        val output = execGit(
            args = configArgs(
                configKey = configKey,
                configValue = configValueRegex,
                configScope = configScope,
                configFile = configFile,
                action = "--get"
            ),
            allowAllExitCodes = true
        )
        if (output.exitCode != 0) {
            return ""
        }
        return output.stdOut
    }

    fun tryConfigUnset(
        configKey: String,
        configScope: GitConfigScope = GitConfigScope.LOCAL,
        configFile: String? = null
    ): Boolean {
        val output = execGit(
            args = configArgs(
                configKey = configKey,
                configScope = configScope,
                configFile = configFile,
                action = "--unset-all"
            ),
            allowAllExitCodes = true
        )
        return output.exitCode == 0
    }

    fun config(
        configKey: String,
        configValue: String,
        configScope: GitConfigScope = GitConfigScope.LOCAL,
        configFile: String? = null
    ) {
        execGit(
            args = configArgs(
                configKey = configKey,
                configValue = configValue,
                configScope = configScope,
                configFile = configFile
            )
        )
    }

    fun configAdd(
        configKey: String,
        configValue: String,
        configScope: GitConfigScope = GitConfigScope.LOCAL,
        configFile: String? = null
    ) {
        execGit(
            args = configArgs(
                configKey = configKey,
                configValue = configValue,
                configScope = configScope,
                configFile = configFile,
                action = "--add"
            )
        )
    }

    private fun configArgs(
        configKey: String,
        configValue: String? = null,
        configScope: GitConfigScope = GitConfigScope.LOCAL,
        configFile: String? = null,
        action: String? = null
    ): List<String> {
        if (configScope == GitConfigScope.FILE && configFile.isNullOrBlank()) {
            throw IllegalArgumentException("config file can't be empty when config scope is file")
        }
        val args = mutableListOf("config", configScope.arg)
        if (!configFile.isNullOrBlank()) {
            args.add(configFile)
        }
        if (!action.isNullOrBlank()) {
            args.add(action)
        }
        args.add(configKey)
        if (!configValue.isNullOrBlank()) {
            args.add(configValue)
        }
        return args
    }

    fun tryDisableOtherGitHelpers(): Boolean {
        // windows执行git config --local credential.helper 不生效,git config --local credential.helper ""才生效
        val helperValue = if (AgentEnv.getOS() == OSType.WINDOWS) {
            "\"\""
        } else {
            ""
        }
        val output = execGit(
            args = listOf("config", "--local", GIT_CREDENTIAL_HELPER, helperValue),
            allowAllExitCodes = true
        )
        return output.exitCode == 0
    }

    fun setEnvironmentVariable(name: String, value: String) {
        gitEnv[name] = value
    }

    fun removeEnvironmentVariable(name: String) {
        gitEnv.remove(name)
    }

    fun submoduleSync(recursive: Boolean, path: String) {
        val args = mutableListOf("submodule", "sync")
        if (recursive) {
            args.add("--recursive")
        }
        if (path.isNotBlank()) {
            args.add(path)
        }
        execGit(args = args)
    }

    fun submoduleForeach(command: String, recursive: Boolean) {
        val args = mutableListOf("submodule", "foreach")
        if (recursive) {
            args.add("--recursive")
        }
        args.add(command)
        execGit(args = args, allowAllExitCodes = true)
    }

    fun submoduleForeach(command: List<String>, recursive: Boolean) {
        val args = mutableListOf("submodule", "foreach")
        if (recursive) {
            args.add("--recursive")
        }
        args.addAll(command)
        execGit(args = args, allowAllExitCodes = true)
    }

    fun submoduleUpdate(recursive: Boolean, path: String) {
        val args = mutableListOf("submodule", "update", "--init", "--force")
        if (recursive) {
            args.add("--recursive")
        }
        if (path.isNotBlank()) {
            args.add(path)
        }
        execGit(args = args)
    }

    fun fetch(refSpec: List<String>, fetchDepth: Int, remoteName: String, preMerge: Boolean) {
        val args = mutableListOf("fetch", "--prune", "--progress", "--no-recurse-submodules")
        if (fetchDepth > 0 && !preMerge) {
            args.add("--depth=$fetchDepth")
        } else if (File(File(workingDirectory, ".git"), "shallow").exists()) {
            args.add("--unshallow")
        }
        args.add(remoteName)
        args.addAll(refSpec)
        doFetch(args = args)
    }

    fun lfsPull() {
        doFetch(args = listOf("lfs", "pull"))
    }

    private fun doFetch(args: List<String>) {
        // add runtime env to git env
        gitEnv.putAll(EnvHelper.getAuthEnv())
        RetryHelper().execute {
            try {
                execGit(args = args, logType = LogType.PROGRESS)
            } catch (e: GitExecuteException) {
                if (e.errorCode == GitConstants.GIT_ERROR) {
                    gitEnv[GIT_TRACE] = "1"
                    throw RetryException(errorType = e.errorType, errorCode = e.errorCode, errorMsg = e.message!!)
                } else {
                    throw e
                }
            }
        }
        // 重试成功移除调试信息
        gitEnv.remove(GIT_TRACE)
    }

    fun lfsInstall() {
        execGit(args = listOf("lfs", "install", "--local"))
    }

    fun checkout(ref: String, startPoint: String) {
        val args = mutableListOf("checkout", "--force")
        if (startPoint.isBlank()) {
            args.add(ref)
        } else {
            args.addAll(listOf("-B", ref, startPoint))
        }
        execGit(args = args, logType = LogType.PROGRESS)
    }

    fun merge(ref: String) {
        execGit(args = listOf("merge", "--progress", ref))
    }

    fun log(maxCount: Int = 1, revisionRange: String = ""): List<CommitLogInfo> {
        val args = mutableListOf(
            "log",
            "-$maxCount",
            "--pretty=format:${GitConstants.GIT_LOG_FORMAT}",
            "--date=format:'%Y-%m-%d %H:%M:%S'"
        )
        if (revisionRange.isNotBlank()) {
            args.add(revisionRange)
        }

        val output = execGit(args = args, allowAllExitCodes = true)
        return output.stdOuts.mapNotNull { log ->
            RegexUtil.parseLog(log)
        }
    }

    fun commitExists(commit: String): Boolean {
        val args = listOf("rev-parse", "-q", "--verify", "$commit^{commit}")
        val output = execGit(args = args, allowAllExitCodes = true, printLogger = false)
        return output.stdOut.trim().isNotBlank()
    }

    fun branchExists(remote: Boolean, branchName: String): Boolean {
        val args = mutableListOf("branch", "--list")
        if (remote) {
            args.add("--remote")
        }
        args.add(branchName)
        val output = execGit(args = args)
        return output.stdOut.trim().isNotBlank()
    }

    fun headExists(): Boolean {
        val output = execGit(
            args = listOf("rev-parse", "--symbolic-full-name", "--verify", "--quiet", "HEAD"),
            allowAllExitCodes = true,
            printLogger = false
        )
        return output.stdOut.trim().isNotBlank()
    }

    private fun execGit(
        args: List<String>,
        allowAllExitCodes: Boolean = false,
        logType: LogType = LogType.TEXT,
        printLogger: Boolean = true
    ): GitOutput {
        return CommandUtil.execute(
            workingDirectory = workingDirectory,
            executable = "git",
            args = args,
            runtimeEnv = gitEnv,
            allowAllExitCodes = allowAllExitCodes,
            logType = logType,
            printLogger = printLogger
        )
    }
}
