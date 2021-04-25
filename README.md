# MiniGDX gradle plugin

## Configure the gradle project

Add those following plugins to configure the gradle build:

```kotlin
plugins {
    // Generate a JVM game
    id("com.github.minigdx.jvm") version "LATEST-SNAPSHOT"
    // Generate a JS game
    id("com.github.minigdx.js") version "LATEST-SNAPSHOT"
    // Configure the game
    id("com.github.minigdx.common") version "LATEST-SNAPSHOT"
}
```

Platform specific plugins are required for each platform.
If a platform is not required, the plugin can be omitted for this platform.

This example will generate a game for the JVM platform and the Web platform.
The plugin for the JS game can be removed to generate the 
game only for the JVM platform.

## MiniGDX configuration

The plugin can be configured to set which miniGDX version is used
or which class to use as Main class for the JVM platform.

```kotlin
minigdx {
    // Configure the version of minigdx used.
    version.set("x.x.x")
    // Configure the main class used by the JVM version
    // Required only if the JVM platform is configured
    jvm.mainClass.set("your.game.Main")
}
```