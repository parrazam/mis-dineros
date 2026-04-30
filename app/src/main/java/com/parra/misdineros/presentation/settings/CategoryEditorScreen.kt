package com.parra.misdineros.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parra.misdineros.R
import com.parra.misdineros.designsystem.component.CategoryIconContent
import com.parra.misdineros.designsystem.component.categoryIconVector
import com.parra.misdineros.domain.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.BreakIterator
import java.util.UUID

private fun isValidSingleEmoji(text: String): Boolean {
    if (text.isEmpty()) return false
    val bi = BreakIterator.getCharacterInstance()
    bi.setText(text)
    bi.next()
    if (bi.next() != BreakIterator.DONE) return false  // more than one grapheme cluster
    return text.codePoints().anyMatch { cp ->
        cp in 0x1F000..0x1FAFF ||  // Modern emoji (supplementary plane)
        cp in 0x2600..0x27BF ||    // Misc Symbols + Dingbats (☀️, ❤️, ✅)
        cp in 0x2300..0x23FF ||    // Misc Technical (⌚, ⏰)
        cp in 0x25A0..0x25FF ||    // Geometric Shapes (▶)
        cp in 0x2B00..0x2BFF       // Misc Arrows/Symbols (⭐, ⬆)
    }
}

private fun takeFirstGraphemeCluster(text: String): String {
    if (text.isEmpty()) return text
    val bi = BreakIterator.getCharacterInstance()
    bi.setText(text)
    val end = bi.next()
    return if (end != BreakIterator.DONE) text.substring(0, end) else text
}

private val CATEGORY_COLORS = listOf(
    0xFFE53935.toInt(),
    0xFF8E24AA.toInt(),
    0xFF1E88E5.toInt(),
    0xFF00ACC1.toInt(),
    0xFF43A047.toInt(),
    0xFFF4511E.toInt(),
    0xFFE91E63.toInt(),
    0xFF00897B.toInt(),
    0xFF546E7A.toInt(),
    0xFFF57C00.toInt(),
    0xFF6D4C41.toInt(),
    0xFF039BE5.toInt(),
)

private val CATEGORY_ICON_KEYS = listOf(
    "live_tv", "music_note", "work", "cloud", "sports_esports",
    "newspaper", "fitness_center", "smart_toy", "article", "category",
)

private enum class IconMode { MATERIAL, EMOJI, IMAGE }

private fun iconModeOf(key: String) = when {
    key.startsWith("emoji:") -> IconMode.EMOJI
    key.startsWith("file:") -> IconMode.IMAGE
    else -> IconMode.MATERIAL
}

