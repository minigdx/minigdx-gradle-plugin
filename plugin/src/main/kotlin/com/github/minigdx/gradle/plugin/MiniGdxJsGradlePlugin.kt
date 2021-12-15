package com.github.minigdx.gradle.plugin

import com.github.minigdx.gradle.plugin.internal.CommonConfiguration.configureProjectRepository
import com.github.minigdx.gradle.plugin.internal.maybeCreateExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import java.io.File

class MiniGdxJsGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
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

        configureMiniGdxDependencies(project, project.maybeCreateExtension(MiniGdxExtension::class.java))

        configureTasks(project)
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

    private fun configureMiniGdxDependencies(project: Project, minigdx: MiniGdxExtension) {
        // Create custom configuration that unpack depdencies on  the js platform
        project.configurations.create("minigdxToUnpack") {
            setTransitive(false)
            attributes {
                attribute(Attribute.of("org.gradle.usage", String::class.java), "kotlin-runtime")
            }
        }

        project.afterEvaluate {
            project.dependencies.add(
                "minigdxToUnpack",
                "com.github.minigdx:minigdx-js:${minigdx.version.get()}"
            )
        }
    }
}
