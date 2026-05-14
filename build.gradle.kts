import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.benManesVersions)
}

// reject unstable releases (alpha/beta/rc/m/dev/snapshot) when checking for updates
fun isStable(version: String): Boolean {
    val unstableKeyword = listOf("ALPHA", "BETA", "RC", "M", "DEV", "PREVIEW", "SNAPSHOT")
        .any { version.uppercase().contains(it) }
    val stableRegex = "^[0-9,.v-]+(-r)?$".toRegex()
    return !unstableKeyword && stableRegex.matches(version)
}

tasks.withType<DependencyUpdatesTask>().configureEach {
    rejectVersionIf {
        isStable(currentVersion) && !isStable(candidate.version)
    }
    gradleReleaseChannel = "current"
    checkForGradleUpdate = true
    outputFormatter = "plain,html"
    outputDir = layout.buildDirectory.dir("dependencyUpdates").get().asFile.path
    reportfileName = "report"
}
