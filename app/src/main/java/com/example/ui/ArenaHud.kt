package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.GameUiState
import com.example.game.MatchOutcome
import com.example.ui.theme.ArenaHeaderBg
import com.example.ui.theme.ArenaNight
import com.example.ui.theme.CrimsonEnemy
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.ShadowCardBg
import com.example.ui.theme.ShadowCardBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.ceil

@Composable
fun ArenaTopHeader(
    state: GameUiState,
    onToggleSound: () -> Unit,
    onTogglePause: () -> Unit,
    onOpenShop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = ArenaHeaderBg,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = CyanGlow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "💰 ${state.coins}",
                        style = MaterialTheme.typography.labelLarge,
                        color = GoldAccent,
                        modifier = Modifier.testTag("coins_text"),
                    )
                    Surface(
                        color = ShadowCardBg,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, ShadowCardBorder),
                    ) {
                        Text(
                            text = "ROUND ${state.round}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("round_badge"),
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(
                    onClick = onOpenShop,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("shop_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingBag,
                        contentDescription = stringResource(R.string.shop_toggle_desc),
                        tint = GoldAccent,
                    )
                }
                IconButton(
                    onClick = onToggleSound,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("sound_toggle_button"),
                ) {
                    Icon(
                        imageVector = if (state.isSoundEnabled) {
                            Icons.AutoMirrored.Filled.VolumeUp
                        } else {
                            Icons.AutoMirrored.Filled.VolumeOff
                        },
                        contentDescription = stringResource(R.string.sound_toggle_desc),
                        tint = if (state.isSoundEnabled) CyanGlow else TextSecondary,
                    )
                }
                IconButton(
                    onClick = onTogglePause,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("pause_button"),
                ) {
                    Icon(
                        imageVector = if (state.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = stringResource(R.string.pause_toggle_desc),
                        tint = TextPrimary,
                    )
                }
            }
        }
    }
}

@Composable
fun FightersHealthBarRow(
    state: GameUiState,
    modifier: Modifier = Modifier,
) {
    val playerHpAnim by animateFloatAsState(
        targetValue = state.player.healthFraction,
        animationSpec = spring(stiffness = 380f),
        label = "playerHp",
    )
    val enemyHpAnim by animateFloatAsState(
        targetValue = state.enemy.healthFraction,
        animationSpec = spring(stiffness = 380f),
        label = "enemyHp",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ArenaNight.copy(alpha = 0.85f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Left: Player Health + Shadow Energy
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = state.player.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${state.player.health.toInt()}/${state.player.maxHealth.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanGlow,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, CyanGlow.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(playerHpAnim)
                            .background(
                                Brush.horizontalGradient(listOf(CyanDeep, CyanGlow)),
                            ),
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                // Shadow Energy bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF172033)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((state.player.shadowEnergy / 100f).coerceIn(0f, 1f))
                            .background(if (state.player.shadowEnergy >= 99f) GoldAccent else CyanGlow.copy(alpha = 0.85f)),
                    )
                }
            }

            // Center: Match Timer Badge
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 4.dp,
                border = BorderStroke(2.dp, GoldAccent),
                modifier = Modifier.testTag("match_timer_badge"),
            ) {
                Text(
                    text = ceil(state.timeRemaining).toInt().toString().padStart(2, '0'),
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ArenaNight,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }

            // Right: Enemy Health
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${state.enemy.health.toInt()}/${state.enemy.maxHealth.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = state.enemyProfile.accentColor,
                    )
                    Text(
                        text = state.enemy.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, state.enemyProfile.accentColor.copy(alpha = 0.55f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(enemyHpAnim)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(state.enemyProfile.accentColor, Color(0xFF8B0016)),
                                ),
                            ),
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = state.enemyProfile.title,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = TextSecondary,
                    maxLines = 1,
                )
            }
        }

        // Live Combo Callout Row
        AnimatedVisibility(
            visible = state.player.comboCount >= 2,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = "⚡ ${state.player.comboCount} HIT COMBO!",
                style = MaterialTheme.typography.labelLarge,
                color = GoldAccent,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("combo_counter"),
            )
        }
    }
}

