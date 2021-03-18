package com.github.minigdx.gradle.plugin

import com.github.minigdx.gradle.plugin.internal.MiniGdxPlatform
import com.github.minigdx.gradle.plugin.internal.checkCommonPlugin
import com.github.minigdx.gradle.plugin.internal.createDir
import com.github.minigdx.gradle.plugin.internal.maybeCreateMiniGdxExtension
import com.github.minigdx.gradle.plugin.internal.minigdx
import org.gradle.api.Plugin
import org.gradle.api.Project

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
            it.dependsOn("gltf", "jsBrowserDevelopmentRun")
        }

        project.tasks.register("bundle-js") {
            it.group = "minigdx"
            it.description = "Create a bundle as zip."
            it.dependsOn("gltf", "jsBrowserProductionWebpack")
        }
    }

    private fun configureMiniGdxDependencies(project: Project) {
        project.afterEvaluate {
            project.dependencies.add("commonMainImplementation", "com.github.minigdx:minigdx:${project.minigdx.version.get()}")
            project.dependencies.add("jsMainImplementation", "com.github.minigdx:minigdx-js:${project.minigdx.version.get()}")
        }
    }
}
