package com.github.minigdx.gradle.plugin

import com.github.minigdx.gradle.plugin.internal.CommonConfiguration.configureProjectRepository
import com.github.minigdx.gradle.plugin.internal.MiniGdxException
import com.github.minigdx.gradle.plugin.internal.Severity
import com.github.minigdx.gradle.plugin.internal.Solution
import com.github.minigdx.gradle.plugin.internal.createDir
import com.github.minigdx.gradle.plugin.internal.maybeCreateExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar
import java.io.File

class MiniGdxJvmGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val minigdx = project.maybeCreateExtension(JvmConfiguration::class.java)

        project.apply { plugin("org.jetbrains.kotlin.jvm") }

        configureSourceSets(project)
        configureProjectRepository(project)

        project.afterEvaluate {
            configureTasks(project, minigdx)
        }
        // TODO:
        //   configure build chain to get java 11 and jpackage
        //   https://walczak.it/blog/distributing-javafx-desktop-applications-without-requiring-jvm-using-jlink-and-jpackage
    }

    private fun configureSourceSets(project: Project) {
        project.createDir("src/main/kotlin")
        project.createDir("src/test/kotlin")
    }

    private fun configureTasks(project: Project, minigdx: JvmConfiguration) {
        project.apply { plugin("org.gradle.application") }

        project.tasks.register("runJvm", JavaExec::class.java) {
            checkMainClass(project, minigdx)
            group = "minigdx"
            description = "Run your game on the JVM."
            if (isMacOs()) {
                jvmArgs = listOf("-XstartOnFirstThread")
            }
            workingDir = project.projectDir.resolve(File("../common/src/commonMain/resources"))
            mainClass.set(minigdx.mainClass)
            classpath = project.files(
                project.tasks.getByName("compileKotlin").outputs,
                project.tasks.getByName("compileJava").outputs,
                project.configurations.getByName("runtimeClasspath")
            )
        }

        project.tasks.register("bundleJar", Jar::class.java) {
            checkMainClass(project, minigdx)
            group = "minigdx"
            description = "Create a bundle as a Fat "
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            archiveFileName.set("${project.rootProject.name}-jvm.jar")
            manifest {
                attributes(mapOf("Main-Class" to (minigdx.mainClass.get())))
            }

            from(project.projectDir.resolve(File("../common/src/commonMain/resources")))
            from(project.tasks.named("compileKotlin"))
            from(project.tasks.named("compileJava"))
            val dependenciesJar = project.configurations.getByName("runtimeClasspath").files
            val flatClasses = dependenciesJar.filter { it.exists() }
                .map { deps ->
                    if (deps.isDirectory) {
                        project.fileTree(deps)
                    } else {
                        project.zipTree(deps)
                    }
                }
            from(flatClasses)
            destinationDirectory.set(project.buildDir.resolve("minigdx"))
            doLast {
                project.logger.lifecycle("[MINIGDX] The jar distribution of your game is available at: ${outputs.files.first()}")
            }
        }
    }

    private fun checkMainClass(project: Project, minigdx: JvmConfiguration) {
        if (!minigdx.mainClass.isPresent) {
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

    private fun isMacOs(): Boolean {
        val osName = System.getProperty("os.name")?.lowercase()
        val index = osName?.indexOf("mac") ?: -1
        return index >= 0
    }
}
