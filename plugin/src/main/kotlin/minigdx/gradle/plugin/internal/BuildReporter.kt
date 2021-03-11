package minigdx.gradle.plugin.internal

import com.github.minigdx.gradle.plugin.internal.MiniGdxException
import com.github.minigdx.gradle.plugin.internal.Severity
import com.github.minigdx.gradle.plugin.internal.Solution
import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle

enum class MiniGdxPlatform(val banner: String) {
    JVM("banner-jvm.txt"),
    JAVASCRIPT(""),
    ANDROID("")
}

class BuildReporter(private val platform: MiniGdxPlatform) : BuildListener {

    private val classLoader = BuildReporter::class.java.classLoader

    override fun buildStarted(gradle: Gradle) = Unit

    override fun settingsEvaluated(settings: Settings) = Unit

    override fun projectsLoaded(gradle: Gradle) = Unit

    override fun projectsEvaluated(gradle: Gradle) {
        printBanner(gradle, "banner.txt")
        printBanner(gradle, platform.banner)
    }

    private fun printBanner(gradle: Gradle, filename: String) {
        val content = classLoader.getResourceAsStream(filename) ?: throw MiniGdxException.create(
            severity = Severity.GRAVE,
            project = gradle.rootProject,
            because = "'$filename' file not found in the plugin jar! The plugin might have been incorrectly packaged.",
            description = "The plugin is trying to copy a resource that should has been packaged into the plugin " +
                "but is not. As this file is required, the plugin will stop.",
            solutions = listOf(Solution("An issue can be reported to the developer", MiniGdxException.ISSUES))
        )
        gradle.rootProject.logger.quiet(String(content.readBytes()))
    }

    override fun buildFinished(result: BuildResult) = Unit
}
