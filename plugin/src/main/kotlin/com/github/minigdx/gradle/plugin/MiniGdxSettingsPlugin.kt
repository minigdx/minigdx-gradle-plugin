package com.github.minigdx.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.jetbrains.kotlin.konan.properties.hasProperty
import java.io.File
import java.util.Properties

fun Settings.includeAndroid(vararg projectNames: String) {
    val localPropertiesFile = File("local.properties")
    // The determination of the android SDK is inspired of the Android Gradle Plugin
    // @see https://android.googlesource.com/platform/tools/build/+/925c0b5cf8730105dd5aa8c851141d5688d07789/gradle/src/main/groovy/com/android/build/gradle/BasePlugin.groovy
    val hasAndroidSdk = if (localPropertiesFile.exists()) {

        val localProperties = Properties().apply {
            load(localPropertiesFile.inputStream())
        }

        // The path to the Android SDK is defined.
        // The plugin will not check if the SDK is valid.
        // It will assume that it's correct.
        localProperties.hasProperty("sdk.dir") || localProperties.hasProperty("android.dir")
    } else {
        System.getenv("ANDROID_HOME") != null
    }

    if (hasAndroidSdk) {
        include(*projectNames)
    } else {
        projectNames.forEach {
            println(
                "module '$it' was not included as not Android SDK has been defined. " +
                    "Please set the SDK path using the ANDROID_HOME environment variable or " +
                    "create a local.properties file with 'sdk.dir=<path to android sdk>'"
            )
        }
    }
}

class MiniGdxSettingsPlugin : Plugin<Settings> {

    override fun apply(target: Settings) = Unit
}
