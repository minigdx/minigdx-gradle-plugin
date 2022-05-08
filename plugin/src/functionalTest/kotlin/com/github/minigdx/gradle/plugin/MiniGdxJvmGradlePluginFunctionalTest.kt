package com.github.minigdx.gradle.plugin

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.test.Test

class MiniGdxJvmGradlePluginFunctionalTest {
    @Test fun `can run task`() {
        // Setup the test build
        val projectDir = File("build/functionalTest")
        projectDir.mkdirs()
        projectDir.resolve("settings.gradle").writeText("")
        projectDir.resolve("build.gradle").writeText(
            """
            plugins {
                id('com.github.minigdx.common')
            }
        """
        )

        // Run the build
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("build")
        runner.withProjectDir(projectDir)
        runner.build()
    }

    @Test fun `can run task multiplatform`() {
        // Setup the test build
        val projectDir = File("build/functionalTest/")
        projectDir.mkdirs()
        projectDir.resolve("common").mkdirs()
        projectDir.resolve("jvm").mkdirs()
        projectDir.resolve("js").mkdirs()
        projectDir.resolve("local.properties").writeText(
            """minigdx.android.enabled=false"""
        )
        projectDir.resolve("settings.gradle").writeText(
            """
            include("common", "jvm", "js")
            """.trimIndent()
        )
        projectDir.resolve("common/build.gradle").writeText(
            """
            plugins {
                id('com.github.minigdx.common')
            }
            
            android {
                defaultConfig {
                    minSdkVersion(13)
                }
        
                compileSdkVersion(29)
                buildToolsVersion = "29.0.3"
        
                sourceSets.getByName("main") {
                    manifest.srcFile("src/androidMain/AndroidManifest.xml")
                    assets.srcDirs("src/commonMain/resources")
                }
        
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_1_8
                    targetCompatibility = JavaVersion.VERSION_1_8
                }
            }
        """
        )

        projectDir.resolve("jvm/build.gradle").writeText(
            """
            plugins {
                id('com.github.minigdx.jvm')
            }
            
            dependencies {
               implementation(project(":common"))
            }
            
            minigdx {
               mainClass.set("your.game.Main")
            }
        """
        )

        projectDir.resolve("js/build.gradle").writeText(
            """
            plugins {
                id('com.github.minigdx.js')
            }
            
            dependencies {
                implementation(project(":common"))
            }
        """
        )
        // Run the build
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("build")
        runner.withProjectDir(projectDir)
        runner.build()
    }
}