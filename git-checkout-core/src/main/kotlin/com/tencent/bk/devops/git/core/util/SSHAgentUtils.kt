package com.tencent.bk.devops.git.core.util

import com.tencent.bk.devops.git.core.enums.OSType
import com.tencent.bk.devops.git.core.exception.ParamInvalidException
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.ExecuteWatchdog
import org.apache.commons.exec.LogOutputStream
import org.apache.commons.exec.PumpStreamHandler
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.TimeUnit

@SuppressWarnings("TooManyFunctions")
class SSHAgentUtils {

    companion object {
        private const val AUTH_SOCKET_VAR = "SSH_AUTH_SOCK"
        private const val AGENT_PID_VAR = "SSH_AGENT_PID"
        private const val AUTH_SOCKET_VAR2 = "SSH2_AUTH_SOCK"
        private const val AGENT_PID_VAR2 = "SSH2_AGENT_PID"
        private val logger = LoggerFactory.getLogger(SSHAgentUtils::class.java)
    }

    fun addIdentity(privateKey: String, passPhrase: String?): Map<String, String> {
        var keyFile: File? = null
        var askPass: File? = null
        var sshAgentFile: File? = null
        var sshAddFile: File? = null
        try {
            if (AgentEnv.getOS() == OSType.WINDOWS) {
                sshAgentFile = createWindowsSshAgent()

                keyFile = File.createTempFile("private_key_", ".key")
                keyFile.writeText(privateKey)

                sshAddFile = createWindowsSshAddFile(keyFile)

                askPass = if (passPhrase != null) createWindowsAskpassScript() else null
            } else {
                sshAgentFile = createUnixSshAgent()

                val perms = PosixFilePermissions.fromString("rw-------")
                val fileAttributes = PosixFilePermissions
                    .asFileAttribute(perms)
                keyFile = Files.createTempFile("private_key_", ".key", fileAttributes).toFile()
                keyFile.writeText(privateKey)

                sshAddFile = createUnixSshAddFile(keyFile)

                askPass = if (passPhrase != null) createUnixAskpassScript() else null
            }
            val agentEnv = parseAgentEnv(executeCommand(sshAgentFile.absolutePath, null))
            val env = HashMap(agentEnv)
            if (passPhrase != null) {
                env["SSH_PASSPHRASE"] = passPhrase
                env["DISPLAY"] = ":0" // just to force using SSH_ASKPASS
                env["SSH_ASKPASS"] = askPass!!.absolutePath
            }
            val output = executeCommand(sshAddFile.absolutePath, env)
            logger.info("Finish add the ssh-agent - (${output.trim()})")
            if (agentEnv.isNotEmpty()) {
                EnvHelper.addSshAgent(agentEnv)
            }
            return agentEnv
        } catch (ignored: Throwable) {
            logger.warn("Fail to add the ssh key to ssh-agent", ignored)
        } finally {
            deleteTempFile(sshAgentFile)
            deleteTempFile(sshAddFile)
            deleteTempFile(keyFile)
            deleteTempFile(askPass)
        }

        return mapOf()
    }

    fun stop(sshAgentPid: String) {
        var sshAgentFile: File? = null
        try {
            sshAgentFile = if (AgentEnv.getOS() == OSType.WINDOWS) {
                createWindowsStop()
            } else {
                createUnixStop()
            }
            executeCommand(sshAgentFile.absolutePath, mapOf(AGENT_PID_VAR to sshAgentPid))
        } catch (ignored: Throwable) {
            logger.warn("Fail to stop ssh-agent ${ignored.message}")
        } finally {
            deleteTempFile(sshAgentFile)
        }
    }

    private fun createWindowsSshAgent(): File {
        val sshAgentFile = File.createTempFile("ssh-agent", ".bat")
        sshAgentFile.setExecutable(true, true)
        sshAgentFile.writeText("@echo off\n\"${getWindowsSshExecutable("ssh-agent.exe")}\"")
        return sshAgentFile
    }

    private fun createWindowsSshAddFile(keyFile: File): File {
        val sshAddFile = File.createTempFile("ssh-add-", ".bat")
        sshAddFile.setExecutable(true, true)
        sshAddFile.writeText("@echo off\n\"${getWindowsSshExecutable("ssh-add.exe")}\" " +
            "\"${keyFile.absolutePath}\"")
        return sshAddFile
    }

    private fun createWindowsAskpassScript(): File {
        val askpass = File.createTempFile("askpass_", ".bat")
        logger.info("Create the askpass file(${askpass.absolutePath})")
        askpass.writeText("@echo off\n@echo %SSH_PASSPHRASE%")
        askpass.setExecutable(true, true)
        return askpass
    }

    private fun createUnixSshAgent(): File {
        val sshAgentFile = File.createTempFile("ssh-agent", ".sh")
        sshAgentFile.setExecutable(true, true)
        sshAgentFile.writeText("#!/bin/sh\nssh-agent")
        return sshAgentFile
    }

    private fun createUnixSshAddFile(keyFile: File): File {
        val sshAddFile = File.createTempFile("ssh-add-", ".sh")
        sshAddFile.setExecutable(true, true)
        sshAddFile.writeText("#!/bin/sh\nssh-add ${keyFile.absolutePath}")
        return sshAddFile
    }

