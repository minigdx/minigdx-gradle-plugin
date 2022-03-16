package com.github.minigdx.gradle.plugin.internal

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

fun <T> Project.maybeCreateExtension(extensionType: Class<T>): T {
    val extension = project.extensions.findByType(extensionType)
    return extension ?: project.extensions.create("minigdx", extensionType, project)
}
