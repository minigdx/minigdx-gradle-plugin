package com.github.minigdx.gradle.plugin

import org.gradle.testkit.runner.GradleRunner
import org.junit.Ignore
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class MiniGdxJvmGradlePluginFunctionalTest {

    @Test
    fun `can run task`() {
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
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("build")
            .withProjectDir(projectDir)
            .build()
    }

    @Test
    fun `can print versions`() {
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
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("version")
            .withProjectDir(projectDir)
            .build()
        assertTrue(result.output.contains("minigdx dependencies"))
    }

    @Test
    fun `can print versions with configuration cache`() {
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
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("--configuration-cache", "version")
            .withProjectDir(projectDir)
            .build()

        val buildResult = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("--configuration-cache", "version")
            .withProjectDir(projectDir)
            .build()

        assertTrue(buildResult.output.contains("Reusing configuration cache."))
    }

    @Test
    fun `can run task multiplatform`() {
        val projectDir = prepareMultiplatformProject()

        // Run the build
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("build", "bundleJar", "bundleJs")
            .withProjectDir(projectDir)
            .build()
    }

    @Ignore("Kotlin MPP is not supporting configuration cache yet.")
    @Test
    fun `can run task multiplatform with configuration cache`() {
        val projectDir = prepareMultiplatformProject()

        // Run the build
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("--configuration-cache", "build", "bundleJar", "bundleJs")
            .withProjectDir(projectDir)
            .build()

        val buildResult = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("--configuration-cache", "build", "bundleJar", "bundleJs")
            .withProjectDir(projectDir)
            .build()

        assertTrue(buildResult.output.contains("Reusing configuration cache."))
    }

    private fun prepareMultiplatformProject(): File {
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
        return projectDir
    }
}
