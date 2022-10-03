package com.github.minigdx.gradle.plugin.tasks

import com.github.dwursteisen.gltf.GltfExtensions
import com.github.minigdx.gradle.plugin.MiniGdxCommonGradlePlugin
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileInputStream
import java.util.jar.JarInputStream
import java.util.jar.Manifest
import kotlin.reflect.KClass

/**
 * Display version of components used in the minigdx stack.
 *
 * It's mainly to help find versions issues.
 * As there is a lot of components, it can be hard/not sure
 * of which version is used for each one.
 */
abstract class PrintVersionTask : DefaultTask() {
    init {
        this.group = "minigdx"
        this.description = "Print versions used by the minigdx stack"
    }

    @get:InputFiles
    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    private fun extractVersion(kclass: KClass<*>): String {
        return kclass.java.`package`.implementationVersion ?: "Unknown version"
    }

    data class DependencyMetadata(
        /**
         * Name that will be displayed to the user
         */
        val name: String,
        /**
         * What prefix is used to catch the related jar?
         */
        val jarPrefix: String,
        /**
         * What prefix is used to catch the group from the path?
         */
        val pathPrefix: String = jarPrefix,
        /**
         * Path of the dependency.
         *
         * Not null when resolved.
         */
        val path: File? = null
    ) {

        fun extractVersion(): String {
            val jarStream = JarInputStream(FileInputStream(path!!))
            val manifest: Manifest = jarStream.manifest
            val mainAttribs = manifest.mainAttributes
            val manifestVersion = mainAttribs.getValue("Implementation-Version")

            val pathParts = path.absolutePath.split("/")
            val (_, pathVersion) = pathParts.dropWhile { part -> !part.startsWith(pathPrefix) }

            return manifestVersion ?: pathVersion
        }
    }

    private val expectedDependencies = setOf(
        DependencyMetadata("kotlinx-coroutines", "kotlinx-coroutines"),
        DependencyMetadata("kotlinx-serialization", "kotlinx-serialization"),
        DependencyMetadata("kotlin", "kotlin-stdlib"),
        DependencyMetadata("kotlin-math", "kotlin-math"),
        DependencyMetadata("minigdx", "minigdx-metadata", pathPrefix = "minigdx"),
        DependencyMetadata("minigdx-imgui-light", "minigdx-imgui-light"),
        DependencyMetadata("minigdx-glft-parser", "gltf-api"),
    )

    @TaskAction
    fun printVersions() {
        println("--- \t minigdx plugins \t ---")
        println("minigdx-gradle-plugin".padEnd(50) + ": \t ${extractVersion(MiniGdxCommonGradlePlugin::class)}")
        println("minigdx-gltf-parser".padEnd(50) + ": \t ${extractVersion(GltfExtensions::class)}")
        println()
        val dependencies = classpath.files
            .asSequence()
            .mapNotNull { file ->
                val resolvedDependency = expectedDependencies
                    .firstOrNull { deps -> file.name.startsWith(deps.jarPrefix) }

                resolvedDependency?.copy(path = file)
            }
            .distinctBy { deps -> deps.name }
            .sortedBy { deps -> deps.name }

        println("--- \t minigdx dependencies \t ---")
        dependencies.forEach { deps ->
            println("${deps.name.padEnd(50)}:               \t ${deps.extractVersion()}")
        }
    }
}
