# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & test commands

The Android SDK is at `~/Android/Sdk` (not in `PATH`; `ANDROID_HOME` unset). Prefix Gradle invocations with `ANDROID_HOME=$HOME/Android/Sdk`, and use `~/Android/Sdk/platform-tools/adb` and `~/Android/Sdk/emulator/emulator` directly. The machine has KVM and headless emulators work (`-no-window -gpu swiftshader_indirect`); AVDs `Pixel_9a`, `Phone_Screenshots` (1080x2160) and `Tablet_MisDineros` (2560x1600) exist under `~/.android/avd/`. There is no `cmdline-tools` (no `avdmanager`/`sdkmanager`) — create AVDs by cloning an existing `.avd` dir and editing `config.ini`.

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

Room schema files are exported to `app/schemas/` (tracked in git). When changing any `@Entity` or `@Database` version, bump `version` in `@Database` and add a migration or `fallbackToDestructiveMigration`. When bumping the Room dependency itself, diff `app/schemas/` afterwards: if the exported JSON is byte-identical the `identityHash` is unchanged and existing installs open their database untouched.

Instrumented test names must be plain identifiers, **not** backticked names with spaces. D8 rejects those below DEX 040 (`Space characters in SimpleName are not allowed`), which needs minSdk 35. Backticks are fine in `src/test/` (JVM) but break `connectedAndroidTest`. The CI runs only `test`/`lint`/`assembleDebug`, so instrumented breakage is not caught automatically — run `connectedAndroidTest` locally after touching `androidTest/` or any model it constructs.

## Architecture

Single-module Clean Architecture by packages. Dependency flow: `presentation → domain ← data`.

```
core/           — utilities with no Android/domain dependencies (MoneyFormatter, DateUtils, AppError)
domain/         — pure Kotlin: models, repository interfaces, use cases
data/           — Room entities+DAOs, repository impls, DataStore, mappers, seeding
backup/         — MisDinerosBackupAgent (Auto Backup), BackupCrypto (AES-256-GCM)
designsystem/   — MisDinerosTheme, Color, Typography, Shape, reusable Compose components
presentation/   — screens + ViewModels (one sub-package per screen)
notifications/  — WorkManager workers + scheduler
di/             — Hilt modules (DatabaseModule, RepositoryModule, WorkerModule)
```

### Key design decisions

**Money as Long (minor units).** All monetary amounts stored and computed as `Long` in minor currency units (e.g., cents). Never use `Double` for money. `MoneyFormatter` handles display conversion. `Subscription.monthlyAmountMinor` normalises annual→monthly inline.

**Auto-advancing renewals.** Subscriptions store `billingAnchorDay` (original billing day-of-month) alongside `nextRenewalDate`. `AdvanceDueRenewalsUseCase` recomputes and persists past-due renewal dates for active subscriptions (handles several missed cycles at once) so a subscription never stays anchored in the past, disappears from Home, or stops notifying. `BillingCycle.nextRenewal(from, anchorDay)` re-anchors with `min(anchorDay, daysInMonth)` (a day-31 subscription keeps the 31st across months with fewer days). It runs on app start, in the daily worker, and after an import. The use case is triggered from those three places only. Room DB is at `version = 2`; `MIGRATION_1_2` adds `billingAnchorDay` and backfills it from the day-of-month of the existing `nextRenewalDate`. The backup JSON is unchanged — `toDomain` derives the anchor from the date's day.

**Lazy seeding.** Categories (`CategoryRepositoryImpl`) and FX rates (`FxRepositoryImpl`) seed their data on first access via `Flow.onStart { seedIfEmpty() }`, not in `RoomDatabase.Callback`. The `seedCallback` in `MisDinerosDatabase` is intentionally empty.

**FX rates.** `BundledFxRates.generateEntities()` produces ~650 cross-rate pairs (NxN via EUR triangulation) from 25 hard-coded base rates. These are seeded once to Room and are then editable. All conversion goes through `FxRepository.convert()`.

**Icon references.** `Subscription.iconRef` is a string discriminated union: `"bundled:<key>"` for catalog icons, `"file:<absolutePath>"` for user-uploaded images, `"initial"` fallback.

**Hilt + WorkManager.** `MisDinerosApplication` implements `Configuration.Provider` and injects `HiltWorkerFactory` to wire Hilt into WorkManager. Do not call `WorkManager.initialize()` elsewhere.

**Daily notifications are self-rescheduling one-time work, never `PeriodicWorkRequest`.** A 24h periodic request re-anchors to the time the last run *actually* executed (Doze defers it), so it drifts permanently away from the configured hour, and `ExistingPeriodicWorkPolicy.UPDATE` preserves the old schedule (the new `initialDelay` is ignored). Instead, `NotificationScheduler` enqueues one-time work (`ExistingWorkPolicy.REPLACE`) delayed to the next occurrence of the configured hh:mm, and each worker re-enqueues **its own** next run as the last statement of `doWork()` (in a `finally`, guarded by `!isStopped` so an externally-cancelled worker doesn't clobber the schedule that cancelled it; re-enqueuing the *other* worker's name would cancel it mid-run since both fire at the same time). If the pre-`try` phase (settings read) throws, the worker returns `Result.retry()` so the chain survives transient failures. App start and Settings changes re-anchor via `schedule()`.

