
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
    "**/*_ViewBinding*.*",
    "**/*_ViewBinder*.*",
    "**/*_GeneratedInstaller*.*",
    "**/BR.class",
    "**/DataBindingInfo.class",
    "**/DataBinderMapperImpl.class",
    "**/DataBinderMapperImpl\$*.class",
    "**/*Args*.*",
    "**/*Directions*.*",
    "**/*_Provide*.*",
    "**/*_Inject*.*",
    "**/*_Module*.*"
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

    // Explicitly declare dependencies on all tasks whose outputs are consumed by this
    // report, as required by Gradle 9.x strict task-dependency validation.
    subprojects.forEach { proj ->
        // Declare that we MUST run after connected tests IF they are in the task graph,
        // but don't force them to run if they weren't requested.
        mustRunAfter(proj.tasks.matching { it.name == "connectedDebugAndroidTest" })

        dependsOn(proj.tasks.matching {
            it.name == "compileDebugKotlin" ||
            it.name == "compileDebugJavaWithJavac" ||
            it.name == "compileKotlin" ||
            it.name == "compileJava" ||
            it.name == "testDebugUnitTest" ||
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
    // Use a Callable to avoid automatic task dependencies on connected tests
    // when we only want to collect existing results during execution.
    executionData.setFrom(files(java.util.concurrent.Callable {
        val allExecFiles = mutableListOf<File>()
        subprojects.forEach { proj ->
            val buildDir = proj.layout.buildDirectory.asFile.get()
            val possibleDirs = listOf(
                File(buildDir, "outputs/unit_test_code_coverage"),
                File(buildDir, "outputs/code_coverage"),
                File(buildDir, "outputs/connected_android_test_code_coverage"),
                File(buildDir, "jacoco")
            )
            possibleDirs.filter { it.exists() }.forEach { dir ->
                dir.walkTopDown().filter { it.extension == "exec" || it.extension == "ec" }.forEach {
                    allExecFiles.add(it)
                }
            }
        }
        // Also look in a root 'coverage-data' directory (useful for CI artifacts)
        val ciDataDir = file("coverage-data")
        if (ciDataDir.exists()) {
            ciDataDir.walkTopDown().filter { it.extension == "exec" || it.extension == "ec" }.forEach {
                allExecFiles.add(it)
            }
        }
        allExecFiles
    }))
}
