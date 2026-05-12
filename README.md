# CuteStickers

[![Build](https://github.com/gugabrilhante/CuteCats/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/gugabrilhante/CuteCats/actions/workflows/build.yml)
[![Unit Tests](https://github.com/gugabrilhante/CuteCats/actions/workflows/unit_test.yml/badge.svg?branch=master)](https://github.com/gugabrilhante/CuteCats/actions/workflows/unit_test.yml)
<-- [![UI Tests](https://github.com/gugabrilhante/CuteCats/actions/workflows/ui_test.yml/badge.svg?branch=master)](https://github.com/gugabrilhante/CuteCats/actions/workflows/ui_test.yml) -->
<-- [![codecov](https://codecov.io/gh/gugabrilhante/CuteCats/branch/master/graph/badge.svg)](https://codecov.io/gh/gugabrilhante/CuteCats) -->

CuteStickers is a scalable Android application built with Jetpack Compose, Hilt, and KSP, following modern architectural principles. Originally focused on cute animal GIFs, the project has been refactored to be domain-agnostic, supporting a wide range of media content categories like cats, dogs, stickers, anime, and more.

## Architecture

The project follows a modular architecture designed for scalability and reusability:

### Modules

- **`:app`**: The main entry point of the application, responsible for navigation and dependency aggregation.
- **`:core:model`**: Domain-agnostic data models used across all layers (e.g., `MediaItem`).
- **`:core:domain`**: Core business logic and repository interfaces (e.g., `MediaRepository`).
- **`:core:data`**: Implementation of domain interfaces, handling data retrieval from network or local sources.
- **`:core:network`**: Network communication layer using Retrofit and domain-agnostic service definitions.
- **`:core:ui`**: Reusable UI components and base screen implementations (e.g., `DiscoverScreen`, `MediaCard`).
- **`:core:designsystem`**: Application theme, colors, and design tokens.
- **`:feature:discover`** (Planned): A unified discovery feature for browsing different media categories.
- **`:feature:media-details`**: A reusable feature for displaying detailed information about a specific media item.
- **`:feature:cats` / `:feature:dogs`**: Specific category implementations leveraging the core discovery and details pipelines.

## Scalability and Extensibility

The refactored architecture offers several benefits for future growth:

- **Domain-Agnostic Core**: The core layers (`model`, `domain`, `data`, `network`, `ui`) deal with generic "Media" concepts rather than specific domains like "Animals".
- **Pluggable Features**: New content categories (e.g., `cars`, `wallpapers`) can be added by creating new feature modules or extending the `discover` feature without modifying existing core logic.
- **Reusable UI**: The `DiscoverScreen` and `MediaCard` components in `:core:ui` are designed to be reused across different features, ensuring UI/UX consistency.
- **Dependency Inversion**: High-level features depend on generic interfaces (e.g., `MediaRepository`), allowing different data sources or category implementations to be swapped in easily.

## Getting Started

1. Clone the repository.
2. Open the project in Android Studio.
3. Sync Gradle and run the `:app` module.

## Technologies Used

- **Jetpack Compose**: For a modern, declarative UI.
- **Hilt**: For dependency injection.
- **KSP (Kotlin Symbol Processing)**: For efficient annotation processing.
- **Retrofit**: For network requests.
- **Kotlin Serialization**: For domain-agnostic data parsing.
- **Navigation 3**: For type-safe and scalable navigation.
- **Shared Element Transitions**: For smooth UI animations between screens.
