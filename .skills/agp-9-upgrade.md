# Skill: AGP 9 Upgrade

## Description
Supports the transition to Android Gradle Plugin (AGP) 9, including handling breaking changes, DSL updates, and Kapt-to-KSP migrations.

## Relevance to CuteCats
The project is on AGP 8.9.2 and Kotlin 2.0.21. Preparing for AGP 9 involves:
- Migrating from `kapt` to `ksp` for Hilt and other libraries.
- Updating Gradle and Java versions (target JDK 17/21).
- Adopting new DSL features.

## Impacted Files
- `gradle/libs.versions.toml`
- `build.gradle.kts` (root and modules)
- `gradle/wrapper/gradle-wrapper.properties`

## Technical Gains
- Faster build times with KSP.
- Access to the latest Android Build tools and optimizations.

## Risks
- Library compatibility issues with AGP 9.0 or KSP.
- Changes in generated code might require small refactorings.
