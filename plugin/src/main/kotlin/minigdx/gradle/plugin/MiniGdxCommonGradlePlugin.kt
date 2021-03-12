package minigdx.gradle.plugin

import com.github.dwursteisen.gltf.Format
import com.github.dwursteisen.gltf.GltfExtensions
import com.github.minigdx.gradle.plugin.internal.MiniGdxException
import com.github.minigdx.gradle.plugin.internal.Severity
import com.github.minigdx.gradle.plugin.internal.Solution
import minigdx.gradle.plugin.internal.BuildReporter
import minigdx.gradle.plugin.internal.MiniGdxPlatform
import minigdx.gradle.plugin.internal.createDir
import minigdx.gradle.plugin.internal.hasPlatforms
import minigdx.gradle.plugin.internal.platforms
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class MiniGdxCommonGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        if (project.platforms().isEmpty()) {
            throw MiniGdxException.create(
                severity = Severity.EASY,
                project = project,
                because = "No MiniGDX platform has been found.",
            description = "When the MiniGDX common plugin has been applied, no platform were found. " +
                "It might be because you forgot to declare it or because you declare it in a wrong order.",
                solutions = listOf(
                    Solution(
                        description = """Add a platform plugin before the common plugin:
                            | plugins {
                            |    id("com.github.minigdx.jvm") <-- A platform needs to be declared before the common plugin
                            |    id("com.github.minigdx.common")
                            | 
                            | }
                        """.trimMargin()
                    ),
                    Solution(
                        description = """Declare platforms need to be declare before the common plugin:
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

        configureDependencies(project)
        configureMiniGdxGltfPlugin(project)
        configure(project)
    }

    private fun configureDependencies(project: Project) {
        project.afterEvaluate {
            // TODO: put the version of MiniGDX in an Gradle Extension
            project.dependencies.add("commonMainImplementation", "com.github.minigdx:minigdx:DEV-SNAPSHOT")
        }
    }

    private fun configureMiniGdxGltfPlugin(project: Project) {
        project.apply { it.plugin("com.github.minigdx.gradle.plugin.gltf") }

        project.extensions.configure<NamedDomainObjectContainer<GltfExtensions>>("gltfPlugin") {
            it.register("assetsSource") {
                it.format.set(Format.PROTOBUF)
                it.gltfDirectory.set(project.file("src/commonMain/assetsSource"))
                it.target.set(project.file("src/commonMain/resources")) // FIXME: it will not work with Android
            }
        }

        project.createDir("src/commonMain/assetsSource")
    }

    fun configure(project: Project) {
        project.apply { it.plugin("org.jetbrains.kotlin.multiplatform") }
        project.extensions.configure<KotlinMultiplatformExtension>("kotlin") { mpp ->
            if (project.hasPlatforms(MiniGdxPlatform.JVM)) {
                mpp.jvm {
                    this.compilations.getByName("main").kotlinOptions.jvmTarget = "1.8"
                    this.compilations.getByName("test").kotlinOptions.jvmTarget = "1.8"
                }
            }

            if (project.hasPlatforms(MiniGdxPlatform.JAVASCRIPT)) {
                mpp.js {
                    this.useCommonJs()
                    this.browser {
                        this.webpackTask {
                            this.compilation.kotlinOptions {
                                this.sourceMap = true
                                this.sourceMapEmbedSources = "always"
                                this.freeCompilerArgs += listOf("-Xopt-in=kotlin.ExperimentalStdlibApi")
                            }
                        }
                    }
                    this.nodejs
                }
            }

            mpp.sourceSets.apply {
                getByName("commonMain") {
                    it.dependencies {
                        implementation(kotlin("stdlib-common"))
                    }
                }

                getByName("commonTest") {
                    it.dependencies {
                        implementation(kotlin("test-common"))
                        implementation(kotlin("test-annotations-common"))
                    }
                }

                if (project.hasPlatforms(MiniGdxPlatform.JVM)) {
                    getByName("jvmMain") {
                        it.dependencies {
                            implementation(kotlin("stdlib-jdk8"))
                        }
                    }

                    getByName("jvmTest") {
                        it.dependencies {
                            implementation(kotlin("test-junit"))
                        }
                    }
                }

                if (project.hasPlatforms(MiniGdxPlatform.JAVASCRIPT)) {
                    getByName("jsMain") {
                        it.dependencies {
                            implementation(kotlin("stdlib-js"))
                        }
                    }

                    getByName("jsTest") {
                        it.dependencies {
                            implementation(kotlin("test-js"))
                        }
                    }
                }
            }
        }
    }
}
