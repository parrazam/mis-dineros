# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & test commands

The project requires Android SDK (not present in this dev machine). Use Android Studio to build locally, or rely on GitHub Actions CI for automated builds. On machines with the SDK available:

```bash
# Unit tests (JVM, no device needed)
./gradlew test

# Single test class
./gradlew test --tests "com.parra.misdineros.domain.usecase.CalcMonthlySpendUseCaseTest"

# Lint
./gradlew lint

# Debug APK
./gradlew assembleDebug

# Instrumented tests (needs connected device or emulator)
./gradlew connectedAndroidTest
```

Gradle wrapper requires JDK 21. The CI uses `actions/setup-java@v5` with `temurin` JDK 21.

Room schema files are exported to `app/schemas/` (tracked in git). When changing any `@Entity` or `@Database` version, bump `version` in `@Database` and add a migration or `fallbackToDestructiveMigration`.

## Architecture

Single-module Clean Architecture by packages. Dependency flow: `presentation → domain ← data`.

```
core/           — utilities with no Android/domain dependencies (MoneyFormatter, DateUtils, AppError)
domain/         — pure Kotlin: models, repository interfaces, use cases
data/           — Room entities+DAOs, repository impls, DataStore, mappers, seeding
designsystem/   — MisDinerosTheme, Color, Typography, Shape, reusable Compose components
presentation/   — screens + ViewModels (one sub-package per screen)
notifications/  — WorkManager workers + scheduler
di/             — Hilt modules (DatabaseModule, RepositoryModule, WorkerModule)
```

### Key design decisions

**Money as Long (minor units).** All monetary amounts stored and computed as `Long` in minor currency units (e.g., cents). Never use `Double` for money. `MoneyFormatter` handles display conversion. `Subscription.monthlyAmountMinor` normalises annual→monthly inline.

**Lazy seeding.** Categories (`CategoryRepositoryImpl`) and FX rates (`FxRepositoryImpl`) seed their data on first access via `Flow.onStart { seedIfEmpty() }`, not in `RoomDatabase.Callback`. The `seedCallback` in `MisDinerosDatabase` is intentionally empty.

**FX rates.** `BundledFxRates.generateEntities()` produces ~650 cross-rate pairs (NxN via EUR triangulation) from 25 hard-coded base rates. These are seeded once to Room and are then editable. All conversion goes through `FxRepository.convert()`.

**Icon references.** `Subscription.iconRef` is a string discriminated union: `"bundled:<key>"` for catalog icons, `"file:<absolutePath>"` for user-uploaded images, `"initial"` fallback.

**Hilt + WorkManager.** `MisDinerosApplication` implements `Configuration.Provider` and injects `HiltWorkerFactory` to wire Hilt into WorkManager. Do not call `WorkManager.initialize()` elsewhere.

**Theme.** `MisDinerosTheme` accepts an `AppTheme` enum (SYSTEM/LIGHT/DARK) and an optional `dynamicColor` flag (Android 12+). Seed color `#0077B6` (Blue Snorkel). Light/dark palettes are in `Color.kt` as `md_theme_light_*` / `md_theme_dark_*` constants.

**Navigation.** Single-activity, Compose NavHost. All routes are defined in `Destination` sealed class (`presentation/navigation/Destinations.kt`). `SubscriptionEdit` and `SubscriptionDetail` take an optional/required `id` string argument.

### Instrumented test runner

`HiltTestRunner` in `androidTest/` is the custom runner configured in `build.gradle.kts`. Hilt-injected components in instrumented tests must use `@HiltAndroidTest` + `HiltAndroidRule`.

## Phases status

All 11 phases complete. v1 feature set is done.
