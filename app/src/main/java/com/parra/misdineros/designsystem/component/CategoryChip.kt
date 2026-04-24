package com.parra.misdineros.designsystem.component

import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.parra.misdineros.domain.model.Category

@Composable
fun CategoryChip(category: Category, modifier: Modifier = Modifier) {
    val color = Color(category.colorArgb)
    AssistChip(
        onClick = {},
        label = { Text(category.name) },
        leadingIcon = {
            Icon(
                imageVector = categoryIconVector(category.iconKey),
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize),
                tint = color,
            )
        },
        modifier = modifier,
    )
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