private data class DialogState(
    val isVisible: Boolean = false,
    val editing: Category? = null,
    val name: String = "",
    val iconKey: String = CATEGORY_ICON_KEYS.last(),
    val colorArgb: Int = CATEGORY_COLORS[2],
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryEditorViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf(DialogState()) }
    var deleteTarget by remember { mutableStateOf<Category?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val path = withContext(Dispatchers.IO) {
                    runCatching {
                        val dir = File(context.filesDir, "category_icons").also { it.mkdirs() }
                        val dest = File(dir, "${UUID.randomUUID()}.jpg")
                        context.contentResolver.openInputStream(uri)!!.use { i ->
                            dest.outputStream().use { o -> i.copyTo(o) }
                        }
                        dest.absolutePath
                    }.getOrNull()
                }
                if (path != null) dialog = dialog.copy(iconKey = "file:$path")
            }
        }
    }

    // ── Add/Edit dialog ───────────────────────────────────────────────────────
    if (dialog.isVisible) {
        CategoryDialog(
            title = if (dialog.editing == null) "Nueva categoría" else "Editar categoría",
            name = dialog.name,
            iconKey = dialog.iconKey,
            colorArgb = dialog.colorArgb,
            onNameChange = { dialog = dialog.copy(name = it) },
            onIconChange = { dialog = dialog.copy(iconKey = it) },
            onColorChange = { dialog = dialog.copy(colorArgb = it) },
            onPickImage = { imageLauncher.launch("image/*") },
            onConfirm = {
                val cat = dialog.editing?.copy(
                    name = dialog.name.trim(),
                    iconKey = dialog.iconKey,
                    colorArgb = dialog.colorArgb,
                ) ?: viewModel.newCategory(dialog.name, dialog.iconKey, dialog.colorArgb)
                viewModel.upsert(cat)
                dialog = DialogState()
            },
            onDismiss = { dialog = DialogState() },
        )
    }

    // ── Delete confirmation ────────────────────────────────────────────────────
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Eliminar categoría") },
            text = { Text("¿Eliminar «${target.name}»? Las suscripciones que la usen conservarán su id de categoría.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(target.id)
                    deleteTarget = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorías") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { dialog = DialogState(isVisible = true) }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir categoría")
            }
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            items(categories, key = { it.id }) { category ->
                CategoryRow(
                    category = category,
                    onEdit = {
                        dialog = DialogState(
                            isVisible = true,
                            editing = category,
                            name = category.name,
                            iconKey = category.iconKey,
                            colorArgb = category.colorArgb,
                        )
                    },
                    onDelete = { deleteTarget = category },
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(category: Category, onEdit: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(category.name) },
        leadingContent = {
            CategoryIconContent(
                iconKey = category.iconKey,
                colorArgb = category.colorArgb,
                size = 36.dp,
            )
        },
        supportingContent = if (category.isBuiltIn) {
            { Text("Predefinida", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else null,
        trailingContent = {
            if (!category.isBuiltIn) {
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDialog(
    title: String,
    name: String,
    iconKey: String,
    colorArgb: Int,
    onNameChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onColorChange: (Int) -> Unit,
    onPickImage: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val mode = iconModeOf(iconKey)
    val emojiValue = iconKey.removePrefix("emoji:")
    val emojiValid = mode != IconMode.EMOJI || isValidSingleEmoji(emojiValue)
    val iconValid = emojiValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Icono", style = MaterialTheme.typography.labelMedium)

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        IconMode.MATERIAL to "Predefinido",
                        IconMode.EMOJI to "Emoji",
                        IconMode.IMAGE to "Imagen",
                    ).forEachIndexed { index, (m, label) ->
                        SegmentedButton(
                            selected = mode == m,
                            onClick = {
                                when (m) {
                                    IconMode.MATERIAL -> if (mode != IconMode.MATERIAL) onIconChange(CATEGORY_ICON_KEYS.last())
                                    IconMode.EMOJI -> if (mode != IconMode.EMOJI) onIconChange("emoji:")
                                    IconMode.IMAGE -> onPickImage()
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                when (mode) {
                    IconMode.MATERIAL -> IconPickerRow(selected = iconKey, onSelect = onIconChange)
                    IconMode.EMOJI -> {
                        val emojiError = emojiValue.isNotEmpty() && !isValidSingleEmoji(emojiValue)
                        OutlinedTextField(
                            value = emojiValue,
                            onValueChange = { raw ->
                                val single = takeFirstGraphemeCluster(raw)
                                onIconChange("emoji:$single")
                            },
                            label = { Text("Emoji") },
                            placeholder = { Text("🎮") },
                            isError = emojiError,
                            supportingText = {
                                if (emojiError) Text("Solo se permite un emoji")
                                else Text("Escribe o pega un emoji")
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    IconMode.IMAGE -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CategoryIconContent(iconKey = iconKey, colorArgb = colorArgb, size = 48.dp)
                            OutlinedButton(onClick = onPickImage) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("Cambiar imagen")
                            }
                        }
                    }
                }

                Text("Color", style = MaterialTheme.typography.labelMedium)
                ColorPickerRow(selected = colorArgb, onSelect = onColorChange)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = name.isNotBlank() && iconValid) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun IconPickerRow(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CATEGORY_ICON_KEYS.forEach { key ->
            val isSelected = key == selected
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = categoryIconVector(key),
                    contentDescription = key,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun ColorPickerRow(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CATEGORY_COLORS.forEach { color ->
            val isSelected = color == selected
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .then(
                        if (isSelected)
                            Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        else Modifier
                    )
                    .clickable { onSelect(color) },
            )
        }
    }
}
