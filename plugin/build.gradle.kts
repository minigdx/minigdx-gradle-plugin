@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    // Plugin publication plugin.
    alias(libs.plugins.gradle.publish)

    // Apply the Java Gradle plugin development plugin to add support for developing Gradle plugins
    `java-gradle-plugin`
    `kotlin-dsl`
    alias(libs.plugins.minigdx.jvm)
}

repositories {
    gradlePluginPortal()
    // Required to access Android Plugin
    google()
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

gradlePlugin.testSourceSets(functionalTestSourceSet)
configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
}

tasks.check {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}

dependencies {
    implementation(platform(libs.jdoctor.bom))
    implementation(libs.jdoctor.core)
    implementation(libs.jdoctor.utils)

    implementation(libs.mockito)?.because(
        "Mockito is used to mock the Android extension when the Android SDK is missing"
    )

    api(libs.android)

    api(libs.kotlin.plugin.mpp)
    api(libs.minigdx.plugin)

    testImplementation(gradleTestKit())
}

gradlePlugin {
    this.isAutomatedPublishing = false
    // Define the plugin
    val common by plugins.creating {
        id = "com.github.minigdx.common"
        implementationClass = "com.github.minigdx.gradle.plugin.MiniGdxCommonGradlePlugin"
        displayName = "MiniGDX Common plugin"
        description = "Configure your gradle project for creating a minigdx game."
    }

    val jvm by plugins.creating {
        id = "com.github.minigdx.jvm"
        implementationClass = "com.github.minigdx.gradle.plugin.MiniGdxJvmGradlePlugin"
        displayName = "MiniGDX Kotlin JVM plugin"
        description = "Configure your gradle project for creating a minigdx game that target the JVM platform."
    }

    val android by plugins.creating {
        id = "com.github.minigdx.android"
        implementationClass = "com.github.minigdx.gradle.plugin.MiniGdxAndroidGradlePlugin"
        displayName = "MiniGDX Kotlin Android plugin"
        description = "Configure your gradle project for creating a minigdx game that target the web platform."
    }

    val js by plugins.creating {
        id = "com.github.minigdx.js"
        implementationClass = "com.github.minigdx.gradle.plugin.MiniGdxJsGradlePlugin"
        displayName = "MiniGDX Kotlin Web plugin"
        description = "Configure your gradle project for creating a minigdx game that target the web platform."
    }

    val settings by plugins.creating {
        id = "com.github.minigdx.settings"
        implementationClass = "com.github.minigdx.gradle.plugin.MiniGdxSettingsPlugin"
        displayName = "MiniGDX Kotlin Settings plugin"
        description = "Add actions for the settings of a MiniGDX Gradle project."
    }
}

pluginBundle {
    website = "https://github.com/minigdx/minigdx-gradle-plugin"
    vcsUrl = "https://github.com/minigdx/minigdx-gradle-plugin"

    tags = listOf("minigdx", "kotlin", "jvm", "js", "android")
}
