package com.parra.misdineros.presentation.subscriptions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.ui.graphics.vector.ImageVector

object BundledServiceIcons {

    data class CatalogEntry(
        val key: String,
        val displayName: String,
        val icon: ImageVector,
    )

    val catalog: List<CatalogEntry> = listOf(
        CatalogEntry("netflix", "Netflix", Icons.Default.LiveTv),
        CatalogEntry("disney", "Disney+", Icons.Default.ChildCare),
        CatalogEntry("amazon_prime", "Amazon Prime Video", Icons.Default.ShoppingCart),
        CatalogEntry("hbo", "Max / HBO", Icons.Default.Theaters),
        CatalogEntry("apple_tv", "Apple TV+", Icons.Default.Tv),
        CatalogEntry("youtube", "YouTube Premium", Icons.Default.OndemandVideo),
        CatalogEntry("twitch", "Twitch", Icons.Default.VideogameAsset),
        CatalogEntry("spotify", "Spotify", Icons.Default.Headphones),
        CatalogEntry("apple_music", "Apple Music", Icons.Default.MusicNote),
        CatalogEntry("youtube_music", "YouTube Music", Icons.Default.QueueMusic),
        CatalogEntry("amazon_music", "Amazon Music", Icons.Default.LibraryMusic),
        CatalogEntry("icloud", "iCloud+", Icons.Default.Cloud),
        CatalogEntry("google_one", "Google One", Icons.Default.Backup),
        CatalogEntry("dropbox", "Dropbox", Icons.Default.CloudDone),
        CatalogEntry("onedrive", "OneDrive", Icons.Default.CloudUpload),
        CatalogEntry("microsoft365", "Microsoft 365", Icons.Default.Apps),
        CatalogEntry("adobe", "Adobe Creative Cloud", Icons.Default.Brush),
        CatalogEntry("notion", "Notion", Icons.Default.Article),
        CatalogEntry("slack", "Slack", Icons.Default.ChatBubble),
        CatalogEntry("zoom", "Zoom", Icons.Default.VideoCall),
        CatalogEntry("github", "GitHub", Icons.Default.Code),
        CatalogEntry("nordvpn", "NordVPN", Icons.Default.VpnLock),
        CatalogEntry("password_mgr", "Gestor de contraseñas", Icons.Default.Lock),
        CatalogEntry("nintendo", "Nintendo Switch Online", Icons.Default.SportsEsports),
        CatalogEntry("xbox", "Xbox Game Pass", Icons.Default.VideogameAsset),
        CatalogEntry("playstation", "PlayStation Plus", Icons.Default.SportsEsports),
        CatalogEntry("chatgpt", "ChatGPT Plus", Icons.Default.SmartToy),
        CatalogEntry("midjourney", "Midjourney", Icons.Default.AutoAwesome),
        CatalogEntry("generic", "Otro servicio", Icons.Default.Subscriptions),
    )

    fun byKey(key: String): CatalogEntry? = catalog.find { it.key == key }
}
