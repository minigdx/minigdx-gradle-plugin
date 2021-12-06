package com.github.minigdx.gradle.plugin

import com.github.minigdx.gradle.plugin.internal.maybeCreateMiniGdxExtension
import com.github.minigdx.gradle.plugin.internal.minigdx
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import java.io.File
import java.net.URI

class MiniGdxJsGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.maybeCreateMiniGdxExtension()

        project.apply { plugin("org.jetbrains.kotlin.js") }

        configureProjectRepository(project)
        project.extensions.configure<KotlinJsProjectExtension>("kotlin") {
            js(IR) {
                browser()
                nodejs()
                binaries.executable()
            }

            sourceSets.maybeCreate("main").resources.srcDir(project.projectDir.resolve(File("../common/src/commonMain/resources")))
        }

        configureMiniGdxDependencies(project)

        configureTasks(project)
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

    private fun configureTasks(project: Project) {
        project.tasks.register("runJs") {
            group = "minigdx"
            description = "Run your game in your browser."
            dependsOn("browserDevelopmentRun")
        }

        project.tasks.register("bundleJs", Zip::class.java) {
            group = "minigdx"
            description = "Create a bundle as zip."
            from(project.tasks.named("browserDistribution"))
            destinationDirectory.set(project.buildDir.resolve("minigdx"))
            doLast {
                project.logger.lifecycle("[MINIGDX] The js distribution of your game is available at: ${outputs.files.first()}")
            }
        }

        // Copy resources from minigdx into the web distribution so it's available, as the web platform
        // can't access bundled resources.
        val copy = project.tasks.register("unpack-minigdx-resources", Copy::class.java) {
            val dependencies = project.configurations.getAt("minigdxToUnpack")

            group = "minigdx"
            description = "Unpack resources used by minigdx needed by the web platform."
            from(dependencies.map { project.zipTree(it) })
            include("/internal/**")
            into("build/processedResources/js/main")
        }
        project.afterEvaluate {
            project.tasks.getByName("processResources").dependsOn(copy)
        }
    }

    private fun configureMiniGdxDependencies(project: Project) {
        // Create custom configuration that unpack depdencies on  the js platform
        project.configurations.create("minigdxToUnpack") {
            setTransitive(false)
            attributes {
                attribute(Attribute.of("org.gradle.usage", String::class.java), "kotlin-runtime")
            }
        }

        project.afterEvaluate {
            project.dependencies.add("implementation", "com.github.minigdx:minigdx:${project.minigdx.version.get()}")
            project.dependencies.add("implementation", "com.github.minigdx:minigdx-js:${project.minigdx.version.get()}")
            project.dependencies.add(
                "minigdxToUnpack",
                "com.github.minigdx:minigdx-js:${project.minigdx.version.get()}"
            )
        }
    }
}
