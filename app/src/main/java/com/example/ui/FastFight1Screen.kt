package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.ArenaViewModel
import com.example.ui.theme.ArenaNight
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.GoldAccent

@Composable
fun FastFight1Screen(
    viewModel: ArenaViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle back press when shop or pause menu is open
    BackHandler(enabled = state.isShopOpen || state.isPaused) {
        if (state.isShopOpen) {
            viewModel.setShopOpen(false)
        } else if (state.isPaused) {
            viewModel.togglePause()
        }
    }

    // True 60fps+ display-synced game loop
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = (frameNanos - lastNanos) / 1_000_000_000f
                    viewModel.onFrameTick(dt)
                }
                lastNanos = frameNanos
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaNight)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
        ) {
            ArenaTopHeader(
                state = state,
                onToggleSound = viewModel::toggleSound,
                onTogglePause = viewModel::togglePause,
                onOpenShop = { viewModel.setShopOpen(true) },
            )

            FightersHealthBarRow(state = state)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                ArenaCanvas(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                )

                // Round Intro Banner
                RoundIntroBanner(
                    visible = state.roundBannerTimer > 0f,
                    bannerText = state.roundBannerText,
                    matchupText = "${state.player.name} VS ${state.enemy.name}",
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            ArenaControlsDeck(
                state = state,
                onMoveLeftChanged = viewModel::setMoveLeftHeld,
                onMoveRightChanged = viewModel::setMoveRightHeld,
                onBlockChanged = viewModel::setBlockHeld,
                onStepLeft = viewModel::stepLeft,
                onStepRight = viewModel::stepRight,
                onJump = viewModel::jump,
                onDash = viewModel::dash,
                onPunch = viewModel::punch,
                onKick = viewModel::kick,
                onClaw = viewModel::claw,
            )
        }

        MatchOutcomeOverlay(
            state = state,
            onNextFight = viewModel::nextRound,
            onRetryRound = viewModel::retryRound,
            onOpenShop = { viewModel.setShopOpen(true) },
            onResume = viewModel::togglePause,
            modifier = Modifier.fillMaxSize(),
        )

        DojoShopOverlay(
            state = state,
            onBuyStrike = viewModel::buyStrikeUpgrade,
            onBuyClaw = viewModel::buyClawUpgrade,
            onBuyArmor = viewModel::buyArmorUpgrade,
            onClose = { viewModel.setShopOpen(false) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun RoundIntroBanner(
    visible: Boolean,
    bannerText: String,
    matchupText: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            color = ArenaNight.copy(alpha = 0.88f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(2.dp, CyanGlow),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = bannerText,
                    style = MaterialTheme.typography.displaySmall,
                    color = GoldAccent,
                )
                Text(
                    text = matchupText,
                    style = MaterialTheme.typography.labelLarge,
                    color = CyanGlow,
                )
            }
        }
    }
}
