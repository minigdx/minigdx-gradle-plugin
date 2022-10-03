package com.github.minigdx.gradle.plugin

import com.github.dwursteisen.gltf.Format
import com.github.dwursteisen.gltf.GltfExtensions
import com.github.minigdx.gradle.plugin.android.MockLibraryExtension
import com.github.minigdx.gradle.plugin.internal.CommonConfiguration.configureProjectRepository
import com.github.minigdx.gradle.plugin.internal.SdkHelper
import com.github.minigdx.gradle.plugin.internal.assertsDirectory
import com.github.minigdx.gradle.plugin.internal.createDir
import com.github.minigdx.gradle.plugin.internal.maybeCreateExtension
import com.github.minigdx.gradle.plugin.tasks.PrintVersionTask
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType

class MiniGdxCommonGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val minigdx = project.maybeCreateExtension(MiniGdxExtension::class.java)

        project.createDir("src/commonMain/kotlin")
        project.createDir("src/commonMain/resources")
        project.createDir("src/jvmMain/kotlin")
        project.createDir("src/jvmTest/kotlin")
        project.createDir("src/jsMain/kotlin")
        project.createDir("src/jsTest/kotlin")
        project.createDir("src/androidMain/kotlin")
        project.createDir("src/androidTest/kotlin")

        configureProjectRepository(project)
        configureMiniGdxGltfPlugin(project)
        configure(project)
        configureDependencies(project, minigdx)

        project.afterEvaluate {
            val commonConfiguration = project.configurations.findByName("commonMainApiDependenciesMetadata")
            val jvmConfiguration = project.configurations.findByName("jvmCompileClasspath")
            project.tasks.register("version", PrintVersionTask::class.java) {
                this.classpath.from(jvmConfiguration?.resolve() ?: emptySet<Unit>())
                this.classpath.from(commonConfiguration?.resolve() ?: emptySet<Unit>())
            }
        }
    }

    private fun configureDependencies(project: Project, minigdx: MiniGdxExtension) {
        project.dependencies.add(
            "commonMainApi",
            minigdx.version.map { version ->
                // Set the dependency as API so there is nothing to configure about dependencies
                // on platforms modules
                "com.github.minigdx:minigdx:$version"
            }
        )
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
            val preBuildTask =
                project.tasks.withType(DefaultTask::class.java).findByName("preBuild")
            preBuildTask?.inputs?.file(project.tasks.named("gltf").get().outputs)
        }
    }

    private fun isAndroidDetected(project: Project): Boolean {
        return SdkHelper.hasAndroid(project.rootDir)
    }

    fun configure(project: Project) {
        val androidDetected = isAndroidDetected(project)
        if (androidDetected) {
            project.plugins.apply("com.android.library")
        } else {
            project.extensions.create("android", MockLibraryExtension::class.java, project)
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
                    this.optIn("kotlin.ExperimentalStdlibApi")
                    this.optIn("kotlinx.serialization.ExperimentalSerializationApi")
                }
            }

            project.tasks.withType(ProcessResources::class.java).named("jvmProcessResources")
                .configure {
                    from(project.tasks.named("gltf"))
                }
        }
    }
}
