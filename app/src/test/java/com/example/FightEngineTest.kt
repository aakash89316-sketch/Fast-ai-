package com.example

import com.example.game.EnemyRoster
import com.example.game.FightEngine
import com.example.game.FighterAction
import com.example.game.InputControlsState
import com.example.game.MatchOutcome
import com.example.game.UpgradeLevels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FightEngineTest {

    @Test
    fun `claw attack deals damage and awards coins when in range`() {
        var state = FightEngine.createRoundState(
            round = 1,
            coins = 5000,
            bestRound = 1,
            upgrades = UpgradeLevels(),
            isSoundEnabled = false,
            totalMatchesWon = 0,
        )
        // Move player into claw range of enemy
        state = state.copy(
            player = state.player.copy(x = 0.55f),
            enemy = state.enemy.copy(x = 0.75f),
        )
        state = FightEngine.triggerPlayerAttack(state, FighterAction.CLAW)
        // Advance past windup into active hit window
        repeat(12) {
            state = FightEngine.step(state, 0.016f, InputControlsState(), Random(1)).state
        }
        assertTrue("Enemy should take claw damage", state.enemy.health < 100f)
        assertTrue("Player should earn coins on claw hit", state.coins > 5000)
    }

    @Test
    fun `defeating enemy triggers player win and round bonus`() {
        var state = FightEngine.createRoundState(
            round = 1,
            coins = 5000,
            bestRound = 1,
            upgrades = UpgradeLevels(),
            isSoundEnabled = false,
            totalMatchesWon = 0,
        ).copy(
            player = FightEngine.createRoundState(1, 5000, 1, UpgradeLevels(), false, 0).player.copy(x = 0.60f),
            enemy = FightEngine.createRoundState(1, 5000, 1, UpgradeLevels(), false, 0).enemy.copy(x = 0.74f, health = 10f),
        )
        state = FightEngine.triggerPlayerAttack(state, FighterAction.CLAW)
        repeat(15) {
            state = FightEngine.step(state, 0.016f, InputControlsState(), Random(2)).state
        }
        assertEquals(MatchOutcome.PLAYER_WIN, state.outcome)
        assertEquals(FighterAction.KO, state.enemy.action)
        assertTrue("Coins should include +500 victory reward", state.coins >= 5500)
    }

    @Test
    fun `enemy roster scales across rounds`() {
        val r1 = EnemyRoster.profileForRound(1)
        val r2 = EnemyRoster.profileForRound(2)
        val r5 = EnemyRoster.profileForRound(5)
        assertEquals("HONG-JOO", r1.name)
        assertEquals("LYNX", r2.name)
        assertTrue(r5.baseHealth > r1.baseHealth)
    }
}
