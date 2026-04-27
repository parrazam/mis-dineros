# Mis Dineros

[![Android CI](https://github.com/parrazam/mis-dineros/actions/workflows/android.yml/badge.svg)](https://github.com/parrazam/mis-dineros/actions/workflows/android.yml)

Gestor de suscripciones personal para Android. Sin servidores, sin telemetría, sin red — 100% local.

## Características

- **Suscripciones** — nombre, importe, moneda, ciclo mensual/anual, categoría, notas e icono personalizado
- **Dashboard** — gasto mensual real, equivalente anual, próximas renovaciones (7 días) y top 5 más caras
- **Estadísticas** — donut por categoría, barras mensual/anual y ranking
- **Divisas** — conversión automática a moneda global con tasas de cambio editables y bundled
- **Notificaciones locales** — aviso configurable N días antes por suscripción + resumen mensual
- **Export/Import JSON** — copia de seguridad versionada con iconos custom embebidos en base64
- **Temas** — claro / oscuro / sistema, seed color Blue Snorkel `#0077B6`
- **Categorías** — 9 predefinidas + creación libre

## Requisitos

- Android 8.0+ (API 26)
- Android Studio Meerkat o superior
- JDK 17

## Build

```bash
# Tests unitarios (JVM, sin dispositivo)
./gradlew test

# Lint
./gradlew lint

# APK de debug
./gradlew assembleDebug

# Tests instrumentados (requiere dispositivo o emulador API 26+)
./gradlew connectedAndroidTest
```

El APK de debug se genera en `app/build/outputs/apk/debug/`.

## Arquitectura

Single-module Clean Architecture por paquetes. Flujo de dependencias: `presentation → domain ← data`.

```
app/src/main/java/com/parra/misdineros/
├── core/           utilidades sin dependencias Android (MoneyFormatter, DateUtils, AppError)
├── domain/         modelos, interfaces de repositorio, use cases (Kotlin puro)
├── data/           Room, DataStore, mappers, backup, seeding
├── notifications/  WorkManager workers y scheduler
├── presentation/   Screens + ViewModels por pantalla
├── designsystem/   MisDinerosTheme, paleta, componentes reutilizables
└── di/             módulos Hilt
```

| Capa | Tecnología |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navegación | Navigation Compose (type-safe routes) |
| DI | Hilt |
| Persistencia | Room + DataStore Preferences |
| Notificaciones | WorkManager |
| Gráficos | Vico |
| Serialización | kotlinx.serialization |

## Backup

El formato de exportación es JSON versionado (`version: 1`):

```json
{
  "version": 1,
  "exportedAt": "2026-04-27T10:00:00Z",
  "subscriptions": [...],
  "categories": [...],
  "fxRates": [...],
  "settings": {...},
  "assets": { "icon.png": "<base64>" }
}
```

Los iconos subidos por el usuario se embeben en `assets` como base64 y se restauran en `filesDir/icons/` al importar.

## CI

GitHub Actions ejecuta en cada push/PR a `master`:

1. `./gradlew test` — tests unitarios JVM
2. `./gradlew lint` — análisis estático
3. `./gradlew assembleDebug` — build completo

Los artefactos (APK, resultados de tests, informe de lint) se suben como artifacts con retención de 7 días.
