package com.parra.misdineros.designsystem.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.parra.misdineros.presentation.subscriptions.BundledServiceIcons

private val InitialColors = listOf(
    Color(0xFF1565C0), Color(0xFF6A1B9A), Color(0xFF00695C),
    Color(0xFFAD1457), Color(0xFF2E7D32), Color(0xFFE65100),
    Color(0xFF37474F), Color(0xFF558B2F),
)

@Composable
fun ServiceIcon(
    iconRef: String,
    modifier: Modifier = Modifier,
    fallbackName: String = "",
    size: Dp = 48.dp,
) {
    when {
        iconRef.startsWith("bundled:") -> {
            val key = iconRef.removePrefix("bundled:")
            val entry = BundledServiceIcons.byKey(key)
            if (entry != null) {
                Box(
                    modifier = modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = entry.icon,
                        contentDescription = entry.displayName,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(size * 0.55f),
                    )
                }
            } else {
                InitialIcon(fallbackName, size, modifier)
            }
        }

        iconRef.startsWith("file:") -> {
            val path = iconRef.removePrefix("file:")
            val bitmap: ImageBitmap? = remember(path) {
                runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
            }
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                        .size(size)
                        .clip(CircleShape),
                )
            } else {
                InitialIcon(fallbackName, size, modifier)
            }
        }

        else -> InitialIcon(fallbackName, size, modifier)
    }
}

@Composable
private fun InitialIcon(name: String, size: Dp, modifier: Modifier = Modifier) {
    val letter = name.firstOrNull()?.uppercaseChar() ?: '?'
    val bgColor = if (name.isNotEmpty()) {
        InitialColors[name.hashCode().and(Int.MAX_VALUE) % InitialColors.size]
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
    }
}
