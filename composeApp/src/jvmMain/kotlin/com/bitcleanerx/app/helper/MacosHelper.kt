package com.bitcleanerx.app.helper

import java.io.File

/**
 * MacOS-specific helper functions
 */
object MacosHelper {

    /**
     * Open a file or folder in Finder (macOS)
     */
    fun openInFinder(path: String) {
        try {
            Runtime.getRuntime().exec(arrayOf("open", path))
        } catch (e: Exception) {
            println("Error opening in Finder: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Resolve macOS-specific folder names
     */
    fun resolveFolderName(path: String, originalName: String): String {
        // Check if it's an Xcode simulator device folder
        if (path.contains("/Library/Developer/CoreSimulator/Devices/")) {
            val simulatorName = resolveXcodeSimulatorName(path)
            if (simulatorName != null) return simulatorName
        }

        // Add more macOS-specific folder resolvers here in the future
        // Example: Xcode DerivedData, iOS backups, etc.

        return originalName
    }

    /**
     * Resolve Xcode simulator device name from device.plist
     *
     * Example path: ~/Library/Developer/CoreSimulator/Devices/[UUID]/
     * Reads device.plist to extract: name, platform (iOS/tvOS/watchOS/visionOS), and version
     *
     * Returns: "iPhone 15 Pro (iOS 17.0)" or "Apple Vision Pro (visionOS 2.0)"
     */
    private fun resolveXcodeSimulatorName(devicePath: String): String? {
        return try {
            val plistFile = File(devicePath, "device.plist")
            if (!plistFile.exists()) return null

            val plistContent = plistFile.readText()

            // Parse device name
            val name = extractPlistValue(plistContent, "name")

            // Parse runtime to extract platform and version
            val runtime = extractPlistValue(plistContent, "runtime")
            val (platform, version) = parseSimulatorRuntime(runtime)

            // Format: "iPhone 15 Pro (iOS 17.0)"
            if (name != null && platform != null && version != null) {
                "$name ($platform $version)"
            } else if (name != null) {
                name
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error parsing device.plist: ${e.message}")
            null
        }
    }

    /**
     * Extract a value from a plist XML file
     *
     * Parses simple key-value pairs in Apple's plist XML format
     */
    private fun extractPlistValue(plistContent: String, key: String): String? {
        return try {
            // Find the key
            val keyPattern = "<key>$key</key>"
            val keyIndex = plistContent.indexOf(keyPattern)
            if (keyIndex == -1) return null

            // Get the next value after the key
            val afterKey = plistContent.substring(keyIndex + keyPattern.length)

            // Match string value
            val stringPattern = Regex("<string>([^<]+)</string>")
            val match = stringPattern.find(afterKey)

            match?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse simulator runtime string to extract platform and version
     *
     * Examples:
     * - "com.apple.CoreSimulator.SimRuntime.iOS-17-0" -> ("iOS", "17.0")
     * - "com.apple.CoreSimulator.SimRuntime.xrOS-2-0" -> ("visionOS", "2.0")
     * - "com.apple.CoreSimulator.SimRuntime.tvOS-17-2" -> ("tvOS", "17.2")
     * - "com.apple.CoreSimulator.SimRuntime.watchOS-10-0" -> ("watchOS", "10.0")
     */
    private fun parseSimulatorRuntime(runtime: String?): Pair<String?, String?> {
        if (runtime == null) return Pair(null, null)

        return try {
            // Pattern: SimRuntime.PLATFORM-MAJOR-MINOR
            val pattern = Regex("SimRuntime\\.([^-]+)-(\\d+)-(\\d+)")
            val match = pattern.find(runtime)

            if (match != null) {
                val platform = match.groupValues[1]
                val majorVersion = match.groupValues[2]
                val minorVersion = match.groupValues[3]

                // Map platform names to user-friendly names
                val friendlyPlatform = when (platform) {
                    "iOS" -> "iOS"
                    "tvOS" -> "tvOS"
                    "watchOS" -> "watchOS"
                    "xrOS" -> "visionOS"  // xrOS is internally called xrOS but displayed as visionOS
                    else -> platform
                }

                val version = "$majorVersion.$minorVersion"
                Pair(friendlyPlatform, version)
            } else {
                Pair(null, null)
            }
        } catch (e: Exception) {
            Pair(null, null)
        }
    }
}

