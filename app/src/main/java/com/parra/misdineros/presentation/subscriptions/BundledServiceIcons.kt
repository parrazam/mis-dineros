package com.parra.misdineros.presentation.subscriptions

import androidx.annotation.DrawableRes
import com.parra.misdineros.R

object BundledServiceIcons {

    data class CatalogEntry(
        val key: String,
        val displayName: String,
        @DrawableRes val iconRes: Int,
    )

    val catalog: List<CatalogEntry> = listOf(
        // Vídeo
        CatalogEntry("netflix",       "Netflix",               R.drawable.ic_brand_netflix),
        CatalogEntry("disney",        "Disney+",               R.drawable.ic_brand_disney),
        CatalogEntry("amazon_prime",  "Prime Video",           R.drawable.ic_brand_amazon_prime),
        CatalogEntry("hbo",           "Max / HBO",             R.drawable.ic_brand_hbo),
        CatalogEntry("apple_tv",      "Apple TV+",             R.drawable.ic_brand_apple_tv),
        CatalogEntry("youtube",       "YouTube Premium",       R.drawable.ic_brand_youtube),
        CatalogEntry("twitch",        "Twitch",                R.drawable.ic_brand_twitch),
        // Música
        CatalogEntry("spotify",       "Spotify",               R.drawable.ic_brand_spotify),
        CatalogEntry("apple_music",   "Apple Music",           R.drawable.ic_brand_apple_music),
        CatalogEntry("youtube_music", "YouTube Music",         R.drawable.ic_brand_youtube_music),
        CatalogEntry("amazon_music",  "Amazon Music",          R.drawable.ic_brand_amazon_music),
        // Almacenamiento
        CatalogEntry("icloud",        "iCloud+",               R.drawable.ic_brand_icloud),
        CatalogEntry("google_one",    "Google One",            R.drawable.ic_brand_google_one),
        CatalogEntry("dropbox",       "Dropbox",               R.drawable.ic_brand_dropbox),
        CatalogEntry("onedrive",      "OneDrive",              R.drawable.ic_brand_onedrive),
        // Productividad
        CatalogEntry("microsoft365",  "Microsoft 365",         R.drawable.ic_brand_microsoft365),
        CatalogEntry("adobe",         "Adobe Creative Cloud",  R.drawable.ic_brand_adobe),
        CatalogEntry("notion",        "Notion",                R.drawable.ic_brand_notion),
        CatalogEntry("slack",         "Slack",                 R.drawable.ic_brand_slack),
        CatalogEntry("zoom",          "Zoom",                  R.drawable.ic_brand_zoom),
        CatalogEntry("github",        "GitHub",                R.drawable.ic_brand_github),
        // Seguridad
        CatalogEntry("nordvpn",       "NordVPN",               R.drawable.ic_brand_nordvpn),
        CatalogEntry("password_mgr",  "Bitwarden / 1Password", R.drawable.ic_brand_bitwarden),
        // Gaming
        CatalogEntry("nintendo",      "Nintendo Switch Online", R.drawable.ic_brand_nintendo),
        CatalogEntry("xbox",          "Xbox Game Pass",        R.drawable.ic_brand_xbox),
        CatalogEntry("playstation",   "PlayStation Plus",      R.drawable.ic_brand_playstation),
        // IA
        CatalogEntry("chatgpt",       "ChatGPT Plus",          R.drawable.ic_brand_openai),
        CatalogEntry("midjourney",    "Midjourney",            R.drawable.ic_brand_midjourney),
        // Genérico
        CatalogEntry("generic",       "Otro servicio",         R.drawable.ic_brand_generic),
    )

    fun byKey(key: String): CatalogEntry? = catalog.find { it.key == key }
}
