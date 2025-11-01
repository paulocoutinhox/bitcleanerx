package com.bitcleanerx.app.helper

/**
 * Unix/Linux-specific helper functions
 */
object UnixHelper {

    /**
     * Open a file or folder in the default file manager (Linux)
     * Uses xdg-open which works across most Linux desktop environments
     */
    fun openInFileManager(path: String) {
        try {
            Runtime.getRuntime().exec(arrayOf("xdg-open", path))
        } catch (e: Exception) {
            println("Error opening in file manager: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Calculate directory size using the 'du' command (Unix/Linux/macOS)
     * This is much faster than using Java NIO for large directories
     */
    fun calculateSizeUsingDu(path: String): Long {
        return try {
            // Use 'du -skP' to get size in kilobytes without following symlinks
            // -s: summarize (display only total)
            // -k: use 1024-byte blocks
            // -P: do not follow symbolic links (explicit, though it's the default)
            val process = ProcessBuilder("du", "-skP", path)
                .redirectErrorStream(true)
                .start()

            process.waitFor()

            if (process.exitValue() != 0) {
                return -1L
            }

            val output = process.inputStream.bufferedReader().readLine() ?: return -1L
            // Output format: "12345\t/path/to/dir"
            val sizeInKB = output.split("\t", " ")[0].toLongOrNull() ?: return -1L

            // Convert KB to bytes
            sizeInKB * 1024
        } catch (e: Exception) {
            -1L
        }
    }
}

