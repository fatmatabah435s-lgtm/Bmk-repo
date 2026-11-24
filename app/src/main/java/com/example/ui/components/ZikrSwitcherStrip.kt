package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.TallySessionState
import com.example.data.ZikrEntry
import com.example.ui.theme.LocalSanctuaryColors

@Composable
fun ZikrSwitcherStrip(
    entries: List<ZikrEntry>,
    activeEntryId: String,
    onSelectEntry: (ZikrEntry) -> Unit,
    onAddCustomClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val sanctuaryColors = LocalSanctuaryColors.current

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item(key = "add_custom_zikr") {
            Surface(
                onClick = onAddCustomClick,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("add_custom_zikr_chip"),
                shape = RoundedCornerShape(16.dp),
                color = sanctuaryColors.goldContainer,
                border = BorderStroke(1.dp, sanctuaryColors.goldAccent.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.cd_add_custom_zikr),
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

        items(
            items = entries,
            key = { it.id }
        ) { entry ->
            val isSelected = entry.id == activeEntryId
            val entryState = entry.parsedSessionState
            val badgeText = if (entry.target > 0) {
                "${entry.count}/${entry.target}"
            } else {
                "${entry.count}"
            }

            Surface(
                onClick = { onSelectEntry(entry) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("zikr_chip_${entry.id}"),
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) {
                    colorScheme.primaryContainer
                } else {
                    colorScheme.surfaceContainer
                },
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) {
                        colorScheme.primary
                    } else {
                        colorScheme.outlineVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (entry.isCustom) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = if (isSelected) colorScheme.primary else sanctuaryColors.goldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    } else if (entryState == TallySessionState.ACTIVE || entryState == TallySessionState.PAUSED) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (entryState == TallySessionState.ACTIVE) {
                                        colorScheme.primary
                                    } else {
                                        sanctuaryColors.pausedTint
                                    }
                                )
                        )
                    }

                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) {
                            colorScheme.onPrimaryContainer
                        } else {
                            colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) {
                            colorScheme.primary
                        } else {
                            colorScheme.surfaceContainerHigh
                        }
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFeatureSettings = "tnum",
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (isSelected) {
                                colorScheme.onPrimary
                            } else {
                                colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
