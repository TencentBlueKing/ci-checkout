package com.tencent.bk.devops.git.core.util

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files

object FileUtils {

    fun forceDelete(file: File) {
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

    private fun deleteDirectory(directory: File) {
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
