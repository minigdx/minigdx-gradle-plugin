package com.github.minigdx.gradle.plugin

import org.gradle.api.Project

class JvmConfiguration(project: Project) {

    /**
     * Main class used to start the JVM version of the game.
     */
    val mainClass = project.objects.property(String::class.java)
}

open class MiniGdxExtension(project: Project) {

    val version = project.objects.property(String::class.java)
        .value("LATEST-SNAPSHOT")

    val jvm = JvmConfiguration(project)
}


