# Skill: Navigation 3

## Description
Guidance for implementing or migrating to Jetpack Navigation 3, focusing on a more reliable, state-driven, and type-safe navigation structure.

## Relevance to CuteCats
The project currently uses string-based routes in `CuteCatsNavHost.kt`. Upgrading to Navigation 3 will provide:
- Type-safety for destinations and arguments.
- Better state management for the back stack using `SnapshotStateList`.
- Improved support for adaptive layouts and deep linking.

## Impacted Files
- `app/src/main/java/com/gustavo/brilhante/cutecats/navigation/CuteCatsNavHost.kt`
- `app/src/main/java/com/gustavo/brilhante/cutecats/ui/CuteCatsApp.kt`
- `gradle/libs.versions.toml`

## Technical Gains
- Elimination of string-based route errors.
- Simplified deep link handling.
- Better integration with multi-pane layouts.

## Risks
- Breaking changes in how `NavController` is used (migrating to `NavKey`).
- Requires updating dependencies and potentially adjusting Hilt ViewModel injection.