@Composable
fun ArenaControlsDeck(
    state: GameUiState,
    onMoveLeftChanged: (Boolean) -> Unit,
    onMoveRightChanged: (Boolean) -> Unit,
    onBlockChanged: (Boolean) -> Unit,
    onStepLeft: () -> Unit,
    onStepRight: () -> Unit,
    onJump: () -> Unit,
    onDash: () -> Unit,
    onPunch: () -> Unit,
    onKick: () -> Unit,
    onClaw: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clawReady = state.player.clawCooldown <= 0f
    val shadowSurgeReady = state.player.shadowEnergy >= 99f

    Surface(
        color = ArenaHeaderBg.copy(alpha = 0.96f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: Movement & Defense Controls
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HoldControlButton(
                        onHoldChanged = onMoveLeftChanged,
                        onTapFallback = onStepLeft,
                        containerColor = ShadowCardBg,
                        borderColor = CyanGlow.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(width = 64.dp, height = 54.dp)
                            .testTag("move_left_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Move Left",
                            tint = TextPrimary,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    HoldControlButton(
                        onHoldChanged = onMoveRightChanged,
                        onTapFallback = onStepRight,
                        containerColor = ShadowCardBg,
                        borderColor = CyanGlow.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(width = 64.dp, height = 54.dp)
                            .testTag("move_right_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Move Right",
                            tint = TextPrimary,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onJump,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShadowCardBg,
                            contentColor = TextPrimary,
                        ),
                        border = BorderStroke(1.dp, ShadowCardBorder),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("jump_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = stringResource(R.string.action_jump),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    HoldControlButton(
                        onHoldChanged = onBlockChanged,
                        onTapFallback = {},
                        containerColor = ShadowCardBg,
                        borderColor = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier
                            .height(50.dp)
                            .width(84.dp)
                            .testTag("block_button"),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = stringResource(R.string.action_block),
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(R.string.action_block),
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                            )
                        }
                    }
                }
            }

            // Right: Shadow Combat Strikes
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onPunch,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanGlow,
                            contentColor = ArenaNight,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("punch_button"),
                    ) {
                        Text(
                            text = "👊 ${stringResource(R.string.action_punch)}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    Button(
                        onClick = onKick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB),
                            contentColor = TextPrimary,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("kick_button"),
                    ) {
                        Text(
                            text = "🦵 ${stringResource(R.string.action_kick)}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onDash,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShadowCardBg,
                            contentColor = CyanGlow,
                        ),
                        border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("dash_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.action_dash),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    Button(
                        onClick = onClaw,
                        enabled = clawReady,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (shadowSurgeReady) GoldAccent else Color(0xFFFACC15),
                            contentColor = ArenaNight,
                            disabledContainerColor = Color(0xFF334155),
                            disabledContentColor = TextSecondary,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("claw_button"),
                    ) {
                        Text(
                            text = if (clawReady) {
                                if (shadowSurgeReady) "🔥 SURGE CLAW" else "🔥 ${stringResource(R.string.action_claw)}"
                            } else {
                                String.format("%.1fs", state.player.clawCooldown)
                            },
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldControlButton(
    onHoldChanged: (Boolean) -> Unit,
    onTapFallback: () -> Unit,
    containerColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }

    Surface(
        color = if (pressed) CyanDeep.copy(alpha = 0.45f) else containerColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, if (pressed) CyanGlow else borderColor),
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    pressed = true
                    onHoldChanged(true)
                    val downTime = System.currentTimeMillis()
                    waitForUpOrCancellation()
                    pressed = false
                    onHoldChanged(false)
                    if (System.currentTimeMillis() - downTime < 140L) {
                        onTapFallback()
                    }
                }
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
fun MatchOutcomeOverlay(
    state: GameUiState,
    onNextFight: () -> Unit,
    onRetryRound: () -> Unit,
    onOpenShop: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.outcome == MatchOutcome.ONGOING && !state.isPaused) return

    Box(
        modifier = modifier
            .background(ArenaNight.copy(alpha = 0.78f))
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ShadowCardBg),
            border = BorderStroke(
                width = 2.dp,
                color = when (state.outcome) {
                    MatchOutcome.PLAYER_WIN -> EmeraldWin
                    MatchOutcome.PLAYER_LOSE -> CrimsonEnemy
                    MatchOutcome.ONGOING -> CyanGlow
                },
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (state.outcome) {
                    MatchOutcome.PLAYER_WIN -> {
                        Text(
                            text = "🏆 ${stringResource(R.string.victory_banner)} 🏆",
                            style = MaterialTheme.typography.titleLarge,
                            color = EmeraldWin,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("victory_title"),
                        )
                        Text(
                            text = "${state.enemy.name} DEFEATED • ROUND ${state.round} CLEAR",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary,
                        )
                        MatchStatsRow(state)
                        Button(
                            onClick = onNextFight,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldWin,
                                contentColor = ArenaNight,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("next_fight_button"),
                        ) {
                            Text(
                                text = "${stringResource(R.string.next_fight)} - ROUND ${state.round + 1}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenShop,
                            border = BorderStroke(1.dp, GoldAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("victory_shop_button"),
                        ) {
                            Text(
                                text = stringResource(R.string.dojo_upgrades),
                                color = GoldAccent,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }

                    MatchOutcome.PLAYER_LOSE -> {
                        Text(
                            text = "💀 ${stringResource(R.string.defeat_banner)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = CrimsonEnemy,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("defeat_title"),
                        )
                        Text(
                            text = "TIP: USE BLOCK OR JUMP WHEN ENEMY EYES GLOW RED",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                        MatchStatsRow(state)
                        Button(
                            onClick = onRetryRound,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanGlow,
                                contentColor = ArenaNight,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("retry_round_button"),
                        ) {
                            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.retry_fight),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenShop,
                            border = BorderStroke(1.dp, GoldAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.dojo_upgrades),
                                color = GoldAccent,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }

                    MatchOutcome.ONGOING -> {
                        Text(
                            text = "MATCH PAUSED",
                            style = MaterialTheme.typography.titleLarge,
                            color = CyanGlow,
                        )
                        Button(
                            onClick = onResume,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanGlow,
                                contentColor = ArenaNight,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("resume_button"),
                        ) {
                            Text("RESUME FIGHT", style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedButton(
                            onClick = onRetryRound,
                            border = BorderStroke(1.dp, ShadowCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("restart_round_button"),
                        ) {
                            Text("RESTART ROUND", color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchStatsRow(state: GameUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ArenaNight.copy(alpha = 0.7f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatItem("DAMAGE", "${state.matchDamageDealt}", CyanGlow)
        StatItem("MAX COMBO", "${state.matchMaxCombo}x", GoldAccent)
        StatItem("EARNED", "+${state.matchCoinsEarned}", EmeraldWin)
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(
            text = value,
            fontFamily = OrbitronFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = color,
        )
    }
}

@Composable
fun DojoShopOverlay(
    state: GameUiState,
    onBuyStrike: () -> Unit,
    onBuyClaw: () -> Unit,
    onBuyArmor: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isShopOpen) return

    Box(
        modifier = modifier
            .background(ArenaNight.copy(alpha = 0.86f))
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ShadowCardBg),
            border = BorderStroke(2.dp, GoldAccent),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.dojo_upgrades),
                        style = MaterialTheme.typography.titleLarge,
                        color = GoldAccent,
                    )
                    Text(
                        text = "💰 ${state.coins}",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldAccent,
                    )
                }

                UpgradeRow(
                    title = "STRIKE POWER (LV ${state.upgrades.strikeLevel})",
                    subtitle = "+18% Punch & Kick Damage",
                    cost = state.upgrades.strikeUpgradeCost(),
                    canAfford = state.coins >= state.upgrades.strikeUpgradeCost(),
                    onBuy = onBuyStrike,
                    testTag = "upgrade_strike_button",
                )

                UpgradeRow(
                    title = "CLAW FURY (LV ${state.upgrades.clawLevel})",
                    subtitle = "+22% Shadow Claw Slash Damage",
                    cost = state.upgrades.clawUpgradeCost(),
                    canAfford = state.coins >= state.upgrades.clawUpgradeCost(),
                    onBuy = onBuyClaw,
                    testTag = "upgrade_claw_button",
                )

                UpgradeRow(
                    title = "LEGION ARMOR (LV ${state.upgrades.armorLevel})",
                    subtitle = "+20 Max Health (${state.upgrades.maxPlayerHealth.toInt()} HP)",
                    cost = state.upgrades.armorUpgradeCost(),
                    canAfford = state.coins >= state.upgrades.armorUpgradeCost(),
                    onBuy = onBuyArmor,
                    testTag = "upgrade_armor_button",
                )

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanGlow,
                        contentColor = ArenaNight,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("close_shop_button"),
                ) {
                    Text("BACK TO ARENA", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun UpgradeRow(
    title: String,
    subtitle: String,
    cost: Int,
    canAfford: Boolean,
    onBuy: () -> Unit,
    testTag: String,
) {
    Surface(
        color = ArenaNight.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ShadowCardBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Button(
                onClick = onBuy,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    contentColor = ArenaNight,
                    disabledContainerColor = Color(0xFF334155),
                    disabledContentColor = TextSecondary,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag(testTag),
            ) {
                Text(text = "💰 $cost", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
