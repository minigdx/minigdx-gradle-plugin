package com.github.minigdx.gradle.plugin

import com.github.dwursteisen.gltf.Format
import com.github.dwursteisen.gltf.GltfExtensions
import com.github.minigdx.gradle.plugin.internal.BuildReporter
import com.github.minigdx.gradle.plugin.internal.MiniGdxException
import com.github.minigdx.gradle.plugin.internal.MiniGdxPlatform
import com.github.minigdx.gradle.plugin.internal.Severity
import com.github.minigdx.gradle.plugin.internal.Solution
import com.github.minigdx.gradle.plugin.internal.assertsDirectory
import com.github.minigdx.gradle.plugin.internal.createDir
import com.github.minigdx.gradle.plugin.internal.hasPlatforms
import com.github.minigdx.gradle.plugin.internal.maybeCreateMiniGdxExtension
import com.github.minigdx.gradle.plugin.internal.minigdx
import com.github.minigdx.gradle.plugin.internal.platforms
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import java.net.URI

class MiniGdxCommonGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.maybeCreateMiniGdxExtension()
        if (project.platforms().isEmpty()) {
            throw MiniGdxException.create(
                severity = Severity.EASY,
                project = project,
                because = "No MiniGDX platform has been found.",
                description = "When the MiniGDX common plugin has been applied, no platform were found. " +
                    "It might be because you forgot to declare it or because you declare it in a wrong order.",
                solutions = listOf(
                    Solution(
                        description =
                            """Add a platform plugin before the common plugin:
                            | plugins {
                            |    id("com.github.minigdx.jvm") <-- A platform needs to be declared before the common plugin
                            |    id("com.github.minigdx.common")
                            | 
                            | }
                        """.trimMargin()
                    ),
                    Solution(
                        description =
                            """Declare platforms need to be declare before the common plugin:
                            | plugins {
                            |    id("com.github.minigdx.common")
                            |    id("com.github.minigdx.js") <-- Wrong! The declaration should be before the common declaration 
                            | }
                        """.trimMargin()
                    )
                )
            )
        }
        project.gradle.addBuildListener(BuildReporter(project))

        project.createDir("src/commonMain/kotlin")
        project.createDir("src/commonMain/resources")

        configureProjectRepository(project)
        configureDependencies(project)
        configureMiniGdxGltfPlugin(project)
        configure(project)
    }

    private fun configureProjectRepository(project: Project) {
        project.repositories.mavenCentral()
        project.repositories.google()
        // Snapshot repository. Select only our snapshot dependencies
        project.repositories.maven {
            url = URI("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        }.mavenContent {
            includeVersionByRegex("com.github.minigdx", "(.*)", "LATEST-SNAPSHOT")
            includeVersionByRegex("com.github.minigdx.(.*)", "(.*)", "LATEST-SNAPSHOT")
        }
        project.repositories.mavenLocal()
        // Will be deprecated soon... Required for dokka
        project.repositories.jcenter()
    }

    private fun configureDependencies(project: Project) {
        // Create custom configuration that unpack depdencies on  the js platform
        project.configurations.create("minigdxToUnpack") {
            setTransitive(false)
            attributes {
                attribute(Attribute.of("org.gradle.usage", String::class.java), "kotlin-runtime")
            }
        }
        project.afterEvaluate {
            project.dependencies.add("commonMainImplementation", "com.github.minigdx:minigdx:${project.minigdx.version.get()}")
        }
    }

    private fun configureMiniGdxGltfPlugin(project: Project) {
        project.apply { plugin("com.github.minigdx.gradle.plugin.gltf") }

        project.extensions.configure<NamedDomainObjectContainer<GltfExtensions>>("gltfPlugin") {
            register("assetsSource") {
                format.set(Format.PROTOBUF)
                gltfDirectory.set(project.file("src/commonMain/assetsSource"))
                target.set(project.assertsDirectory())
            }
        }

        project.createDir("src/commonMain/assetsSource")
    }

    fun configure(project: Project) {
        project.apply { plugin("org.jetbrains.kotlin.multiplatform") }
        project.extensions.configure<KotlinMultiplatformExtension>("kotlin") {
            if (project.hasPlatforms(MiniGdxPlatform.JVM)) {
                jvm {
                    this.compilations.getByName("main").kotlinOptions.jvmTarget = "1.8"
                    this.compilations.getByName("test").kotlinOptions.jvmTarget = "1.8"
                }
            }

            if (project.hasPlatforms(MiniGdxPlatform.JAVASCRIPT)) {
                js(KotlinJsCompilerType.IR) {
                    this.binaries.executable()
                    this.browser {
                        this.webpackTask {
                            this.compilation.kotlinOptions {
                                this.sourceMap = true
                                this.sourceMapEmbedSources = "always"
                                this.freeCompilerArgs += listOf("-Xopt-in=kotlin.ExperimentalStdlibApi")
                            }
                        }
                    }
                }
            }

            sourceSets.apply {
                getByName("commonMain") {
                    dependencies {
                        implementation(kotlin("stdlib-common"))
                    }
                }

                getByName("commonTest") {
                    dependencies {
                        implementation(kotlin("test-common"))
                        implementation(kotlin("test-annotations-common"))
                    }
                }

                if (project.hasPlatforms(MiniGdxPlatform.JVM)) {
                    getByName("jvmMain") {
                        dependencies {
                            implementation(kotlin("stdlib-jdk8"))
                        }
                    }

                    getByName("jvmTest") {
                        dependencies {
                            implementation(kotlin("test-junit"))
                        }
                    }
                }

                if (project.hasPlatforms(MiniGdxPlatform.JAVASCRIPT)) {
                    getByName("jsMain") {
                        dependencies {
                            implementation(kotlin("stdlib-js"))
                        }
                    }

                    getByName("jsTest") {
                        dependencies {
                            implementation(kotlin("test-js"))
                        }
                    }
                }
            }
            sourceSets.all {
                languageSettings.apply {
                    this.useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
                    this.useExperimentalAnnotation("kotlinx.serialization.ExperimentalSerializationApi")
                }
            }
        }
    }
}
