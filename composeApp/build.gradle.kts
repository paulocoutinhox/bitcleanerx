import org.jetbrains.compose.desktop.application.dsl.TargetFormat

group = "com.bitcleanerx"
version = "1.0.0"

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    jvm()
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kaml)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.coil.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
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
            copyright = "2025 Paulo Coutinho. All rights reserved."
            vendor = "Paulo Coutinho"
            licenseFile.set(project.file("LICENSE.txt"))
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
