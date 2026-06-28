plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.compose") version "1.7.0"
    application
}

group = "com.filamentmanager"
version = "3.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.uiToolingPreview)

    // Will pull in shared logic later (adapter, models)
    // implementation(project(":core")) or copy domain for v3 start
}

application {
    mainClass.set("com.filamentmanager.MainKt")
}

compose.desktop {
    application {
        mainClass = "com.filamentmanager.MainKt"
        nativeDistributions {
            packageName = "FilamentManager"
            packageVersion = "3.0.0"
            description = "All-in-one Windows desktop app for OrcaSlicer filament profiles"
            copyright = "© 2026 Moosepond Designs"
            vendor = "Moosepond Designs"
        }
    }
}