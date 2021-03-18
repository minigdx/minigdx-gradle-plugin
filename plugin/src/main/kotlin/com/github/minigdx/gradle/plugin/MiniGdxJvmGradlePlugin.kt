/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package com.github.minigdx.gradle.plugin

import com.github.minigdx.gradle.plugin.internal.MiniGdxException
import com.github.minigdx.gradle.plugin.internal.MiniGdxPlatform
import com.github.minigdx.gradle.plugin.internal.Severity
import com.github.minigdx.gradle.plugin.internal.Solution
import com.github.minigdx.gradle.plugin.internal.assertsDirectory
import com.github.minigdx.gradle.plugin.internal.checkCommonPlugin
import com.github.minigdx.gradle.plugin.internal.createDir
import com.github.minigdx.gradle.plugin.internal.maybeCreateMiniGdxExtension
import com.github.minigdx.gradle.plugin.internal.minigdx
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar

/**
 * A simple 'hello world' plugin.
 */
class MiniGdxJvmGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.maybeCreateMiniGdxExtension()
        project.checkCommonPlugin(MiniGdxPlatform.JVM)

        configureSourceSets(project)

        project.afterEvaluate {
            configureMiniGdxDependencies(project, it.minigdx)
            configureTasks(project, it.minigdx)
        }
        // TODO:
        //   configure task to create a templated game
        //   configure task to run application
        //   configure task to bundle fat jar
        //   configure task to bundle app+jvm ?
        //   configure pipeline pour github ?
        //   configure build chain to get java 11 and jpackage
        //   https://walczak.it/blog/distributing-javafx-desktop-applications-without-requiring-jvm-using-jlink-and-jpackage
    }

    private fun configureSourceSets(project: Project) {
        project.createDir("src/jvmMain/kotlin")
        project.createDir("src/jvmTest/kotlin")
    }

    private fun configureMiniGdxDependencies(project: Project, minigdx: MiniGdxExtension) {
        project.afterEvaluate {
            project.dependencies.add(
                "jvmMainImplementation",
                "com.github.minigdx:minigdx-jvm:${minigdx.version.get()}"
            )
        }
    }

    private fun configureTasks(project: Project, minigdx: MiniGdxExtension) {
        project.apply { it.plugin("org.gradle.application") }

        project.tasks.register("runJvm", JavaExec::class.java) {
            checkMainClass(project, minigdx)
            it.group = "minigdx"
            it.description = "Run your game on the JVM."
            it.jvmArgs = listOf("-XstartOnFirstThread")
            it.workingDir = project.assertsDirectory()
            it.mainClass.set(minigdx.jvm.mainClass)
            it.classpath = project.files(
                project.buildDir.resolve("classes/kotlin/jvm/main"),
                project.configurations.getByName("jvmRuntimeClasspath")
            )
        }

        project.tasks.register("bundle-jar", Jar::class.java) { jar ->
            checkMainClass(project, minigdx)
            jar.group = "minigdx"
            jar.description = "Create a bundle as a Fat jar."

            jar.archiveFileName.set("${project.rootProject.name}-jvm.jar")
            jar.manifest { m ->
                m.attributes(mapOf("Main-Class" to (project.minigdx.jvm.mainClass.get())))
            }

            jar.from(project.assertsDirectory())
            jar.from(project.buildDir.resolve("classes/kotlin/jvm/main"))
            val dependenciesJar = project.configurations.getByName("jvmRuntimeClasspath").files
            val flatClasses = dependenciesJar.filter { it.exists() }
                .map { deps ->
                    if (deps.isDirectory) {
                        project.fileTree(deps)
                    } else {
                        project.zipTree(deps)
                    }
                }
            jar.from(flatClasses)
            jar.dependsOn("jvmJar", "gltf")
        }
    }

    private fun checkMainClass(project: Project, minigdx: MiniGdxExtension) {
        if (!minigdx.jvm.mainClass.isPresent) {
            throw MiniGdxException.create(
                severity = Severity.EASY,
                project = project,
                because = "The main class used to start the game is not configured.",
                description = "MiniGDX needs the name of the class that will start the game to configure how to run it.",
                solutions = listOf(
                    Solution(
                        description =
                            """Add the configuration of the main class in your gradle build script:
                            | minigdx {
                            |   jvm.mainClass.set("com.example.MainKt")
                            | 
                            | }
                        """.trimMargin()

                    )
                )
            )
        }
    }
}
