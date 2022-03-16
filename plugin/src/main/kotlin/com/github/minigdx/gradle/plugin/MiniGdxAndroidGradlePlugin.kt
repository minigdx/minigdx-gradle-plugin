package com.github.minigdx.gradle.plugin

import com.github.minigdx.gradle.plugin.internal.CommonConfiguration.configureProjectRepository
import com.github.minigdx.gradle.plugin.internal.MiniGdxException
import com.github.minigdx.gradle.plugin.internal.Severity
import com.github.minigdx.gradle.plugin.internal.Solution
import org.gradle.api.Plugin
import org.gradle.api.Project

class MiniGdxAndroidGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        if (project.extensions.findByName("android") == null) {
            throw MiniGdxException.create(
                severity = Severity.EASY,
                project = project,
                because = "The android plugin is not added before the minigdx plugin.",
                description = "MiniGDX needs the Android plugin before being applied.",
                solutions = listOf(
                    Solution(
                        description =
                            """Add the the android plugin before the minigdx plugin:
                            | plugin {
                            |   id("com.android.application")
                            |   id("com.github.minigdx.android")
                            | }
                        """.trimMargin()

                    )
                )
            )
        }
        project.apply { plugin("org.jetbrains.kotlin.android") }

        configureProjectRepository(project)
    }
}
