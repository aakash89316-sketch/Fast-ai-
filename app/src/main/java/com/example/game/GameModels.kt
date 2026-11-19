package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CrimsonEnemy
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.GoldAccent

enum class FighterAction {
    IDLE,
    WALK_FORWARD,
    WALK_BACKWARD,
    JUMP,
    DASH,
    BLOCK,
    PUNCH,
    KICK,
    CLAW,
    HIT_STUN,
    KO,
}

enum class EnemyWeaponStyle {
    STAFF,
    TWIN_CLAWS,
    CLEAVER,
    SHADOW_BLADES,
}

data class EnemyProfile(
    val name: String,
    val title: String,
    val baseHealth: Float,
    val baseDamage: Float,
    val moveSpeed: Float,
    val aggression: Float,
    val accentColor: Color,
    val weaponStyle: EnemyWeaponStyle,
)

data class FighterState(
    val name: String,
    val x: Float,              // Normalized horizontal position [0.08f .. 0.92f]
    val yOffset: Float = 0f,   // Height above ground in arena height units [0f .. 0.45f]
    val vy: Float = 0f,
    val facingRight: Boolean,
    val health: Float,
    val maxHealth: Float,
    val shadowEnergy: Float = 40f,
    val action: FighterAction = FighterAction.IDLE,
    val actionTimer: Float = 0f,
    val actionDuration: Float = 0.01f,
    val hitRegistered: Boolean = false,
    val flashTimer: Float = 0f,
    val comboCount: Int = 0,
    val comboTimer: Float = 0f,
    val clawCooldown: Float = 0f,
) {
    val actionProgress: Float
        get() = if (actionDuration <= 0f) 1f else (actionTimer / actionDuration).coerceIn(0f, 1f)

    val healthFraction: Float
        get() = if (maxHealth <= 0f) 0f else (health / maxHealth).coerceIn(0f, 1f)
}

data class HitSpark(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val life: Float,
    val maxLife: Float,
    val color: Color,
    val radius: Float,
)

data class FloatingDamageText(
    val id: Long,
    val x: Float,
    val y: Float,
    val text: String,
    val color: Color,
    val life: Float,
    val maxLife: Float,
    val isCrit: Boolean = false,
)

data class SlashTrail(
    val id: Long,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val color: Color,
    val life: Float,
    val maxLife: Float,
    val strokeWidth: Float,
)

enum class MatchOutcome {
    ONGOING,
    PLAYER_WIN,
    PLAYER_LOSE,
}

data class UpgradeLevels(
    val strikeLevel: Int = 1,
    val clawLevel: Int = 1,
    val armorLevel: Int = 1,
) {
    val strikeMultiplier: Float get() = 1f + (strikeLevel - 1) * 0.18f
    val clawMultiplier: Float get() = 1f + (clawLevel - 1) * 0.22f
    val maxPlayerHealth: Float get() = 100f + (armorLevel - 1) * 20f

    fun strikeUpgradeCost(): Int = 350 * strikeLevel
    fun clawUpgradeCost(): Int = 450 * clawLevel
    fun armorUpgradeCost(): Int = 400 * armorLevel
}

data class InputControlsState(
    val moveLeftHeld: Boolean = false,
    val moveRightHeld: Boolean = false,
    val blockHeld: Boolean = false,
)

data class GameUiState(
    val round: Int = 1,
    val bestRound: Int = 1,
    val coins: Int = 5000,
    val timeRemaining: Float = 48f,
    val player: FighterState = FighterState(
        name = "KING OF LEGION",
        x = 0.24f,
        facingRight = true,
        health = 100f,
        maxHealth = 100f,
        shadowEnergy = 50f,
    ),
    val enemy: FighterState = FighterState(
        name = "HONG-JOO",
        x = 0.76f,
        facingRight = false,
        health = 100f,
        maxHealth = 100f,
        shadowEnergy = 20f,
    ),
    val enemyProfile: EnemyProfile = EnemyRoster.profileForRound(1),
    val sparks: List<HitSpark> = emptyList(),
    val floatingTexts: List<FloatingDamageText> = emptyList(),
    val slashTrails: List<SlashTrail> = emptyList(),
    val screenShake: Float = 0f,
    val hitStopTimer: Float = 0f,
    val roundBannerText: String = "ROUND 1 • FIGHT!",
    val roundBannerTimer: Float = 1.4f,
    val outcome: MatchOutcome = MatchOutcome.ONGOING,
    val isPaused: Boolean = false,
    val isSoundEnabled: Boolean = true,
    val isShopOpen: Boolean = false,
    val upgrades: UpgradeLevels = UpgradeLevels(),
    val matchDamageDealt: Int = 0,
    val matchMaxCombo: Int = 0,
    val matchCoinsEarned: Int = 0,
    val totalMatchesWon: Int = 0,
    val aiThinkTimer: Float = 0.45f,
    val idleTime: Float = 0f,
)

object EnemyRoster {
    private val profiles = listOf(
        EnemyProfile(
            name = "HONG-JOO",
            title = "TRIUMPH ACROBAT",
            baseHealth = 100f,
            baseDamage = 8f,
            moveSpeed = 0.26f,
            aggression = 0.55f,
            accentColor = CrimsonEnemy,
            weaponStyle = EnemyWeaponStyle.STAFF,
        ),
        EnemyProfile(
            name = "LYNX",
            title = "SHADOW ASSASSIN",
            baseHealth = 120f,
            baseDamage = 10f,
            moveSpeed = 0.34f,
            aggression = 0.68f,
            accentColor = Color(0xFFFF6B35),
            weaponStyle = EnemyWeaponStyle.TWIN_CLAWS,
        ),
        EnemyProfile(
            name = "BUTCHER",
            title = "IRON EXECUTIONER",
            baseHealth = 150f,
            baseDamage = 14f,
            moveSpeed = 0.22f,
            aggression = 0.65f,
            accentColor = GoldAccent,
            weaponStyle = EnemyWeaponStyle.CLEAVER,
        ),
        EnemyProfile(
            name = "SHADOW EMPEROR",
            title = "VOID SOVEREIGN",
            baseHealth = 185f,
            baseDamage = 16f,
            moveSpeed = 0.32f,
            aggression = 0.78f,
            accentColor = CyanGlow,
            weaponStyle = EnemyWeaponStyle.SHADOW_BLADES,
        ),
    )

    fun profileForRound(round: Int): EnemyProfile {
        val index = (round - 1).coerceAtLeast(0) % profiles.size
        val tier = (round - 1) / profiles.size
        val base = profiles[index]
        return if (tier == 0) {
            base
        } else {
            base.copy(
                name = "${base.name} +$tier",
                baseHealth = base.baseHealth + tier * 35f,
                baseDamage = base.baseDamage + tier * 2.5f,
                aggression = (base.aggression + tier * 0.05f).coerceAtMost(0.92f),
            )
        }
    }
}
