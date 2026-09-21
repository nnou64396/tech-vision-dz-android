// Standalone build-script module hosting the release-APK verification rules.
// Kept separate from the app module so the rules are pure Kotlin, unit-testable
// with JUnit, and usable directly by the `verifyReleaseApk` Gradle task.
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}