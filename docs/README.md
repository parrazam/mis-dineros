# Sitio web de Mis Dineros

Sitio público servido vía GitHub Pages desde esta carpeta `docs/`.

🌐 **https://mis-dineros.cuzo.dev/**

Contiene:

- `index.html` — landing principal
- `privacy.html` — política de privacidad (URL exigida por Google Play Console)
- `styles.css` — paleta exacta de la app (Pantone Blue Snorkel `#0077B6`)

## Stack

HTML + CSS sin frameworks ni build.

## Desarrollo local

```bash
cd docs
python3 -m http.server 8000
# abrir http://localhost:8000
```

## Despliegue

Configurado en **Settings → Pages**:

- Source: `Deploy from a branch`
- Branch: `master` / `/docs`

El deploy es automático en cada push a `master` que toque `docs/`.

## Tipografías

- [Fraunces](https://fonts.google.com/specimen/Fraunces) — display
- [IBM Plex Sans](https://fonts.google.com/specimen/IBM+Plex+Sans) — body
- [JetBrains Mono](https://fonts.google.com/specimen/JetBrains+Mono) — detalles

Servidas vía Google Fonts.
