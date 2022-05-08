package com.github.minigdx.gradle.plugin.android

import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.mockito.Mockito

open class MockLibraryExtension(project: Project) {

    fun defaultConfig(block: EmptyDefaultConfig.() -> Unit) = Unit

    fun compileSdkVersion(param: Int) = Unit

    var buildToolsVersion: String = ""

    fun compileOptions(block: EmptyCompileOptions.() -> Unit) = Unit

    val sourceSets: NamedDomainObjectContainer<AndroidSourceSet> = createContainer(AndroidSourceSet::class.java)

    companion object {

        @Suppress("UNCHECKED_CAST")
        private inline fun <reified T> createContainer(clazz: Class<T>): NamedDomainObjectContainer<T> {
            return Mockito.mock(NamedDomainObjectContainer::class.java) as NamedDomainObjectContainer<T>
        }
    }
}

class EmptyCompileOptions {

    var sourceCompatibility: JavaVersion = JavaVersion.VERSION_1_8
    var targetCompatibility: JavaVersion = JavaVersion.VERSION_1_8
}

open class EmptyDefaultConfig {

    fun minSdkVersion(param: Int) = Unit
    fun targetSdkVersion(param: Int) = Unit
    fun maxSdkVersion(param: Int) = Unit
}
