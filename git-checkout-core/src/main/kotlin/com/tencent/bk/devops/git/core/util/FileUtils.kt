package com.tencent.bk.devops.git.core.util

import com.tencent.bk.devops.git.core.enums.OSType
import com.tencent.bk.devops.plugin.script.CommandLineUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files

object FileUtils {

    private val logger = LoggerFactory.getLogger(FileUtils::class.java)

    fun deleteRepositoryFile(repositoryPath: String) {
        val repositoryFile = File(repositoryPath)
        repositoryFile.listFiles()?.forEach {
            try {
                logger.info("delete the file: ${it.canonicalPath}")
                forceDelete(it)
            } catch (ignore: Exception) {
                logger.error("delete file fail: ${it.canonicalPath}, ${ignore.message} (${it.exists()}")
                if (AgentEnv.getOS() != OSType.WINDOWS) {
                    CommandLineUtils.execute(
                        command = "rm -rf ${it.canonicalPath}",
                        workspace = repositoryFile,
                        print2Logger = true
                    )
                }
            }
        }
    }

    private fun forceDelete(file: File) {
        if (file.isDirectory) {
            deleteDirectory(file)
        } else {
            val filePresent = file.exists()
            if (!file.delete()) {
                if (!filePresent) {
                    throw FileNotFoundException("File does not exist: $file")
                }
                val message = "Unable to delete file: $file"
                throw IOException(message)
            }
        }
    }

    fun deleteDirectory(directory: File) {
        if (!directory.exists()) {
            return
        }

        if (!Files.isSymbolicLink(directory.toPath())) {
            cleanDirectory(directory)
        }
        if (!directory.delete()) {
            val message = "Unable to delete directory $directory."
            throw IOException(message)
        }
    }

    private fun cleanDirectory(directory: File) {
        val files = verifiedListFiles(directory)

        var exception: IOException? = null
        for (file in files) {
            try {
                forceDelete(file)
            } catch (ioe: IOException) {
                exception = ioe
            }
        }
        if (null != exception) {
            throw exception
        }
    }

    @SuppressWarnings("ThrowsCount")
    private fun verifiedListFiles(directory: File): Array<File> {
        if (!directory.exists()) {
            val message = "$directory does not exist"
            throw IllegalArgumentException(message)
        }
        if (!directory.isDirectory) {
            val message = "$directory is not a directory"
            throw IllegalArgumentException(message)
        }
        return directory.listFiles()
            ?: throw IOException("Failed to list contents of $directory")
    }
}
