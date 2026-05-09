
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.dagger.hilt.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// ── JaCoCo aggregated coverage ────────────────────────────────────────────────
// Usage:  ./gradlew testDebugUnitTest jacocoTestReport
// Output: build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
//
// Coverage for Android modules is driven by enableUnitTestCoverage = true in
// each module's debug buildType (set in the individual build.gradle.kts files).
// AGP writes exec files to:
//   <module>/build/outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec
//
// NOTE: Do NOT apply the Gradle jacoco plugin to Android submodules — it is
// incompatible with AGP 8.x's AndroidUnitTest task (TypeNotPresentException).
// Apply it only to the root project and pure-JVM modules.

val jacocoExclusions = listOf(
    "**/R.class",
    "**/R\$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "**/Hilt_*",
    "**/*_HiltModules*",
    "**/*_Factory*",
    "**/*_MembersInjector*",
    "**/DaggerHilt*",
    "**/ComposableSingletons*",
    "**/*_Impl.*",
    "**/Dagger*.*",
    "**/*_LifecycleAdapter.*",
)

// Apply jacoco only to the pure-JVM :core:model module — safe because it has
// no Android tasks that would clash with the plugin.
subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        apply(plugin = "jacoco")
        configure<JacocoPluginExtension> { toolVersion = "0.8.12" }
    }
}

// Root-level jacoco plugin is needed for the JacocoReport task type below.
apply(plugin = "jacoco")
configure<JacocoPluginExtension> { toolVersion = "0.8.12" }

tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generates aggregated JaCoCo coverage report for all modules."

    // Wait for every module's unit-test task before generating the report.
    dependsOn(
        subprojects.map { proj ->
            proj.tasks.matching { t ->
                (t.name == "testDebugUnitTest") ||
                (t.name == "connectedDebugAndroidTest" && proj.file("src/androidTest").exists()) ||
                (t.name == "test" && !proj.plugins.hasPlugin("com.android.base"))
            }
        }
    )

    reports {
        xml.required.set(true)
        xml.outputLocation.set(
            file("${layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
        )
        html.required.set(true)
        html.outputLocation.set(
            file("${layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/html")
        )
    }

    classDirectories.setFrom(
        files(
            subprojects.flatMap { proj ->
                listOf(
                    // Android modules: Kotlin classes
                    fileTree("${proj.layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
                        exclude(jacocoExclusions)
                    },
                    // Android modules: Java classes
                    fileTree("${proj.layout.buildDirectory.get()}/intermediates/javac/debug/classes") {
                        exclude(jacocoExclusions)
                    },
                    // Pure JVM module (:core:model)
                    fileTree("${proj.layout.buildDirectory.get()}/classes/kotlin/main") {
                        exclude(jacocoExclusions)
                    },
                )
            }
        )
    )

    sourceDirectories.setFrom(
        files(
            subprojects.flatMap { proj ->
                listOf("src/main/java", "src/main/kotlin").map { "${proj.projectDir}/$it" }
            }
        )
    )

    // AGP's enableUnitTestCoverage writes exec files here (AGP 7+ / 8.x / 9.x).
    executionData.setFrom(
        subprojects.map { proj ->
            fileTree(proj.layout.buildDirectory.map { it.asFile }) {
                include("outputs/unit_test_code_coverage/**/*.exec")
                include("outputs/code_coverage/**/*.ec")
                include("jacoco/*.exec") // For JVM modules like :core:model
            }
        }
    )
}
