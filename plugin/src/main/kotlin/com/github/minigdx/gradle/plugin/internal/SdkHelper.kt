package com.github.minigdx.gradle.plugin.internal

import org.jetbrains.kotlin.konan.properties.hasProperty
import java.io.File
import java.util.Properties

object SdkHelper {

    fun hasAndroid(rootProjectPath: File): Boolean {
        val localPropertiesFile = rootProjectPath.resolve(File("local.properties"))
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

        return hasAndroidSdk
    }
}
