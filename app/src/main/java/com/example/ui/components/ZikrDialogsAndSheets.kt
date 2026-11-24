package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.QuickTargetOptions
import com.example.data.TallySessionState
import com.example.data.ZikrEntry
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.CormorantGaramondFontFamily
import com.example.ui.theme.LocalSanctuaryColors

@Composable
fun AddCustomZikrDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, arabic: String, meaning: String, target: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var arabic by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("33") }
    var selectedQuickTarget by remember { mutableIntStateOf(33) }
    var showNameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_title_add_zikr),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = CormorantGaramondFontFamily
                )
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (showNameError && it.isNotBlank()) showNameError = false
                    },
                    label = { Text(stringResource(R.string.field_custom_title)) },
                    isError = showNameError,
                    supportingText = if (showNameError) {
                        { Text(stringResource(R.string.error_empty_name)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_zikr_name_input")
                )

                OutlinedTextField(
                    value = arabic,
                    onValueChange = { arabic = it },
                    label = { Text(stringResource(R.string.field_custom_arabic)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_zikr_arabic_input")
                )

                OutlinedTextField(
                    value = meaning,
                    onValueChange = { meaning = it },
                    label = { Text(stringResource(R.string.field_custom_meaning)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_zikr_meaning_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickTargetOptions.forEach { option ->
                        FilterChip(
                            selected = selectedQuickTarget == option && targetText == option.toString(),
                            onClick = {
                                selectedQuickTarget = option
                                targetText = option.toString()
                            },
                            label = {
                                Text(if (option == 0) "∞" else option.toString())
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(5)
                        targetText = digits
                        selectedQuickTarget = digits.toIntOrNull() ?: -1
                    },
                    label = { Text(stringResource(R.string.field_custom_target)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_zikr_target_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        showNameError = true
                    } else {
                        val parsedTarget = targetText.toIntOrNull() ?: 33
                        onSave(name, arabic, meaning, parsedTarget)
                    }
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("save_custom_zikr_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZikrLibraryBottomSheet(
    entries: List<ZikrEntry>,
    activeEntryId: String,
    onDismiss: () -> Unit,
    onSelectEntry: (ZikrEntry) -> Unit,
    onAddCustomClick: () -> Unit,
    onDeleteCustomEntry: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colorScheme = MaterialTheme.colorScheme
    val sanctuaryColors = LocalSanctuaryColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Decorative Sanctuary Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(124.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_sanctuary_banner_1795372135305),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.2f),
                                    Color.Black.copy(alpha = 0.72f)
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.sheet_title_select_zikr),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = CormorantGaramondFontFamily
                            ),
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.sheet_subtitle_select_zikr),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        onClick = onAddCustomClick,
                        shape = RoundedCornerShape(14.dp),
                        color = sanctuaryColors.goldContainer,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("sheet_add_custom_zikr_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = sanctuaryColors.onGoldContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.action_add_zikr),
                                style = MaterialTheme.typography.labelLarge,
                                color = sanctuaryColors.onGoldContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 420.dp)
            ) {
                items(
                    items = entries,
                    key = { it.id }
                ) { entry ->
                    val isSelected = entry.id == activeEntryId
                    val sessionState = entry.parsedSessionState
                    Surface(
                        onClick = {
                            onSelectEntry(entry)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) {
                            colorScheme.primaryContainer.copy(alpha = 0.72f)
                        } else {
                            colorScheme.surfaceContainer
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) colorScheme.primary else colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sheet_zikr_item_${entry.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = entry.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colorScheme.onSurface
                                    )
                                    if (entry.isCustom) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = sanctuaryColors.goldContainer
                                        ) {
                                            Text(
                                                text = stringResource(R.string.label_custom_badge),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = sanctuaryColors.onGoldContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (sessionState == TallySessionState.PAUSED) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = sanctuaryColors.pausedTint.copy(alpha = 0.16f)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.status_paused),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = sanctuaryColors.pausedTint,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                if (entry.arabic.isNotBlank()) {
                                    Text(
                                        text = entry.arabic,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = AmiriFontFamily
                                        ),
                                        color = colorScheme.primary
                                    )
                                }
                                if (entry.meaning.isNotBlank()) {
                                    Text(
                                        text = entry.meaning,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) colorScheme.primary else colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        text = if (entry.target > 0) "${entry.count} / ${entry.target}" else "${entry.count}",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontFeatureSettings = "tnum",
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.label_lifetime_total, entry.totalCount),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    color = colorScheme.onSurfaceVariant
                                )
                            }

                            if (entry.isCustom) {
                                IconButton(
                                    onClick = { onDeleteCustomEntry(entry.id) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("delete_custom_${entry.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = stringResource(R.string.cd_delete_custom_zikr),
                                        tint = colorScheme.error
                                    )
                                }
                            } else if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
