package com.tencent.bk.devops.git.core.util

import com.tencent.bk.devops.git.core.exception.CompressException
import java.io.BufferedInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

@Suppress("NestedBlockDepth")
object CompressUtil {

    /**
     * 解压tar.gz文件
     * @param sourcePath 压缩文件路径
     * @param targetPath 解压到目标目录
     *
     */
    fun deCompressTarGzip(sourcePath: String, targetPath: String) {
        val source = Paths.get(sourcePath)
        val target = Paths.get(targetPath)

        if (Files.notExists(source)) {
            throw CompressException(errorMsg = "the compressed file does not exist")
        }
        Files.newInputStream(source).use { sourceInputStream ->
            BufferedInputStream(sourceInputStream).use { bi ->
                GzipCompressorInputStream(bi).use { gzi ->
                    TarArchiveInputStream(gzi).use { ti ->
                        var entry: ArchiveEntry?
                        while ((ti.nextEntry.also { entry = it }) != null) {
                            val newPath = zipSlipProtect(entry!!, target)
                            if (entry!!.isDirectory) {
                                Files.createDirectories(newPath)
                            } else {
                                val parent = newPath.parent
                                if (parent != null && Files.notExists(parent)) {
                                    Files.createDirectories(parent)
                                }
                                Files.copy(ti, newPath, StandardCopyOption.REPLACE_EXISTING)
                            }
                        }
                    }
                }
            }
        }
    }

    fun deCompressTar(sourcePath: String, targetPath: String) {
        val source = Paths.get(sourcePath)
        val target = Paths.get(targetPath)

        if (Files.notExists(source)) {
            throw CompressException(errorMsg = "the compressed file does not exist")
        }
        Files.newInputStream(source).use { sourceInputStream ->
            BufferedInputStream(sourceInputStream).use { bi ->
                TarArchiveInputStream(bi).use { ti ->
                    var entry: ArchiveEntry?
                    while ((ti.nextEntry.also { entry = it }) != null) {
                        val newPath = zipSlipProtect(entry!!, target)
                        if (entry!!.isDirectory) {
                            Files.createDirectories(newPath)
                        } else {
                            val parent = newPath.parent
                            if (parent != null && Files.notExists(parent)) {
                                Files.createDirectories(parent)
                            }
                            Files.copy(ti, newPath, StandardCopyOption.REPLACE_EXISTING)
                        }
                    }

                }
            }
        }
    }

    /**
     * 判断文件是否破坏
     */
    private fun zipSlipProtect(entry: ArchiveEntry, targetDir: Path): Path {
        val targetDirResolved = targetDir.resolve(entry.name)
        val normalizePath = targetDirResolved.normalize()
        if (!normalizePath.startsWith(targetDir)) {
            throw CompressException(errorMsg = "the compressed file has been corrupted: " + entry.name)
        }
        return normalizePath
    }
}
