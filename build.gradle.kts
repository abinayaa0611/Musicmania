// Top-level build file: only declares plugin versions. Modules apply them.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    // Kotlin 2.0+ ships the Compose compiler as a Gradle plugin.
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    // Turns @Serializable classes into JSON parsers at compile time.
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20" apply false
}
