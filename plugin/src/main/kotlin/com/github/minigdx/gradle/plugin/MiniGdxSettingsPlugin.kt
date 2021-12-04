package com.github.minigdx.gradle.plugin

import com.github.minigdx.gradle.plugin.internal.SdkHelper
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

fun Settings.includeAndroid(vararg projectNames: String) {
    val hasAndroidSdk = SdkHelper.hasAndroid(this.rootDir)

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
