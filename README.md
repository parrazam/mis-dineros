# Mis Dineros

[![Android CI](https://github.com/parrazam/mis-dineros/actions/workflows/android.yml/badge.svg)](https://github.com/parrazam/mis-dineros/actions/workflows/android.yml)
[![Release](https://img.shields.io/github/v/release/parrazam/mis-dineros)](https://github.com/parrazam/mis-dineros/releases/latest)

Gestor de suscripciones personal para Android. Sin servidores, sin telemetría, sin red — 100% local.

🌐 **Web** · [parrazam.github.io/mis-dineros](https://parrazam.github.io/mis-dineros/)
🔒 **Privacidad** · [parrazam.github.io/mis-dineros/privacy.html](https://parrazam.github.io/mis-dineros/privacy.html)

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

## CI/CD

Hay dos workflows de GitHub Actions:

- **`android.yml`** — corre en cada push/PR a `master`: tests JVM, lint y `assembleDebug`. Sube APK de debug, resultados de tests e informe de lint como artifacts (7 días).
- **`release.yml`** — se dispara con tags `v*`. Construye el APK y AAB firmados, verifica la firma con `apksigner` y publica una GitHub Release con ambos artefactos.

Buenas prácticas aplicadas:
- Todas las acciones están pinneadas a SHA (no a tags movibles).
- Permisos mínimos en cada workflow (`contents: read`, salvo `release.yml` que necesita `contents: write` para crear releases).
- El keystore de firma se decodifica desde un secret y se elimina al final del job.
- La firma del APK se verifica e imprime su huella SHA-256 en el log de cada release.

### Versionado automático

Tanto `versionName` como `versionCode` se derivan automáticamente del repositorio:

- `versionName` — `git describe --tags --always`, sin la `v` inicial. Ej.: tag `v1.0.0` → `1.0.0`.
- `versionCode` — `git rev-list --count HEAD`. Estrictamente creciente, lo que satisface el requisito de Play Console.

Por eso el workflow de release hace `fetch-depth: 0` y `fetch-tags: true`: sin el historial completo, ambos comandos darían valores erróneos en el runner.

## Releases firmados

Los APKs publicados en [Releases](https://github.com/parrazam/mis-dineros/releases) están firmados con la misma clave que la build de Play Store.

Para verificar la firma de un APK descargado:

```bash
$ANDROID_HOME/build-tools/<version>/apksigner verify --print-certs mis-dineros-v1.0.0.apk
```

## Configuración de secrets (mantenedores)

El workflow de release necesita estos secrets configurados en **Settings → Secrets and variables → Actions**:

| Secret | Descripción |
|---|---|
| `KEYSTORE_BASE64` | Keystore JKS codificado en base64: `base64 -w0 release.jks` |
| `KEYSTORE_PASSWORD` | Contraseña del keystore |
| `KEY_PASSWORD` | Contraseña del alias (igual a la del keystore si nunca se separaron) |
| `KEY_ALIAS` | Alias de la clave dentro del keystore |
