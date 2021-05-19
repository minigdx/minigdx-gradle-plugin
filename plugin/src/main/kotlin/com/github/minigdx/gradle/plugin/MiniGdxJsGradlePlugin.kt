package com.github.minigdx.gradle.plugin

import com.github.minigdx.gradle.plugin.internal.MiniGdxPlatform
import com.github.minigdx.gradle.plugin.internal.checkCommonPlugin
import com.github.minigdx.gradle.plugin.internal.createDir
import com.github.minigdx.gradle.plugin.internal.maybeCreateMiniGdxExtension
import com.github.minigdx.gradle.plugin.internal.minigdx
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip

class MiniGdxJsGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.maybeCreateMiniGdxExtension()
        project.checkCommonPlugin(MiniGdxPlatform.JAVASCRIPT)

        configureMiniGdxDependencies(project)

        project.createDir("src/jsMain/kotlin")
        project.createDir("src/jsTest/kotlin")

        configureTasks(project)
    }

    private fun configureTasks(project: Project) {
        project.tasks.register("runJs") {
            it.group = "minigdx"
            it.description = "Run your game in your browser."
            it.dependsOn("gltf", "browserDevelopmentRun")
        }

        project.tasks.register("bundle-js", Zip::class.java) {
            it.group = "minigdx"
            it.description = "Create a bundle as zip."
            it.dependsOn("gltf", "jsBrowserDistribution")
            it.from(project.buildDir.resolve("distributions"))
            it.destinationDirectory.set(project.buildDir.resolve("minigdx"))
            it.doLast {
                project.logger.lifecycle("[MINIGDX] The js distribution of your game is available at: ${it.outputs.files.first()}")
            }
        }

        // Copy resources from minigdx into the web distribution so it's available, as the web platform
        // can't access bundled resources.
        val copy = project.tasks.register("unpack-minigdx-resources", Copy::class.java) {
            val dependencies = project.configurations.getAt("minigdxToUnpack")

            it.group = "minigdx"
            it.description = "Unpack resources used by minigdx needed by the web platform."
            it.from(project.zipTree(dependencies.singleFile))
            it.include("/internal/**")
            it.into("build/processedResources/js/main")
        }
        project.afterEvaluate {
            project.tasks.getByName("jsProcessResources").dependsOn(copy)
        }
    }

    private fun configureMiniGdxDependencies(project: Project) {
        project.configurations.create("minigdxToUnpack") {
            it.setTransitive(false)
            it.attributes {
                it.attribute(Attribute.of("org.gradle.usage", String::class.java), "kotlin-runtime")
            }
        }

        project.afterEvaluate {
            project.dependencies.add("commonMainImplementation", "com.github.minigdx:minigdx:${project.minigdx.version.get()}")
            project.dependencies.add("jsMainImplementation", "com.github.minigdx:minigdx-js:${project.minigdx.version.get()}")
            project.dependencies.add("minigdxToUnpack", "com.github.minigdx:minigdx-js:${project.minigdx.version.get()}")
        }
    }
}
