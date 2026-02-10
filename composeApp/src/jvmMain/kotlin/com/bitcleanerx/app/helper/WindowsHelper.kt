package com.bitcleanerx.app.helper

import java.util.concurrent.TimeUnit

/**
 * Windows-specific helper functions
 */
object WindowsHelper {

    /**
     * Open a file or folder in Windows Explorer
     */
    fun openInExplorer(path: String) {
        try {
            Runtime.getRuntime().exec(arrayOf("explorer.exe", path))
        } catch (e: Exception) {
            println("Error opening in Explorer: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Calculate directory size using PowerShell (Windows)
     * This is much faster than using Java NIO for large directories
     */
    fun calculateSizeUsingPowerShell(path: String): Long {
        return try {
            val command = listOf(
                "powershell.exe",
                "-Command",
                "(Get-ChildItem -Path \"$path\" -Recurse -Force -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum"
            )

            val process = ProcessBuilder(command)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()

            val output = process.inputStream.bufferedReader().readLine()?.trim()
            process.waitFor()

            if (output == null) return -1L
            output.toLongOrNull() ?: -1L
        } catch (e: Exception) {
            -1L
        }
    }
}

