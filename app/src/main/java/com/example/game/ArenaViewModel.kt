package com.example.game

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ArenaViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("fast_fight_1_prefs", Context.MODE_PRIVATE)
    private val soundManager = SoundEffectsManager()

    private val _controls = MutableStateFlow(InputControlsState())
    private val _uiState: MutableStateFlow<GameUiState>

    init {
        val savedCoins = prefs.getInt(KEY_COINS, 5000)
        val savedBestRound = prefs.getInt(KEY_BEST_ROUND, 1)
        val savedWins = prefs.getInt(KEY_WINS, 0)
        val savedSound = prefs.getBoolean(KEY_SOUND, true)
        val upgrades = UpgradeLevels(
            strikeLevel = prefs.getInt(KEY_UPGRADE_STRIKE, 1),
            clawLevel = prefs.getInt(KEY_UPGRADE_CLAW, 1),
            armorLevel = prefs.getInt(KEY_UPGRADE_ARMOR, 1),
        )
        _uiState = MutableStateFlow(
            FightEngine.createRoundState(
                round = 1,
                coins = savedCoins,
                bestRound = savedBestRound,
                upgrades = upgrades,
                isSoundEnabled = savedSound,
                totalMatchesWon = savedWins,
            ),
        )
    }

    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
    val controls: StateFlow<InputControlsState> = _controls.asStateFlow()

    fun onFrameTick(dtSeconds: Float) {
        val current = _uiState.value
        val result = FightEngine.step(current, dtSeconds, _controls.value)
        if (result.state != current) {
            _uiState.value = result.state
            if (result.state.coins != current.coins || result.state.outcome != current.outcome) {
                persistProgress(result.state)
            }
        }
        val soundEnabled = result.state.isSoundEnabled
        result.triggeredSounds.forEach { sound ->
            soundManager.play(sound, soundEnabled)
        }
    }

    fun setMoveLeftHeld(held: Boolean) {
        _controls.update { it.copy(moveLeftHeld = held) }
    }

    fun setMoveRightHeld(held: Boolean) {
        _controls.update { it.copy(moveRightHeld = held) }
    }

    fun setBlockHeld(held: Boolean) {
        _controls.update { it.copy(blockHeld = held) }
    }

    fun stepLeft() {
        _uiState.update { FightEngine.nudgePlayer(it, -0.045f) }
    }

    fun stepRight() {
        _uiState.update { FightEngine.nudgePlayer(it, 0.045f) }
    }

    fun jump() {
        _uiState.update { FightEngine.triggerPlayerJump(it) }
    }

    fun dash() {
        _uiState.update { FightEngine.triggerPlayerDash(it, forward = true) }
    }

    fun punch() {
        _uiState.update { FightEngine.triggerPlayerAttack(it, FighterAction.PUNCH) }
    }

    fun kick() {
        _uiState.update { FightEngine.triggerPlayerAttack(it, FighterAction.KICK) }
    }

    fun claw() {
        _uiState.update { FightEngine.triggerPlayerAttack(it, FighterAction.CLAW) }
    }

    fun nextRound() {
        val cur = _uiState.value
        val next = FightEngine.createRoundState(
            round = cur.round + 1,
            coins = cur.coins,
            bestRound = cur.bestRound,
            upgrades = cur.upgrades,
            isSoundEnabled = cur.isSoundEnabled,
            totalMatchesWon = cur.totalMatchesWon,
        )
        _uiState.value = next
        persistProgress(next)
    }

    fun retryRound() {
        val cur = _uiState.value
        _uiState.value = FightEngine.createRoundState(
            round = cur.round,
            coins = cur.coins,
            bestRound = cur.bestRound,
            upgrades = cur.upgrades,
            isSoundEnabled = cur.isSoundEnabled,
            totalMatchesWon = cur.totalMatchesWon,
        )
    }

    fun togglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    fun toggleSound() {
        _uiState.update { state ->
            val next = state.copy(isSoundEnabled = !state.isSoundEnabled)
            prefs.edit().putBoolean(KEY_SOUND, next.isSoundEnabled).apply()
            next
        }
    }

    fun setShopOpen(open: Boolean) {
        _uiState.update { it.copy(isShopOpen = open) }
    }

    fun buyStrikeUpgrade() {
        _uiState.update { state ->
            val cost = state.upgrades.strikeUpgradeCost()
            if (state.coins < cost) return@update state
            val updatedUpgrades = state.upgrades.copy(strikeLevel = state.upgrades.strikeLevel + 1)
            val next = state.copy(
                coins = state.coins - cost,
                upgrades = updatedUpgrades,
            )
            soundManager.play(FightSound.UPGRADE, state.isSoundEnabled)
            persistProgress(next)
            next
        }
    }

    fun buyClawUpgrade() {
        _uiState.update { state ->
            val cost = state.upgrades.clawUpgradeCost()
            if (state.coins < cost) return@update state
            val updatedUpgrades = state.upgrades.copy(clawLevel = state.upgrades.clawLevel + 1)
            val next = state.copy(
                coins = state.coins - cost,
                upgrades = updatedUpgrades,
            )
            soundManager.play(FightSound.UPGRADE, state.isSoundEnabled)
            persistProgress(next)
            next
        }
    }

    fun buyArmorUpgrade() {
        _uiState.update { state ->
            val cost = state.upgrades.armorUpgradeCost()
            if (state.coins < cost) return@update state
            val updatedUpgrades = state.upgrades.copy(armorLevel = state.upgrades.armorLevel + 1)
            val newMaxHp = updatedUpgrades.maxPlayerHealth
            val hpRatio = state.player.healthFraction
            val next = state.copy(
                coins = state.coins - cost,
                upgrades = updatedUpgrades,
                player = state.player.copy(
                    maxHealth = newMaxHp,
                    health = (newMaxHp * hpRatio).coerceAtLeast(1f),
                ),
            )
            soundManager.play(FightSound.UPGRADE, state.isSoundEnabled)
            persistProgress(next)
            next
        }
    }

    private fun persistProgress(state: GameUiState) {
        prefs.edit()
            .putInt(KEY_COINS, state.coins)
            .putInt(KEY_BEST_ROUND, state.bestRound)
            .putInt(KEY_WINS, state.totalMatchesWon)
            .putInt(KEY_UPGRADE_STRIKE, state.upgrades.strikeLevel)
            .putInt(KEY_UPGRADE_CLAW, state.upgrades.clawLevel)
            .putInt(KEY_UPGRADE_ARMOR, state.upgrades.armorLevel)
            .apply()
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }

    private companion object {
        const val KEY_COINS = "coins"
        const val KEY_BEST_ROUND = "best_round"
        const val KEY_WINS = "wins"
        const val KEY_SOUND = "sound_enabled"
        const val KEY_UPGRADE_STRIKE = "upgrade_strike"
        const val KEY_UPGRADE_CLAW = "upgrade_claw"
        const val KEY_UPGRADE_ARMOR = "upgrade_armor"
    }
}
