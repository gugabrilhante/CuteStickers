# Skill: Edge-to-Edge

## Description
Instructions for implementing edge-to-edge UI, ensuring apps properly utilize the full screen area, including system bars, as required by newer Android versions.

## Relevance to CuteCats
While `MainActivity` calls `enableEdgeToEdge()`, a full implementation ensures:
- Proper handling of `WindowInsets` in `Scaffold` and custom layouts.
- Correct status bar and navigation bar contrast.
- Avoiding UI overlap with display cutouts and system gestures.

## Impacted Files
- `app/src/main/java/com/gustavo/brilhante/cutecats/MainActivity.kt`
- `app/src/main/java/com/gustavo/brilhante/cutecats/ui/CuteCatsApp.kt`
- Core UI components in `:core:designsystem` and `:core:ui`.

## Technical Gains
- Modern, immersive UI that respects system boundaries.
- Future-proofing for Android 15+ where edge-to-edge is the default.

## Risks
- Unexpected padding or clipping if insets are not applied correctly in deeply nested layouts.
