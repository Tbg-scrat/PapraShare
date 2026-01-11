// Datei: build.gradle.kts (im Hauptordner des Projekts)
plugins {
    // Wir definieren hier explizite Versionen für Stabilität
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.3.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0" apply false
}

tasks.register("clean", Delete::class) {
    // KORREKTUR: 'layout.buildDirectory' statt 'buildDir'
    delete(rootProject.layout.buildDirectory)
}