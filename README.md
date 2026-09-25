<p align="center">
  <img src="art/feature-graphic.png" alt="Mis Dineros — Tus suscripciones, bajo control" width="640">
</p>

[![Android CI](https://github.com/parrazam/mis-dineros/actions/workflows/android.yml/badge.svg)](https://github.com/parrazam/mis-dineros/actions/workflows/android.yml)
[![Release](https://img.shields.io/github/v/release/parrazam/mis-dineros)](https://github.com/parrazam/mis-dineros/releases/latest)

Gestor de suscripciones personal para Android. Sin servidores, sin telemetría, sin red — 100% local.

🌐 **Web** · [mis-dineros.cuzo.dev](https://mis-dineros.cuzo.dev/)
🔒 **Privacidad** · [mis-dineros.cuzo.dev/privacy.html](https://mis-dineros.cuzo.dev/privacy.html)

## Características

- **Suscripciones** — nombre, importe, moneda, ciclo mensual/anual, categoría, notas e icono personalizado (las imágenes subidas se reescalan a 512 px y se recomprimen a JPEG). En la lista, deslizar a la derecha pausa y a la izquierda elimina, ambas con confirmación
- **Dashboard** — gasto mensual real, equivalente anual, activas/pausadas, días hasta el próximo cargo, próximas renovaciones (7 días) y las 5 más caras
- **Renovaciones automáticas** — al vencer una fecha de renovación, la suscripción avanza sola al siguiente ciclo (reanclando el día de facturación original), de modo que nunca queda anclada en el pasado ni deja de notificarse
- **Estadísticas** — donut por categoría, vista global (gasto mensual y anual, reparto por ciclo, activas frente al total contratado) y top 5
- **Divisas** — conversión automática a moneda global con tasas de cambio editables y bundled. Si falta el par de conversión, la suscripción se excluye del total en vez de contarse a 1:1, y la moneda afectada se avisa en Inicio y Estadísticas
- **Notificaciones locales** — aviso configurable N días antes por suscripción + resumen mensual
- **Exportación e importación** — copia de seguridad con iconos embebidos, cifrado AES-256-GCM opcional y share sheet nativo (LocalSend, Telegram, Drive…)
- **Copia de seguridad automática** — Android Auto Backup a cuenta Google, activable/desactivable desde Ajustes
- **Temas** — claro / oscuro / sistema, seed color Blue Snorkel `#0077B6` sobre fondo cálido tipo papel, tipografías Bricolage Grotesque y Figtree con cifras tabulares, y colores dinámicos opcionales (Material You, Android 12+)
- **Categorías** — 9 predefinidas + creación libre

## Requisitos

- Android 8.0+ (API 26)
- Android Studio Meerkat o superior
- JDK 21

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
├── backup/         MisDinerosBackupAgent (Auto Backup)
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
| Gráficos | Compose Canvas (donut y barras propios) |
| Serialización | kotlinx.serialization |

## Backup

### Exportación/importación manual

El fichero exportado empieza por el magic `MDB1` (4 bytes) y un byte de flags que identifica el formato:

| Formato | Flags | Cabecera | Payload |
|---|---|---|---|
| Plano | `0x00` | 5B (magic + flags) | JSON UTF-8 |
| Cifrado sin AAD | `0x01` | 33B (magic + flags + salt 16B + IV 12B) | AES-256-GCM ciphertext |
| Cifrado con AAD | `0x02` | 33B (magic + flags + salt 16B + IV 12B) | AES-256-GCM ciphertext |

Todas las exportaciones cifradas nuevas usan `0x02`, que mete los 33 bytes de cabecera en GCM como AAD: así un byte de flags degradado a `0x01` o unos parámetros de KDF alterados hacen fallar el tag en vez de descifrar algo distinto. Los ficheros `0x01` anteriores se siguen descifrando sin AAD y deben poder leerse siempre; `BackupCryptoTest` guarda un fixture generado por una implementación independiente para garantizarlo.

El cifrado es opcional: el usuario lo activa al exportar e introduce una contraseña de al menos 8 caracteres (`BackupCrypto.MIN_PASSWORD_LENGTH`). La clave se deriva con PBKDF2WithHmacSHA256 (200 000 iteraciones, salt de 16 bytes e IV de 12 bytes aleatorios por fichero). Los ficheros exportados antes de que existiera la cabecera (JSON pelado) se importan sin cambios — compatibilidad total hacia atrás.

El JSON embebido tiene esta estructura (`version: 1`):

```json
{
  "version": 1,
  "exportedAt": "2026-05-08T10:00:00Z",
  "subscriptions": [...],
  "categories": [...],
  "fxRates": [...],
  "settings": {...},
  "assets": { "icon.png": "<base64>" }
}
```

Los iconos de usuario se embeben en `assets` como base64 y se restauran en `filesDir/icons/` al importar.

El share sheet nativo permite enviar el fichero directamente a Telegram, LocalSend, Drive, etc. La opción "Guardar en archivos" usa SAF (`CreateDocument`) para elegir ubicación en el dispositivo.

### Android Auto Backup

Con `allowBackup="true"` y `MisDinerosBackupAgent`, el sistema sube automáticamente la BD Room, DataStore, iconos de usuario y preferencias a la cuenta Google del dispositivo (máx. 25 MB, cifrado por Google). Por eso los iconos pasan siempre por `IconStorage`, que los reescala a 512 px y los reencoda como JPEG: unas pocas fotos de cámara a tamaño completo agotaban la cuota y dejaban de hacerse copias en silencio. Los ficheros que dejan de estar referenciados se borran al guardar, al eliminar y en el barrido de huérfanos del arranque. El usuario puede desactivarlo desde Ajustes → Datos. La flag se persiste en `SharedPreferences("auto_backup_prefs")` además de DataStore, para que el agente pueda leerla de forma síncrona.

## CI/CD

Hay dos workflows de GitHub Actions:

- **`android.yml`** — corre en cada push a `master`/`develop` y en cada PR a `master`: tests JVM, lint y `assembleDebug`. Sube APK de debug, resultados de tests e informe de lint como artifacts (7 días).
- **`release.yml`** — se dispara con tags `v*`. Construye el APK y AAB firmados, verifica la firma con `apksigner` y publica una GitHub Release con ambos artefactos.

Dependabot (`.github/dependabot.yml`) mantiene actualizadas las dependencias de Gradle (agrupando minor/patch en un solo PR) y las acciones de los workflows, con revisión semanal.

Buenas prácticas aplicadas:
- Todas las acciones están pinneadas a SHA (no a tags movibles).
- Permisos mínimos en cada workflow (`contents: read`, salvo `release.yml` que necesita `contents: write` para crear releases).
- El keystore de firma se decodifica desde un secret y se elimina al final del job.
- La firma del APK se verifica e imprime su huella SHA-256 en el log de cada release.

### Versionado automático

Tanto `versionName` como `versionCode` se derivan automáticamente del repositorio:

- `versionName` — `git describe --tags --always`, sin la `v` inicial. Ej.: tag `v1.0.0` → `1.0.0`.
- `versionCode` — derivado de `git describe --tags --long`: `major*1000000 + minor*10000 + patch*100 + commits desde la etiqueta` (tope 99). Ej.: tag `v1.5.1` → `1050100`. Crece con cada etiqueta, que es lo que exige Play Console, y a diferencia de `git rev-list --count` no baja con un rebase o un force-push.

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
