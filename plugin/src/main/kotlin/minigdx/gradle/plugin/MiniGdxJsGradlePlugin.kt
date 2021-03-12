package minigdx.gradle.plugin

import minigdx.gradle.plugin.internal.MiniGdxPlatform
import minigdx.gradle.plugin.internal.checkCommonPlugin
import minigdx.gradle.plugin.internal.createDir
import org.gradle.api.Plugin
import org.gradle.api.Project

class MiniGdxJsGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.checkCommonPlugin(MiniGdxPlatform.JAVASCRIPT)

        configureMiniGdxDependencies(project)

        project.createDir("src/jsMain/kotlin")
        project.createDir("src/jsTest/kotlin")
    }

    private fun configureMiniGdxDependencies(project: Project) {
        project.afterEvaluate {
            project.dependencies.add("commonMainImplementation", "com.github.minigdx:minigdx:DEV-SNAPSHOT")
            project.dependencies.add("jsMainImplementation", "com.github.minigdx:minigdx-js:DEV-SNAPSHOT")
        }
    }
}
