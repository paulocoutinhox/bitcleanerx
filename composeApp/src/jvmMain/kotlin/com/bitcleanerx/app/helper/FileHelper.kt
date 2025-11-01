package com.bitcleanerx.app.helper

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

/**
 * Generic file helper that coordinates file operations across different operating systems
 * Delegates OS-specific operations to appropriate helpers
 */
object FileHelper {

    private val osName = System.getProperty("os.name").lowercase()
    private val isWindows = osName.contains("win")
    private val isMacOS = osName.contains("mac") || osName.contains("darwin")
    private val isUnix = osName.contains("nix") || osName.contains("nux") || isMacOS

    /**
     * Calculate the total size of a directory
     * Uses native OS commands for better performance, falls back to Java NIO if needed
     */
    fun calculateDirectorySize(path: String): Long {
        return try {
            val expandedPath = expandPath(path)
            val file = File(expandedPath)
            if (!file.exists()) return 0L

            // Try using native OS commands first (much faster for large directories)
            val systemSize = calculateSizeUsingNativeCommand(expandedPath)
            if (systemSize >= 0) {
                println("Calculated size for $path using native command: ${formatSize(systemSize)}")
                return systemSize
            }

            // Fallback to efficient Java NIO method
            println("Using Java NIO for $path")
            calculateSizeNIO(file.toPath())
        } catch (e: Exception) {
            println("Error calculating size for $path: ${e.message}")
            0L
        }
    }

    private fun calculateSizeUsingNativeCommand(path: String): Long {
        return when {
            isUnix -> UnixHelper.calculateSizeUsingDu(path)
            isWindows -> WindowsHelper.calculateSizeUsingPowerShell(path)
            else -> -1L
        }
    }

    private fun calculateSizeNIO(path: Path): Long {
        return try {
            if (!Files.exists(path)) return 0L

            // Ignore symbolic links
            if (Files.isSymbolicLink(path)) {
                return 0L
            }

            if (path.isRegularFile()) {
                path.fileSize()
            } else if (path.isDirectory()) {
                var totalSize = 0L

                // Use Files.walk which is more efficient than listFiles
                Files.walk(path)
                    .use { stream ->
                        stream.forEach { file ->
                            try {
                                // Skip symbolic links
                                if (!Files.isSymbolicLink(file) && file.isRegularFile()) {
                                    totalSize += file.fileSize()
                                }
                            } catch (e: Exception) {
                                // Ignore errors for individual files
                            }
                        }
                    }

                totalSize
            } else {
                0L
            }
        } catch (e: Exception) {
            println("Error in NIO calculation: ${e.message}")
            0L
        }
    }

    /**
     * Delete a file or directory recursively
     * Returns true if deletion was successful, false otherwise
     */
    fun deleteFileOrDirectory(path: String): Boolean {
        return try {
            val file = File(expandPath(path))
            if (!file.exists()) return false
            file.deleteRecursively()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Expand tilde (~) in paths to the user's home directory
     */
    fun expandPath(path: String): String {
        return path.replace("~", System.getProperty("user.home"))
    }

    /**
     * Format byte size to human-readable format (B, KB, MB, GB, TB)
     */
    fun formatSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return "%.2f %s".format(size, units[unitIndex])
    }

    /**
     * Get all subdirectories of a given path
     * Returns empty list if path doesn't exist or is not a directory
     * Ignores symbolic links to prevent infinite loops and unwanted traversal
     */
    fun getSubFolders(path: String): List<File> {
        return try {
            val file = File(expandPath(path))
            if (!file.exists() || !file.isDirectory) return emptyList()
            
            // Filter out symbolic links and only return real directories
            file.listFiles()
                ?.filter { it.isDirectory && !Files.isSymbolicLink(it.toPath()) }
                ?.toList() 
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Open a file or folder in the system's default file manager
     * Delegates to OS-specific helpers
     */
    fun openInFileManager(path: String) {
        val expandedPath = expandPath(path)

        when {
            isMacOS -> MacosHelper.openInFinder(expandedPath)
            isWindows -> WindowsHelper.openInExplorer(expandedPath)
            isUnix -> UnixHelper.openInFileManager(expandedPath)
            else -> println("Unsupported OS for opening file manager")
        }
    }

    /**
     * Resolve specific folder names for better user experience
     * Delegates to OS-specific helpers to get human-readable names
     * Example: Xcode simulator UUIDs become "iPhone 15 Pro (iOS 17.0)"
     */
    fun resolveFolderName(path: String, originalName: String): String {
        val expandedPath = expandPath(path)

        // Delegate to macOS-specific helper
        if (isMacOS) {
            val resolvedName = MacosHelper.resolveFolderName(expandedPath, originalName)
            if (resolvedName != originalName) return resolvedName
        }

        // Add more OS-specific helpers here in the future
        // if (isWindows) { WindowsHelper.resolveFolderName(...) }
        // if (isLinux) { LinuxHelper.resolveFolderName(...) }

        return originalName
    }
}