**Theme.** `MisDinerosTheme` accepts an `AppTheme` enum (SYSTEM/LIGHT/DARK) and an optional `dynamicColor` flag (Android 12+). The flag is driven by the `dynamicColorEnabled` setting (`AppSettings`, default `false`), toggleable from Settings → Apariencia (the switch is hidden below Android 12); `MainViewModel` exposes both as a `ThemeConfig` flow. Seed color `#0077B6` (Blue Snorkel). Light/dark palettes are in `Color.kt` as `md_theme_light_*` / `md_theme_dark_*` constants.

**Navigation.** Single-activity, Compose NavHost. All routes are defined in `Destination` sealed class (`presentation/navigation/Destinations.kt`). `SubscriptionEdit` and `SubscriptionDetail` take an optional/required `id` string argument.

**targetSdk 36 (Android 16).** Required by Play for updates from 2026-08-31. The two hard behaviour changes were already satisfied: edge-to-edge is enforced (the app calls `enableEdgeToEdge()` and drives bar appearance through `WindowInsetsControllerCompat` in `MisDinerosTheme`, never `setStatusBarColor`), and orientation/resizeability restrictions are ignored on displays ≥ 600 dp — the portrait lock in `MainActivity` is already gated by `R.bool.lock_portrait_orientation`, which is `false` in `values-sw600dp/`. `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` (the opt-out available until API 37) is deliberately not declared. `android:enableOnBackInvokedCallback="true"` is declared explicitly: it is the default at target 36 but this equalises behaviour on API 33–35, and it is safe because nothing in the app intercepts back (no `BackHandler`, no `onBackPressed()`).

**Layout must not assume portrait on large screens.** Since the system now ignores orientation restrictions above 600 dp, two-column layouts need to check the aspect ratio, not just the width. `StatsScreen` picks its wide layout with `LocalWindowInfo.current.containerSize` requiring **both** ≥ 600 dp wide **and** width ≥ height; with the width check alone, the donut's `fillMaxHeight().aspectRatio(1f)` consumed the full width on a portrait tablet and the legend's `weight(1f)` collapsed to 0 px. Prefer `LocalWindowInfo.containerSize` over `LocalConfiguration.screenWidthDp`, whose inset behaviour depends on the targetSdk (lint: `ConfigurationScreenWidthHeight`). `HomeScreen` applies the same rule as `maxWidth >= 600.dp && maxWidth >= maxHeight`, reusing the constraints of the `BoxWithConstraints` it already has (they exclude the Scaffold's `innerPadding`, so no `LocalWindowInfo` lookup is needed); there the width-only check did not collapse anything, it just left the two columns cramped in the upper half of a portrait tablet. Any new two-column layout must check both dimensions.

**16 KB page size.** The app ships no `jniLibs` of its own, but the AAB does contain `libandroidx.graphics.path.so` and `libdatastore_shared_counter.so` from transitive AndroidX dependencies. Both are already 16 KB-aligned (`objdump -p` reports `align 2**14` on every LOAD segment), so Play's requirement is met. Re-check with `unzip -l <aab> | grep '\.so$'` after upgrading Compose or DataStore.

**Backup file format.** Exported files carry a 5-byte header: magic `MDB1` (4 bytes) + flags byte (`0x00` = plain, `0x01` = AES-256-GCM encrypted). Files without this header are treated as legacy plain JSON (full backward compatibility). Encryption key is derived with PBKDF2WithHmacSHA256 (200 000 iterations, 16-byte random salt, 12-byte random IV). `BackupCrypto` (`data/backup/BackupCrypto.kt`) is a pure `object` with no Android dependencies — unit-testable on JVM. The CPU-heavy PBKDF2 call runs on `Dispatchers.Default` inside the use cases.

**Auto Backup.** `allowBackup="true"` + `fullBackupOnly="true"` with a custom `MisDinerosBackupAgent` (`backup/MisDinerosBackupAgent.kt`). `fullBackupOnly="true"` forces the system to use Auto Backup (full-data) even though a `BackupAgent` is declared — without it the system treated the app as key/value backup and stored nothing (it didn't even appear in Google Backup). The agent reads `SharedPreferences("auto_backup_prefs").getBoolean("enabled", true)` in `onFullBackup` and skips the backup if disabled. It does NOT duplicate the XML rules: it runs `PRAGMA wal_checkpoint(FULL)` on the Room DB so the `.db` file is consistent, then delegates to `super.onFullBackup(data)` to let the system apply `data_extraction_rules.xml` (Android 12+) and `backup_rules.xml` (Android <12). `onBackup`/`onRestore` (k/v) are no-ops. Restore is never blocked. The `autoBackupEnabled` flag is mirrored from DataStore to `SharedPreferences` on every `SettingsRepository.update()` call because `BackupAgent` runs outside Hilt's lifecycle and cannot call suspend functions.

**FileProvider for share exports.** Temporary export files are written to `cacheDir/exports/` and shared via `${applicationId}.fileprovider` (authority defined in `AndroidManifest.xml`, paths in `res/xml/file_provider_paths.xml`). The exports directory is not included in Auto Backup. The pending encryption password between the export dialog and the SAF callback is stored as `CharArray?` in `SettingsViewModel` and zeroed out immediately after use.

### Instrumented test runner

`HiltTestRunner` in `androidTest/` is the custom runner configured in `build.gradle.kts`. Hilt-injected components in instrumented tests must use `@HiltAndroidTest` + `HiltAndroidRule`.

## Phases status

All 11 phases complete. v1 feature set is done.
