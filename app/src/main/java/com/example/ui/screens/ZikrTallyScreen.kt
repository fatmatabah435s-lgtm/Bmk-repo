package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.QuickTargetOptions
import com.example.data.TallySessionState
import com.example.data.ZikrEntry
import com.example.ui.ZikrUiEvent
import com.example.ui.ZikrUiState
import com.example.ui.components.AddCustomZikrDialog
import com.example.ui.components.BeadRingOrb
import com.example.ui.components.ZikrLibraryBottomSheet
import com.example.ui.components.ZikrSwitcherStrip
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.CormorantGaramondFontFamily
import com.example.ui.theme.LocalSanctuaryColors
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun ZikrTallyScreen(
    uiState: ZikrUiState,
    events: SharedFlow<ZikrUiEvent>,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onUndoReset: () -> Unit,
    onTapOrb: () -> Unit,
    onDecrementOne: () -> Unit,
    onSelectEntry: (ZikrEntry) -> Unit,
    onChangeTarget: (Int) -> Unit,
    onAddCustomZikr: (name: String, arabic: String, meaning: String, target: Int) -> Unit,
    onDeleteCustomZikr: (String) -> Unit,
    onToggleHaptics: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val sanctuaryColors = LocalSanctuaryColors.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showLibrarySheet by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val startNudgeScale = remember { Animatable(1f) }
    LaunchedEffect(uiState.startNudgeTrigger) {
        if (uiState.startNudgeTrigger > 0) {
            startNudgeScale.snapTo(1f)
            startNudgeScale.animateTo(
                targetValue = 1.07f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessHigh
                )
            )
            startNudgeScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
        }
    }

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is ZikrUiEvent.ShowResetUndo -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.snackbar_reset_done, event.zikrName),
                        actionLabel = context.getString(R.string.action_undo),
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onUndoReset()
                    }
                }
                is ZikrUiEvent.RoundCompleted -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = context.getString(
                            R.string.snackbar_round_complete,
                            event.zikrName,
                            event.roundNumber
                        ),
                        duration = SnackbarDuration.Short
                    )
                }
                else -> Unit
            }
        }
    }

    val bgPrimaryGlow = colorScheme.primaryContainer.copy(alpha = 0.25f)
    val bgGoldGlow = sanctuaryColors.goldContainer.copy(alpha = 0.28f)
    val bgColor = colorScheme.background

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(color = bgColor)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(bgPrimaryGlow, bgColor.copy(alpha = 0f)),
                        center = Offset(size.width * 0.2f, size.height * 0.12f),
                        radius = size.maxDimension * 0.55f
                    ),
                    center = Offset(size.width * 0.2f, size.height * 0.12f),
                    radius = size.maxDimension * 0.55f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(bgGoldGlow, bgColor.copy(alpha = 0f)),
                        center = Offset(size.width * 0.85f, size.height * 0.85f),
                        radius = size.maxDimension * 0.5f
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.85f),
                    radius = size.maxDimension * 0.5f
                )
            },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWide = maxWidth >= 600.dp

            Column(modifier = Modifier.fillMaxSize()) {
                // Top Sanctuary Header Bar
                TopSanctuaryBar(
                    lifetimeTotal = uiState.lifetimeTotalAcrossAll,
                    hapticsEnabled = uiState.hapticsEnabled,
                    onToggleHaptics = onToggleHaptics,
                    onOpenLibrary = { showLibrarySheet = true }
                )

                if (isWide) {
                    // Two-pane layout for tablets / landscape
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                        ) {
                            ZikrSwitcherStrip(
                                entries = uiState.entries,
                                activeEntryId = uiState.activeEntry.id,
                                onSelectEntry = onSelectEntry,
                                onAddCustomClick = { showAddDialog = true }
                            )
                            ActiveZikrCard(
                                entry = uiState.activeEntry,
                                onCardClick = { showLibrarySheet = true },
                                onChangeTarget = onChangeTarget,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            BeadRingOrb(
                                count = uiState.count,
                                target = uiState.target,
                                roundsCompleted = uiState.roundsCompleted,
                                elapsedSeconds = uiState.elapsedSeconds,
                                sessionState = uiState.sessionState,
                                tapPulseTrigger = uiState.tapPulseTrigger,
                                onTapOrb = onTapOrb,
                                onDecrementOne = onDecrementOne,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .size(280.dp)
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            ControlButtonDock(
                                sessionState = uiState.sessionState,
                                canReset = uiState.canReset,
                                startNudgeScale = startNudgeScale.value,
                                onStartOrResume = onStartOrResume,
                                onPause = onPause,
                                onReset = onReset,
                                modifier = Modifier.widthIn(max = 420.dp)
                            )
                        }
                    }
                } else {
                    // Compact Portrait Phone Layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 560.dp)
                            .align(Alignment.CenterHorizontally),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))

                        // Main screen switcher strip with Add Zikr + per-entry separate counts
                        ZikrSwitcherStrip(
                            entries = uiState.entries,
                            activeEntryId = uiState.activeEntry.id,
                            onSelectEntry = onSelectEntry,
                            onAddCustomClick = { showAddDialog = true }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Active Zikr Phrase Card + Target selector
                        ActiveZikrCard(
                            entry = uiState.activeEntry,
                            onCardClick = { showLibrarySheet = true },
                            onChangeTarget = onChangeTarget,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )

                        // Center Tactile Bead Ring Orb
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BeadRingOrb(
                                count = uiState.count,
                                target = uiState.target,
                                roundsCompleted = uiState.roundsCompleted,
                                elapsedSeconds = uiState.elapsedSeconds,
                                sessionState = uiState.sessionState,
                                tapPulseTrigger = uiState.tapPulseTrigger,
                                onTapOrb = onTapOrb,
                                onDecrementOne = onDecrementOne,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Primary Start / Pause / Reset Dock
                        ControlButtonDock(
                            sessionState = uiState.sessionState,
                            canReset = uiState.canReset,
                            startNudgeScale = startNudgeScale.value,
                            onStartOrResume = onStartOrResume,
                            onPause = onPause,
                            onReset = onReset,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showLibrarySheet) {
        ZikrLibraryBottomSheet(
            entries = uiState.entries,
            activeEntryId = uiState.activeEntry.id,
            onDismiss = { showLibrarySheet = false },
            onSelectEntry = onSelectEntry,
            onAddCustomClick = {
                showLibrarySheet = false
                showAddDialog = true
            },
            onDeleteCustomEntry = onDeleteCustomZikr
        )
    }

    if (showAddDialog) {
        AddCustomZikrDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, arabic, meaning, target ->
                onAddCustomZikr(name, arabic, meaning, target)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun TopSanctuaryBar(
    lifetimeTotal: Int,
    hapticsEnabled: Boolean,
    onToggleHaptics: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val sanctuaryColors = LocalSanctuaryColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = CormorantGaramondFontFamily
                ),
                color = colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.label_lifetime_total, lifetimeTotal),
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = sanctuaryColors.goldAccent
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onToggleHaptics,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("haptic_toggle_button")
            ) {
                Icon(
                    imageVector = if (hapticsEnabled) Icons.Rounded.Vibration else Icons.Outlined.Vibration,
                    contentDescription = stringResource(R.string.cd_haptic_toggle),
                    tint = if (hapticsEnabled) colorScheme.primary else colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onOpenLibrary,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("open_zikr_sheet_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.LibraryBooks,
                    contentDescription = stringResource(R.string.cd_choose_zikr),
                    tint = colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ActiveZikrCard(
    entry: ZikrEntry,
    onCardClick: () -> Unit,
    onChangeTarget: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val sanctuaryColors = LocalSanctuaryColors.current

    Surface(
        onClick = onCardClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_zikr_card"),
        shape = RoundedCornerShape(22.dp),
        color = colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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

                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = stringResource(R.string.cd_choose_zikr),
                    tint = colorScheme.onSurfaceVariant
                )
            }

            if (entry.arabic.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entry.arabic,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = AmiriFontFamily,
                        fontSize = 26.sp
                    ),
                    color = colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Cycle Target Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickTargetOptions.forEach { targetOption ->
                    val selected = entry.target == targetOption
                    Surface(
                        onClick = { onChangeTarget(targetOption) },
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .testTag("target_chip_$targetOption"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) {
                            colorScheme.primary
                        } else {
                            colorScheme.surfaceContainerHigh
                        }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (targetOption == 0) "∞" else targetOption.toString(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFeatureSettings = "tnum"
                                ),
                                color = if (selected) {
                                    colorScheme.onPrimary
                                } else {
                                    colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlButtonDock(
    sessionState: TallySessionState,
    canReset: Boolean,
    startNudgeScale: Float,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val sanctuaryColors = LocalSanctuaryColors.current

    val isActive = sessionState == TallySessionState.ACTIVE
    val isPaused = sessionState == TallySessionState.PAUSED

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // START / RESUME BUTTON
        Button(
            onClick = onStartOrResume,
            enabled = !isActive,
            modifier = Modifier
                .weight(1.2f)
                .heightIn(min = 54.dp)
                .scale(startNudgeScale)
                .testTag("start_button"),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                disabledContainerColor = colorScheme.primaryContainer.copy(alpha = 0.55f),
                disabledContentColor = colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isPaused) stringResource(R.string.action_resume) else stringResource(R.string.action_start),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1
            )
        }

        // PAUSE BUTTON
        FilledTonalButton(
            onClick = onPause,
            enabled = isActive,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 54.dp)
                .testTag("pause_button"),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = sanctuaryColors.goldContainer,
                contentColor = sanctuaryColors.onGoldContainer,
                disabledContainerColor = colorScheme.surfaceContainer,
                disabledContentColor = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Pause,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.action_pause),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }

        // RESET BUTTON
        OutlinedButton(
            onClick = onReset,
            enabled = canReset,
            modifier = Modifier
                .weight(0.95f)
                .heightIn(min = 54.dp)
                .testTag("reset_button"),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
            border = BorderStroke(
                width = 1.dp,
                color = if (canReset) colorScheme.outline else colorScheme.outlineVariant
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stringResource(R.string.action_reset),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}
