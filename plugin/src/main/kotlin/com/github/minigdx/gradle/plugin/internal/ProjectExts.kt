package com.github.minigdx.gradle.plugin.internal

import com.github.minigdx.gradle.plugin.MiniGdxExtension
import org.gradle.api.Project
import java.io.File

fun Project.hasPlatforms(vararg platforms: MiniGdxPlatform): Boolean {
    return platforms.all { this.plugins.hasPlugin(it.pluginId) }
}

fun Project.platforms(): Set<MiniGdxPlatform> {
    return MiniGdxPlatform.values()
        .filter { this.hasPlatforms(it) }
        .toSet()
}

fun Project.hasCommonPlugin(): Boolean {
    return this.plugins.hasPlugin("com.github.minigdx.common")
}

fun Project.checkCommonPlugin(platform: MiniGdxPlatform) {
    if (hasCommonPlugin()) {
        MiniGdxException.create(
            severity = Severity.EASY,
            project = this,
            because = "MiniGDX Common plugin is already declared. It should be the last declared plugin.",
            description = "When plugins are declared, the MiniGDX common plugin needs to be declared after all " +
                "platform MiniGDX plugins.",
            solutions = listOf(
                Solution(
                    """Re-order MiniGDX plugins by putting the MiniGDX Common plugin after platform plugins:
                        | plugins {
                        |    id("${platform.pluginId}") <-- Put this platform plugin before the common plugin 
                        |    id("com.github.minigdx.common")
                        | 
                        | }
                    """.trimMargin()
                )
            )
        )
    }
}

fun Project.createDir(directoryName: String): Boolean {
    return if (!this.file(directoryName).exists()) {
        this.file(directoryName).mkdirs()
    } else {
        false
    }
}

fun Project.assertsDirectory(): File {
    // FIXME: it will not work with Android
    return this.projectDir.resolve("src/commonMain/resources")
}

/**
 * Helper to access the extension.
 * Do NOT call this method too early as the extension might not be configured by the user.
 */
val Project.minigdx: MiniGdxExtension
    get() {
        return this.extensions.getByType(MiniGdxExtension::class.java)
    }

fun Project.maybeCreateMiniGdxExtension() {
    if(project.extensions.findByType(MiniGdxExtension::class.java) == null) {
        project.extensions.create("minigdx", MiniGdxExtension::class.java, project)
    }
}
