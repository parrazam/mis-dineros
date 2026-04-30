package com.parra.misdineros.designsystem.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parra.misdineros.domain.model.Category

@Composable
fun CategoryChip(category: Category, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = {},
        label = { Text(category.name) },
        leadingIcon = {
            CategoryIconContent(
                iconKey = category.iconKey,
                colorArgb = category.colorArgb,
                size = AssistChipDefaults.IconSize,
            )
        },
        modifier = modifier,
    )
}

/**
 * Renders a category icon in a colored circle.
 * iconKey formats:
 *   - "emoji:<char>"   → emoji text on colored background
 *   - "file:<path>"    → bitmap image cropped to circle
 *   - anything else    → Material icon on colored background (backward compat)
 */
@Composable
fun CategoryIconContent(
    iconKey: String,
    colorArgb: Int,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier,
) {
    val bgColor = Color(colorArgb)
    when {
        iconKey.startsWith("emoji:") -> {
            val emoji = iconKey.removePrefix("emoji:")
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji.ifEmpty { "?" }, fontSize = (size.value * 0.55f).sp)
            }
        }

        iconKey.startsWith("file:") -> {
            val path = iconKey.removePrefix("file:")
            val bitmap: ImageBitmap? = remember(path) {
                runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = modifier.size(size).clip(CircleShape),
                )
            } else {
                Box(
                    modifier = modifier.size(size).clip(CircleShape).background(bgColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.55f),
                    )
                }
            }
        }

        else -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = categoryIconVector(iconKey),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.55f),
                )
            }
        }
    }
}

fun categoryIconVector(iconKey: String): ImageVector = when (iconKey) {
    "live_tv" -> Icons.Default.LiveTv
    "music_note" -> Icons.Default.MusicNote
    "work" -> Icons.Default.Work
    "cloud" -> Icons.Default.Cloud
    "sports_esports" -> Icons.Default.SportsEsports
    "newspaper" -> Icons.Default.Newspaper
    "fitness_center" -> Icons.Default.FitnessCenter
    "smart_toy" -> Icons.Default.SmartToy
    "article" -> Icons.Default.Article
    else -> Icons.Default.Category
}
