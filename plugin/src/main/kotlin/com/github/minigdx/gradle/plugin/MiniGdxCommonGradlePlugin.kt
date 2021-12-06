package com.github.minigdx.gradle.plugin

import com.android.build.gradle.LibraryExtension
import com.github.dwursteisen.gltf.Format
import com.github.dwursteisen.gltf.GltfExtensions
import com.github.minigdx.gradle.plugin.internal.SdkHelper
import com.github.minigdx.gradle.plugin.internal.assertsDirectory
import com.github.minigdx.gradle.plugin.internal.createDir
import com.github.minigdx.gradle.plugin.internal.maybeCreateMiniGdxExtension
import com.github.minigdx.gradle.plugin.internal.minigdx
import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.kotlin.dsl.withType
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import java.net.URI

class MiniGdxCommonGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.maybeCreateMiniGdxExtension()

        project.createDir("src/commonMain/kotlin")
        project.createDir("src/commonMain/resources")
        project.createDir("src/jvmMain/kotlin")
        project.createDir("src/jvmTest/kotlin")
        project.createDir("src/jsMain/kotlin")
        project.createDir("src/jsTest/kotlin")
        project.createDir("src/androidMain/kotlin")
        project.createDir("src/androidTest/kotlin")

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
            project.dependencies.add(
                "commonMainApi",
                "com.github.minigdx:minigdx:${project.minigdx.version.get()}"
            )
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

        project.afterEvaluate {
            project.tasks.withType(DefaultTask::class.java).named("preBuild").configure {
                this.inputs.file(project.tasks.named("gltf").get().outputs)
            }
        }
    }

    private fun isAndroidDetected(project: Project): Boolean {
        return SdkHelper.hasAndroid(project.rootDir)
    }

    fun configure(project: Project) {

        val androidDetected = isAndroidDetected(project)
        if (androidDetected) {
            project.plugins.apply("com.android.library")

            project.extensions.configure<LibraryExtension>("android") {
                compileSdkVersion(29)
                buildToolsVersion = "29.0.3" // FIXME: configure that using extension method?
                defaultConfig {
                    minSdkVersion(13)
                }
                sourceSets.getByName("main") {
                    manifest.srcFile("src/androidMain/AndroidManifest.xml")
                    assets.srcDirs("src/commonMain/resources")
                }

                packagingOptions {
                    exclude("META-INF/DEPENDENCIES")
                    exclude("META-INF/LICENSE")
                    exclude("META-INF/LICENSE.txt")
                    exclude("META-INF/license.txt")
                    exclude("META-INF/NOTICE")
                    exclude("META-INF/NOTICE.txt")
                    exclude("META-INF/notice.txt")
                    exclude("META-INF/ASL2.0")
                    exclude("META-INF/*.kotlin_module")
                }

                // Configure only for each module that uses Java 8
                // language features (either in its source code or
                // through dependencies).
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_1_8
                    targetCompatibility = JavaVersion.VERSION_1_8
                }
            }
        }

        project.apply { plugin("org.jetbrains.kotlin.multiplatform") }

        project.extensions.configure<KotlinMultiplatformExtension>("kotlin") {
            jvm {
                this.compilations.getByName("main").kotlinOptions.jvmTarget = "1.8"
                this.compilations.getByName("test").kotlinOptions.jvmTarget = "1.8"
            }

            js(KotlinJsCompilerType.IR) {
                this.binaries.library()
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

            if (androidDetected) {
                android {
                    publishLibraryVariants("release", "debug")
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

                if (androidDetected) {
                    getByName("androidMain") {
                        dependencies {
                            implementation(kotlin("stdlib-jdk8"))
                        }
                    }

                    getByName("androidTest") {
                        dependencies {
                            implementation(kotlin("test-junit"))
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

            project.tasks.withType(ProcessResources::class.java).named("jvmProcessResources").configure {
                from(project.tasks.named("gltf"))
            }
        }
    }
}
