import org.jetbrains.compose.desktop.application.dsl.TargetFormat

group = "com.bitcleanerx"
version = "1.4.0"

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinx.serialization)
}

val generateBuildConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/source/buildConfig/jvmMain/kotlin")
    val appVersion = project.version.toString()

    inputs.property("appVersion", appVersion)
    outputs.dir(outputDir)

    doLast {
        val packageDir = outputDir.get().asFile.resolve("com/bitcleanerx/app")
        packageDir.mkdirs()
        packageDir.resolve("BuildConfig.kt").writeText(
            """
            package com.bitcleanerx.app

            object BuildConfig {
                const val APP_VERSION = "$appVersion"
            }
            """.trimIndent() + "\n"
        )
    }
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kaml)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.coil.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain {
            kotlin.srcDir(generateBuildConfig)
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutinesSwing)
            }
        }
    }
}


compose.desktop {
    application {
        mainClass = "com.bitcleanerx.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "BitCleanerX"
            packageVersion = version.toString()
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            copyright = "2026 Paulo Coutinho. All rights reserved."
            vendor = "Paulo Coutinho"
            licenseFile.set(project.file("../LICENSE.txt"))
            modules(
                "jdk.unsupported"
            )

            windows {
                dirChooser = true
                menuGroup = "BitCleanerX"
                iconFile.set(project.file("src/jvmMain/resources/icons/app.ico"))
            }

            macOS {
                iconFile.set(project.file("src/jvmMain/resources/icons/app.icns"))
                bundleID = "com.bitcleanerx.app"

                signing {
                    val providers = project.providers
                    sign.set(true)
                    identity.set(providers.environmentVariable("SIGNING_IDENTITY"))
                }

                notarization {
                    val providers = project.providers
                    appleID.set(providers.environmentVariable("NOTARIZATION_APPLE_ID"))
                    teamID.set(providers.environmentVariable("NOTARIZATION_TEAM_ID"))
                    password.set(providers.environmentVariable("NOTARIZATION_PASSWORD"))
                }
            }

            linux {
                iconFile.set(project.file("src/jvmMain/resources/icons/app.png"))
            }
        }
    }
}
