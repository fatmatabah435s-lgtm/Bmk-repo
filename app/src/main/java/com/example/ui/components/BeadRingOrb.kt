package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExposureNeg1
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.TallySessionState
import com.example.ui.theme.CormorantGaramondFontFamily
import com.example.ui.theme.LocalSanctuaryColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BeadRingOrb(
    count: Int,
    target: Int,
    roundsCompleted: Int,
    elapsedSeconds: Long,
    sessionState: TallySessionState,
    tapPulseTrigger: Int,
    onTapOrb: () -> Unit,
    onDecrementOne: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sanctuaryColors = LocalSanctuaryColors.current
    val colorScheme = MaterialTheme.colorScheme

    val isActive = sessionState == TallySessionState.ACTIVE
    val isPaused = sessionState == TallySessionState.PAUSED

    val targetProgress = remember(count, target) {
        if (target > 0) (count.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "orbProgress"
    )

    // Bounce and expanding wave animation on each tap
    val tapScale = remember { Animatable(1f) }
    val rippleWave = remember { Animatable(0f) }
    LaunchedEffect(tapPulseTrigger) {
        if (tapPulseTrigger > 0) {
            tapScale.snapTo(0.96f)
            rippleWave.snapTo(0f)
            tapScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }
    LaunchedEffect(tapPulseTrigger) {
        if (tapPulseTrigger > 0) {
            rippleWave.snapTo(0f)
            rippleWave.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "activeGlow")
    val activeHaloPulse by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloPulse"
    )

    val statusColor by animateColorAsState(
        targetValue = when (sessionState) {
            TallySessionState.ACTIVE -> colorScheme.primary
            TallySessionState.PAUSED -> sanctuaryColors.pausedTint
            TallySessionState.READY -> colorScheme.onSurfaceVariant
        },
        label = "statusColor"
    )

    val cdTallyOrb = stringResource(R.string.cd_tally_orb)
    val cdDecrement = stringResource(R.string.cd_decrement_one)

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val orbDiameter = min(maxWidth, maxHeight).coerceIn(220.dp, 340.dp)
        val innerDiameter = orbDiameter * 0.74f
        val beadCount = when {
            target in 1..36 -> target
            else -> 33
        }

        Box(
            modifier = Modifier.size(orbDiameter),
            contentAlignment = Alignment.Center
        ) {
            // Canvas: ambient glow, expanding tap wave, progress arc, and circular prayer beads
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.minDimension / 2f
                val beadRingRadius = maxRadius * 0.87f
                val arcRadius = maxRadius * 0.79f

                // Ambient glow when active
                if (isActive) {
                    val glowRadius = maxRadius * activeHaloPulse
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                sanctuaryColors.orbGlowStart.copy(alpha = 0.85f),
                                sanctuaryColors.orbGlowEnd.copy(alpha = 0.15f),
                                colorScheme.background.copy(alpha = 0f)
                            ),
                            center = center,
                            radius = glowRadius
                        ),
                        radius = glowRadius,
                        center = center
                    )
                }

                // Expanding tap ripple wave
                if (rippleWave.value > 0f && rippleWave.value < 1f) {
                    val waveRadius = (maxRadius * 0.65f) + (maxRadius * 0.33f * rippleWave.value)
                    drawCircle(
                        color = sanctuaryColors.beadHighlight.copy(alpha = (1f - rippleWave.value) * 0.45f),
                        radius = waveRadius,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // Background arc track
                drawCircle(
                    color = sanctuaryColors.beadUpcoming.copy(alpha = 0.5f),
                    radius = arcRadius,
                    center = center,
                    style = Stroke(width = 5.dp.toPx())
                )

                // Active progress arc
                if (target > 0 && animatedProgress > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                sanctuaryColors.beadCompleted,
                                sanctuaryColors.beadHighlight,
                                sanctuaryColors.beadCompleted
                            ),
                            center = center
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                        size = Size(arcRadius * 2f, arcRadius * 2f),
                        style = Stroke(width = 5.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Prayer beads arranged in a circle
                val filledBeads = if (target > 0) {
                    ((count.toFloat() / target.toFloat()) * beadCount).toInt().coerceIn(0, beadCount)
                } else {
                    count % beadCount
                }

                for (i in 0 until beadCount) {
                    val angleRad = (-90.0 + (360.0 / beadCount) * i) * (PI / 180.0)
                    val bx = center.x + (beadRingRadius * cos(angleRad)).toFloat()
                    val by = center.y + (beadRingRadius * sin(angleRad)).toFloat()
                    val beadCenter = Offset(bx, by)

                    val isCurrentHead = if (target > 0) {
                        filledBeads > 0 && i == (filledBeads - 1).coerceAtLeast(0)
                    } else {
                        count > 0 && i == ((count - 1) % beadCount)
                    }
                    val isFilled = if (target > 0) {
                        i < filledBeads
                    } else {
                        i <= ((count - 1) % beadCount) && count > 0
                    }

                    val beadRadius = when {
                        isCurrentHead -> 5.2.dp.toPx()
                        isFilled -> 3.8.dp.toPx()
                        else -> 3.0.dp.toPx()
                    }
                    val beadColor = when {
                        isCurrentHead -> sanctuaryColors.beadHighlight
                        isFilled -> sanctuaryColors.beadCompleted
                        else -> sanctuaryColors.beadUpcoming
                    }

                    if (isCurrentHead) {
                        drawCircle(
                            color = sanctuaryColors.beadHighlight.copy(alpha = 0.28f),
                            radius = beadRadius + 4.dp.toPx(),
                            center = beadCenter
                        )
                    }
                    drawCircle(
                        color = beadColor,
                        radius = beadRadius,
                        center = beadCenter
                    )
                }
            }

            // Inner Tactile Tap Orb
            val interactionSource = remember { MutableInteractionSource() }
            Surface(
                modifier = Modifier
                    .size(innerDiameter)
                    .scale(tapScale.value)
                    .clip(CircleShape)
                    .testTag("tally_orb_button")
                    .semantics { contentDescription = cdTallyOrb }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(
                            bounded = true,
                            color = if (isActive) colorScheme.primary else sanctuaryColors.pausedTint
                        ),
                        role = Role.Button,
                        onClick = onTapOrb
                    ),
                shape = CircleShape,
                color = if (isActive) {
                    colorScheme.surfaceContainerHigh
                } else {
                    colorScheme.surfaceContainer
                },
                tonalElevation = if (isActive) 6.dp else 2.dp,
                shadowElevation = if (isActive) 8.dp else 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusColor.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isPaused) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                            }
                            Text(
                                text = when (sessionState) {
                                    TallySessionState.READY -> stringResource(R.string.status_ready)
                                    TallySessionState.ACTIVE -> stringResource(R.string.status_active)
                                    TallySessionState.PAUSED -> stringResource(R.string.status_paused)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Hero Count Numeral
                    AnimatedContent(
                        targetState = count,
                        transitionSpec = {
                            (fadeIn(tween(120)) + scaleIn(initialScale = 0.92f)) togetherWith
                                fadeOut(tween(90))
                        },
                        label = "countNumeral"
                    ) { displayedCount ->
                        Text(
                            text = displayedCount.toString(),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = CormorantGaramondFontFamily,
                                fontSize = if (displayedCount >= 1000) 50.sp else 62.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = if (isPaused) {
                                colorScheme.onSurface.copy(alpha = 0.72f)
                            } else {
                                colorScheme.onSurface
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }

                    // Target & Round indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (target > 0) {
                                stringResource(R.string.label_target_of, target)
                            } else {
                                stringResource(R.string.label_target_unlimited)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.onSurfaceVariant
                        )
                        if (roundsCompleted > 0) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = sanctuaryColors.goldContainer
                            ) {
                                Text(
                                    text = stringResource(R.string.label_round, roundsCompleted),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = sanctuaryColors.onGoldContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Elapsed Session Timer
                    Text(
                        text = formatElapsedTime(elapsedSeconds),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        color = if (isActive) colorScheme.primary else colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when (sessionState) {
                            TallySessionState.READY -> stringResource(R.string.hint_press_start)
                            TallySessionState.ACTIVE -> stringResource(R.string.hint_tap_to_count)
                            TallySessionState.PAUSED -> stringResource(R.string.hint_paused_locked)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }

            // Subtle -1 correction button tucked at bottom-right of the orb
            if (isActive && count > 0) {
                FilledTonalIconButton(
                    onClick = onDecrementOne,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(48.dp)
                        .testTag("decrement_one_button"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = colorScheme.surfaceContainerHigh,
                        contentColor = colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ExposureNeg1,
                        contentDescription = cdDecrement
                    )
                }
            }
        }
    }
}

fun formatElapsedTime(totalSeconds: Long): String {
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
