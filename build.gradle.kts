
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

    // Fix: Explicitly declare dependencies on compilation and test tasks to satisfy Gradle 9.x+ validation.
    subprojects.forEach { proj ->
        dependsOn(proj.tasks.matching {
            it.name == "compileDebugKotlin" ||
            it.name == "compileDebugJavaWithJavac" ||
            it.name == "compileKotlin" ||
            it.name == "compileJava" ||
            it.name == "testDebugUnitTest" ||
            // Only include 'test' for JVM modules to avoid triggering connected tests in some Android versions/configurations
            (it.name == "test" && !proj.plugins.hasPlugin("com.android.library") && !proj.plugins.hasPlugin("com.android.application"))
        })
    }

    reports {
        xml.required.set(true)
        xml.outputLocation.set(
            layout.buildDirectory.file("reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
        )
        html.required.set(true)
        html.outputLocation.set(
            layout.buildDirectory.dir("reports/jacoco/jacocoTestReport/html")
        )
    }

    classDirectories.setFrom(
        subprojects.flatMap { proj ->
            listOf(
                // Android modules: Kotlin classes
                proj.fileTree(proj.layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
                    exclude(jacocoExclusions)
                },
                // Android modules: Java classes
                proj.fileTree(proj.layout.buildDirectory.dir("intermediates/javac/debug/classes")) {
                    exclude(jacocoExclusions)
                },
                // Pure JVM module (:core:model)
                proj.fileTree(proj.layout.buildDirectory.dir("classes/kotlin/main")) {
                    exclude(jacocoExclusions)
                },
            )
        }
    )

    sourceDirectories.setFrom(
        subprojects.flatMap { proj ->
            listOf("src/main/java", "src/main/kotlin").map { proj.projectDir.resolve(it) }
        }
    )

    // AGP's enableUnitTestCoverage writes exec files here (AGP 7+ / 8.x / 9.x).
    // Use .asFile.get() to avoid automatic task dependencies on connected tests
    // when we only want to collect existing results.
    executionData.setFrom(
        subprojects.flatMap { proj ->
            val buildDir = proj.layout.buildDirectory.asFile.get()
            listOf(
                proj.fileTree(buildDir.resolve("outputs/unit_test_code_coverage")) {
                    include("**/*.exec")
                },
                proj.fileTree(buildDir.resolve("outputs/code_coverage")) {
                    include("**/*.ec")
                },
                proj.fileTree(buildDir.resolve("outputs/connected_android_test_code_coverage")) {
                    include("**/*.ec")
                },
                proj.fileTree(buildDir.resolve("jacoco")) {
                    include("**/*.exec")
                }
            )
        }
    )
}
