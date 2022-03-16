package com.github.minigdx.gradle.plugin.internal

import org.gradle.api.Project
import java.net.URI

object CommonConfiguration {
    internal fun configureProjectRepository(project: Project) {
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
}