    /**
     * Creates a self-deleting script for SSH_ASKPASS. Self-deleting to be able to detect a wrong passphrase.
     */
    private fun createUnixAskpassScript(): File {
        val perms = PosixFilePermissions.fromString("rwx------")
        val fileAttributes = PosixFilePermissions
            .asFileAttribute(perms)
        val askpass = Files.createTempFile("askpass_", ".sh", fileAttributes).toFile()
        logger.info("Create the askpass file(${askpass.absolutePath})")
        askpass.writeText("#!/bin/sh\necho \"\$SSH_PASSPHRASE\"\nrm \"$0\"\n")
        executeCommand("chmod +x ${askpass.absolutePath}", null)
        return askpass
    }

    private fun createWindowsStop(): File {
        val sshAgentFile = File.createTempFile("ssh-agent-stop-", ".bat")
        sshAgentFile.setExecutable(true, true)
        sshAgentFile.writeText("@echo off\n\"${getWindowsSshExecutable("ssh-agent.exe")}\" -k")
        return sshAgentFile
    }

    private fun createUnixStop(): File {
        val sshAgentFile = File.createTempFile("ssh-agent-stop-", ".sh")
        sshAgentFile.setExecutable(true, true)
        sshAgentFile.writeText("#!/bin/sh\nssh-agent -k")
        return sshAgentFile
    }

    private fun deleteTempFile(tempFile: File?) {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete()
        }
    }

    @SuppressWarnings("MagicNumber")
    private fun executeCommand(commandLine: String, env: Map<String, String>?): String {
        val cmdLine = CommandLine.parse(commandLine)
        val executor = DefaultExecutor()
        val output = StringBuilder()
        val outputStream = object : LogOutputStream() {
            override fun processLine(line: String?, level: Int) {
                output.append(line).append("\n")
            }
        }
        executor.streamHandler = PumpStreamHandler(outputStream)
        executor.watchdog = ExecuteWatchdog(TimeUnit.MINUTES.toMillis(5))
        try {
            val exitCode = executor.execute(cmdLine, env)
            if (exitCode != 0) {
                logger.warn(
                    "Fail to execute the command($commandLine) because of exitCode($exitCode) and output($output)"
                )
            }
        } catch (ignore: Throwable) {
            logger.warn("Error message(${output.trim()})", ignore)
        }
        return output.toString()
    }

    /**
     * Parses ssh-agent output.
     */
    private fun parseAgentEnv(agentOutput: String): Map<String, String> {
        val env = HashMap<String, String>()

        if (agentOutput.contains("SSH2_")) {
            // get SSH_AUTH_SOCK
            val socketValue = getAgentValue(agentOutput, AUTH_SOCKET_VAR2)
            env[AUTH_SOCKET_VAR] = socketValue
            env[AUTH_SOCKET_VAR2] = socketValue
            logger.info(AUTH_SOCKET_VAR + "=" + env[AUTH_SOCKET_VAR])
            logger.info(AUTH_SOCKET_VAR2 + "=" + env[AUTH_SOCKET_VAR2])

            // get SSH_AGENT_PID
            val pidValue = getAgentValue(agentOutput, AGENT_PID_VAR2)
            env[AGENT_PID_VAR] = pidValue
            env[AGENT_PID_VAR2] = pidValue
            logger.info(AGENT_PID_VAR + "=" + env[AGENT_PID_VAR])
            logger.info(AGENT_PID_VAR2 + "=" + env[AGENT_PID_VAR2])
        } else {
            // get SSH_AUTH_SOCK
            env[AUTH_SOCKET_VAR] = getAgentValue(agentOutput, AUTH_SOCKET_VAR)
            logger.info(AUTH_SOCKET_VAR + "=" + env[AUTH_SOCKET_VAR])

            // get SSH_AGENT_PID
            env[AGENT_PID_VAR] = getAgentValue(agentOutput, AGENT_PID_VAR)
            logger.info(AGENT_PID_VAR + "=" + env[AGENT_PID_VAR])
        }

        return env
    }

    /**
     * Parses a value from ssh-agent output.
     */
    private fun getAgentValue(agentOutput: String, envVar: String): String {
        val pos = agentOutput.indexOf(envVar) + envVar.length + 1 // +1 for '='
        val end = agentOutput.indexOf(';', pos)
        return agentOutput.substring(pos, end)
    }

    @SuppressWarnings("ReturnCount")
    private fun getWindowsSshExecutable(cmd: String): File {
        // First check the GIT_SSH environment variable
        var sshexe = getFileFromEnv("GIT_SSH", "")
        if (sshexe != null && sshexe.exists()) {
            return sshexe
        }

        sshexe = getFileFromEnv("ProgramFiles", "\\Git\\bin\\$cmd")
        if (sshexe != null && sshexe.exists()) {
            return sshexe
        }
        sshexe = getFileFromEnv("ProgramFiles", "\\Git\\usr\\bin\\$cmd")
        if (sshexe != null && sshexe.exists()) {
            return sshexe
        }
        sshexe = getFileFromEnv("ProgramFiles(x86)", "\\Git\\bin\\$cmd")
        if (sshexe != null && sshexe.exists()) {
            return sshexe
        }
        sshexe = getFileFromEnv("ProgramFiles(x86)", "\\Git\\usr\\bin\\$cmd")
        if (sshexe != null && sshexe.exists()) {
            return sshexe
        }
        throw ParamInvalidException(
            errorMsg = "ssh executable not found. " +
                "The plugin only supports official git client http://git-scm.com/download/win"
        )
    }

    private fun getFileFromEnv(envVar: String, suffix: String): File? {
        val envValue = System.getenv(envVar) ?: return null
        return File(envValue + suffix)
    }
}
