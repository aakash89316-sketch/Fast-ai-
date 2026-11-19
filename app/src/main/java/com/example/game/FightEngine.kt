package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CrimsonEnemy
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.GoldAccent
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class StepResult(
    val state: GameUiState,
    val triggeredSounds: List<FightSound> = emptyList(),
)

object FightEngine {
    private const val MIN_X = 0.08f
    private const val MAX_X = 0.92f
    private const val GRAVITY = -4.2f
    private const val JUMP_VELOCITY = 1.48f
    private const val PLAYER_WALK_SPEED = 0.36f
    private const val MIN_FIGHTER_SEPARATION = 0.09f

    private var nextEffectId = 1L
    private fun nextId(): Long = nextEffectId++

    fun createRoundState(
        round: Int,
        coins: Int,
        bestRound: Int,
        upgrades: UpgradeLevels,
        isSoundEnabled: Boolean,
        totalMatchesWon: Int,
    ): GameUiState {
        val profile = EnemyRoster.profileForRound(round)
        val maxPlayerHp = upgrades.maxPlayerHealth
        return GameUiState(
            round = round,
            bestRound = maxOf(bestRound, round),
            coins = coins,
            timeRemaining = 48f,
            player = FighterState(
                name = "KING OF LEGION",
                x = 0.24f,
                facingRight = true,
                health = maxPlayerHp,
                maxHealth = maxPlayerHp,
                shadowEnergy = 55f,
            ),
            enemy = FighterState(
                name = profile.name,
                x = 0.76f,
                facingRight = false,
                health = profile.baseHealth,
                maxHealth = profile.baseHealth,
                shadowEnergy = 20f,
            ),
            enemyProfile = profile,
            roundBannerText = "ROUND $round • FIGHT!",
            roundBannerTimer = 1.35f,
            outcome = MatchOutcome.ONGOING,
            isPaused = false,
            isSoundEnabled = isSoundEnabled,
            isShopOpen = false,
            upgrades = upgrades,
            totalMatchesWon = totalMatchesWon,
        )
    }

    fun triggerPlayerAttack(
        state: GameUiState,
        attack: FighterAction,
    ): GameUiState {
        if (state.outcome != MatchOutcome.ONGOING || state.isPaused || state.isShopOpen) return state
        val p = state.player
        if (p.action == FighterAction.HIT_STUN || p.action == FighterAction.KO) return state

        if (attack == FighterAction.CLAW && p.clawCooldown > 0f) return state

        val canInterruptWalkOrIdle = p.action == FighterAction.IDLE ||
            p.action == FighterAction.WALK_FORWARD ||
            p.action == FighterAction.WALK_BACKWARD ||
            p.action == FighterAction.JUMP ||
            p.action == FighterAction.BLOCK

        // Allow combo canceling late in punch/kick into kick/claw!
        val canComboCancel = (p.action == FighterAction.PUNCH && p.actionProgress >= 0.45f) ||
            (p.action == FighterAction.KICK && p.actionProgress >= 0.50f && attack == FighterAction.CLAW)

        if (!canInterruptWalkOrIdle && !canComboCancel) return state

        val duration = when (attack) {
            FighterAction.PUNCH -> 0.24f
            FighterAction.KICK -> 0.34f
            FighterAction.CLAW -> 0.42f
            else -> return state
        }

        val updatedPlayer = p.copy(
            action = attack,
            actionTimer = 0f,
            actionDuration = duration,
            hitRegistered = false,
            facingRight = state.enemy.x >= p.x,
            clawCooldown = if (attack == FighterAction.CLAW) 1.15f else p.clawCooldown,
        )
        return state.copy(player = updatedPlayer)
    }

    fun triggerPlayerJump(state: GameUiState): GameUiState {
        if (state.outcome != MatchOutcome.ONGOING || state.isPaused || state.isShopOpen) return state
        val p = state.player
        if (p.yOffset > 0.01f || p.action == FighterAction.HIT_STUN || p.action == FighterAction.KO) return state
        return state.copy(
            player = p.copy(
                vy = JUMP_VELOCITY,
                action = FighterAction.JUMP,
                actionTimer = 0f,
                actionDuration = 0.65f,
            ),
        )
    }

    fun triggerPlayerDash(state: GameUiState, forward: Boolean = true): GameUiState {
        if (state.outcome != MatchOutcome.ONGOING || state.isPaused || state.isShopOpen) return state
        val p = state.player
        if (p.action == FighterAction.HIT_STUN || p.action == FighterAction.KO || p.action == FighterAction.DASH) {
            return state
        }
        val dir = if (p.facingRight) (if (forward) 1f else -1f) else (if (forward) -1f else 1f)
        val targetX = (p.x + dir * 0.14f).coerceIn(MIN_X, MAX_X)
        val trail = SlashTrail(
            id = nextId(),
            startX = p.x,
            startY = 0.58f - p.yOffset,
            endX = targetX,
            endY = 0.58f - p.yOffset,
            color = CyanGlow.copy(alpha = 0.65f),
            life = 0.20f,
            maxLife = 0.20f,
            strokeWidth = 14f,
        )
        return state.copy(
            player = p.copy(
                x = targetX,
                action = FighterAction.DASH,
                actionTimer = 0f,
                actionDuration = 0.18f,
                hitRegistered = false,
            ),
            slashTrails = (state.slashTrails + trail).takeLast(14),
        )
    }

    fun nudgePlayer(state: GameUiState, deltaX: Float): GameUiState {
        if (state.outcome != MatchOutcome.ONGOING || state.isPaused || state.isShopOpen) return state
        val p = state.player
        if (p.action == FighterAction.HIT_STUN || p.action == FighterAction.KO) return state
        return state.copy(
            player = p.copy(
                x = (p.x + deltaX).coerceIn(MIN_X, MAX_X),
                facingRight = state.enemy.x >= p.x,
            ),
        )
    }

    fun step(
        currentState: GameUiState,
        dtRaw: Float,
        controls: InputControlsState,
        random: Random = Random.Default,
    ): StepResult {
        if (currentState.isPaused || currentState.isShopOpen) {
            return StepResult(currentState)
        }

        val dt = dtRaw.coerceIn(0.001f, 0.045f)
        val sounds = mutableListOf<FightSound>()

        var state = currentState.copy(
            idleTime = currentState.idleTime + dt,
            roundBannerTimer = (currentState.roundBannerTimer - dt).coerceAtLeast(0f),
            screenShake = (currentState.screenShake - dt * 28f).coerceAtLeast(0f),
        )

        // Update visual effects even during hitstop or outcome screen
        state = updateParticles(state, dt)

        if (state.outcome != MatchOutcome.ONGOING) {
            return StepResult(state, sounds)
        }

        if (state.hitStopTimer > 0f) {
            return StepResult(
                state.copy(hitStopTimer = (state.hitStopTimer - dt).coerceAtLeast(0f)),
                sounds,
            )
        }

        // Countdown timer
        val newTime = (state.timeRemaining - dt).coerceAtLeast(0f)
        state = state.copy(timeRemaining = newTime)

        // Advance player & enemy timers and vertical physics
        var p = advanceFighter(state.player, dt)
        var e = advanceFighter(state.enemy, dt)

        // Apply player movement / block inputs when free
        val playerCanMove = p.action == FighterAction.IDLE ||
            p.action == FighterAction.WALK_FORWARD ||
            p.action == FighterAction.WALK_BACKWARD ||
            p.action == FighterAction.BLOCK ||
            p.action == FighterAction.JUMP

        if (playerCanMove) {
            p = p.copy(facingRight = e.x >= p.x)
            when {
                controls.blockHeld && p.yOffset <= 0.01f -> {
                    p = p.copy(action = FighterAction.BLOCK)
                }
                controls.moveLeftHeld && !controls.moveRightHeld -> {
                    val nx = (p.x - PLAYER_WALK_SPEED * dt).coerceIn(MIN_X, MAX_X)
                    val moveAction = if (p.yOffset > 0.01f) {
                        FighterAction.JUMP
                    } else if (p.facingRight) {
                        FighterAction.WALK_BACKWARD
                    } else {
                        FighterAction.WALK_FORWARD
                    }
                    p = p.copy(x = nx, action = moveAction)
                }
                controls.moveRightHeld && !controls.moveLeftHeld -> {
                    val nx = (p.x + PLAYER_WALK_SPEED * dt).coerceIn(MIN_X, MAX_X)
                    val moveAction = if (p.yOffset > 0.01f) {
                        FighterAction.JUMP
                    } else if (p.facingRight) {
                        FighterAction.WALK_FORWARD
                    } else {
                        FighterAction.WALK_BACKWARD
                    }
                    p = p.copy(x = nx, action = moveAction)
                }
                p.yOffset <= 0.01f && p.action != FighterAction.IDLE -> {
                    p = p.copy(action = FighterAction.IDLE)
                }
            }
        }

        // Enemy AI
        val aiResult = stepEnemyAi(state, p, e, dt, random)
        e = aiResult.first
        val nextAiTimer = aiResult.second

        // Keep fighters from overlapping completely
        val separation = abs(p.x - e.x)
        if (separation < MIN_FIGHTER_SEPARATION && p.yOffset < 0.08f && e.yOffset < 0.08f) {
            val push = (MIN_FIGHTER_SEPARATION - separation) * 0.5f
            if (p.x <= e.x) {
                p = p.copy(x = (p.x - push).coerceIn(MIN_X, MAX_X))
                e = e.copy(x = (e.x + push).coerceIn(MIN_X, MAX_X))
            } else {
                p = p.copy(x = (p.x + push).coerceIn(MIN_X, MAX_X))
                e = e.copy(x = (e.x - push).coerceIn(MIN_X, MAX_X))
            }
        }

        state = state.copy(player = p, enemy = e, aiThinkTimer = nextAiTimer)

        // Resolve player active attacks -> enemy
        state = resolvePlayerHit(state, sounds, random)

        // Resolve enemy active attacks -> player
        state = resolveEnemyHit(state, sounds, random)

        // Check KO / Time Up
        state = evaluateOutcome(state, sounds)

        return StepResult(state, sounds)
    }

    private fun advanceFighter(fighter: FighterState, dt: Float): FighterState {
        var f = fighter.copy(
            flashTimer = (fighter.flashTimer - dt).coerceAtLeast(0f),
            clawCooldown = (fighter.clawCooldown - dt).coerceAtLeast(0f),
            comboTimer = (fighter.comboTimer - dt).coerceAtLeast(0f),
        )
        if (f.comboTimer <= 0f && f.comboCount > 0) {
            f = f.copy(comboCount = 0)
        }

        // Vertical jump physics
        if (f.yOffset > 0f || f.vy > 0f) {
            val nextVy = f.vy + GRAVITY * dt
            val nextY = (f.yOffset + nextVy * dt).coerceAtLeast(0f)
            val landed = nextY <= 0f && f.yOffset > 0f
            f = f.copy(
                yOffset = nextY,
                vy = if (nextY <= 0f) 0f else nextVy,
                action = if (landed && f.action == FighterAction.JUMP) FighterAction.IDLE else f.action,
            )
        }

        // Action timer
        val isTimedAction = f.action == FighterAction.PUNCH ||
            f.action == FighterAction.KICK ||
            f.action == FighterAction.CLAW ||
            f.action == FighterAction.DASH ||
            f.action == FighterAction.HIT_STUN

        if (isTimedAction) {
            val nextTimer = f.actionTimer + dt
            f = if (nextTimer >= f.actionDuration) {
                f.copy(
                    action = if (f.yOffset > 0.01f) FighterAction.JUMP else FighterAction.IDLE,
                    actionTimer = 0f,
                    hitRegistered = false,
                )
            } else {
                f.copy(actionTimer = nextTimer)
            }
        }
        return f
    }

    private fun stepEnemyAi(
        state: GameUiState,
        player: FighterState,
        enemy: FighterState,
        dt: Float,
        random: Random,
    ): Pair<FighterState, Float> {
        if (enemy.action == FighterAction.HIT_STUN ||
            enemy.action == FighterAction.KO ||
            enemy.action == FighterAction.PUNCH ||
            enemy.action == FighterAction.KICK ||
            enemy.action == FighterAction.CLAW
        ) {
            return enemy to state.aiThinkTimer
        }

        val profile = state.enemyProfile
        val dist = abs(enemy.x - player.x)
        var e = enemy.copy(facingRight = player.x >= enemy.x)
        var thinkTimer = state.aiThinkTimer - dt

        val attackRange = 0.21f
        if (dist > attackRange) {
            val dir = if (player.x > enemy.x) 1f else -1f
            e = e.copy(
                x = (e.x + dir * profile.moveSpeed * dt).coerceIn(MIN_X, MAX_X),
                action = FighterAction.WALK_FORWARD,
            )
        } else if (e.action == FighterAction.WALK_FORWARD) {
            e = e.copy(action = FighterAction.IDLE)
        }

        if (thinkTimer <= 0f) {
            val nextInterval = (0.58f - profile.aggression * 0.25f).coerceAtLeast(0.24f)
            thinkTimer = nextInterval + random.nextFloat() * 0.22f

            val playerIsAttacking = player.action == FighterAction.PUNCH ||
                player.action == FighterAction.KICK ||
                player.action == FighterAction.CLAW

            if (dist <= 0.25f && playerIsAttacking && random.nextFloat() < 0.28f) {
                e = e.copy(
                    action = FighterAction.BLOCK,
                    actionTimer = 0f,
                    actionDuration = 0.35f,
                )
            } else if (dist <= 0.23f && random.nextFloat() < profile.aggression) {
                val roll = random.nextFloat()
                val chosen = when {
                    roll < 0.55f -> FighterAction.PUNCH
                    roll < 0.88f -> FighterAction.KICK
                    else -> FighterAction.CLAW
                }
                val duration = when (chosen) {
                    FighterAction.PUNCH -> 0.52f // Clear 0.25s wind-up telegraph so player can react!
                    FighterAction.KICK -> 0.62f
                    else -> 0.72f
                }
                e = e.copy(
                    action = chosen,
                    actionTimer = 0f,
                    actionDuration = duration,
                    hitRegistered = false,
                )
            }
        }

        return e to thinkTimer
    }

    private fun resolvePlayerHit(
        state: GameUiState,
        sounds: MutableList<FightSound>,
        random: Random,
    ): GameUiState {
        val p = state.player
        val e = state.enemy
        if (p.hitRegistered || e.action == FighterAction.KO) return state

        val progress = p.actionProgress
        val (inWindow, reach, baseDmg, coinReward, sound) = when (p.action) {
            FighterAction.PUNCH -> AttackSpec(progress in 0.30f..0.75f, 0.22f, 12f * state.upgrades.strikeMultiplier, 10, FightSound.PUNCH)
            FighterAction.KICK -> AttackSpec(progress in 0.32f..0.78f, 0.26f, 18f * state.upgrades.strikeMultiplier, 15, FightSound.KICK)
            FighterAction.CLAW -> AttackSpec(progress in 0.22f..0.82f, 0.38f, 35f * state.upgrades.clawMultiplier, 50, FightSound.CLAW)
            else -> return state
        }

        if (!inWindow) return state

        val dx = e.x - p.x
        val facingTarget = if (p.facingRight) dx >= -0.05f else dx <= 0.05f
        val dist = abs(dx)
        val verticalDiff = abs(p.yOffset - e.yOffset)
        if (!facingTarget || dist > reach || (p.action != FighterAction.CLAW && verticalDiff > 0.26f)) {
            return state
        }

        val isShadowSurge = p.action == FighterAction.CLAW && p.shadowEnergy >= 99f
        val enemyBlocked = e.action == FighterAction.BLOCK && p.action != FighterAction.CLAW
        val rawDmg = if (isShadowSurge) baseDmg * 1.35f else baseDmg
        val finalDmg = if (enemyBlocked) rawDmg * 0.25f else rawDmg
        val damageInt = finalDmg.toInt().coerceAtLeast(1)

        val newCombo = p.comboCount + 1
        val comboBonusCoins = if (newCombo >= 3) newCombo * 3 else 0
        val newShadowEnergy = if (p.action == FighterAction.CLAW) {
            if (isShadowSurge) 15f else (p.shadowEnergy + 12f).coerceAtMost(100f)
        } else {
            (p.shadowEnergy + 16f).coerceAtMost(100f)
        }

        val knockbackDir = if (p.facingRight) 1f else -1f
        val knockbackDist = when {
            enemyBlocked -> 0.02f
            p.action == FighterAction.CLAW -> 0.075f
            p.action == FighterAction.KICK -> 0.045f
            else -> 0.025f
        }

        val hitX = (e.x - knockbackDir * 0.02f).coerceIn(MIN_X, MAX_X)
        val hitY = 0.56f - e.yOffset
        val sparkColor = when {
            enemyBlocked -> Color(0xFFE2E8F0)
            p.action == FighterAction.CLAW -> CyanGlow
            isShadowSurge -> GoldAccent
            else -> CyanGlow
        }

        val newSparks = spawnSparks(
            x = hitX,
            y = hitY,
            color = sparkColor,
            count = if (p.action == FighterAction.CLAW) 16 else 9,
            random = random,
        )

        val newTrails = if (p.action == FighterAction.CLAW) {
            listOf(
                SlashTrail(nextId(), p.x, hitY - 0.06f, hitX + knockbackDir * 0.08f, hitY + 0.05f, CyanGlow, 0.26f, 0.26f, 12f),
                SlashTrail(nextId(), p.x, hitY, hitX + knockbackDir * 0.10f, hitY - 0.02f, Color.White, 0.24f, 0.24f, 8f),
                SlashTrail(nextId(), p.x, hitY + 0.06f, hitX + knockbackDir * 0.08f, hitY - 0.08f, CyanGlow, 0.26f, 0.26f, 12f),
            )
        } else {
            listOf(
                SlashTrail(nextId(), p.x + knockbackDir * 0.04f, hitY, hitX, hitY - 0.03f, sparkColor, 0.14f, 0.14f, 7f),
            )
        }

        val label = when {
            enemyBlocked -> "BLOCKED -$damageInt"
            isShadowSurge -> "SHADOW CLAW -$damageInt!"
            p.action == FighterAction.CLAW -> "CLAW -$damageInt!"
            newCombo >= 3 -> "${newCombo}x -$damageInt"
            else -> "-$damageInt"
        }

        val dmgText = FloatingDamageText(
            id = nextId(),
            x = hitX,
            y = hitY - 0.12f,
            text = label,
            color = when {
                enemyBlocked -> Color(0xFFCBD5E1)
                p.action == FighterAction.CLAW -> GoldAccent
                else -> CyanGlow
            },
            life = 0.72f,
            maxLife = 0.72f,
            isCrit = p.action == FighterAction.CLAW || newCombo >= 3,
        )

        sounds.add(if (enemyBlocked) FightSound.BLOCK else sound)

        val updatedEnemy = e.copy(
            health = (e.health - finalDmg).coerceAtLeast(0f),
            x = (e.x + knockbackDir * knockbackDist).coerceIn(MIN_X, MAX_X),
            flashTimer = 0.16f,
            action = if (enemyBlocked) FighterAction.BLOCK else FighterAction.HIT_STUN,
            actionTimer = 0f,
            actionDuration = if (enemyBlocked) 0.18f else if (p.action == FighterAction.CLAW) 0.30f else 0.18f,
        )

        val updatedPlayer = p.copy(
            hitRegistered = true,
            comboCount = newCombo,
            comboTimer = 1.85f,
            shadowEnergy = newShadowEnergy,
        )

        return state.copy(
            player = updatedPlayer,
            enemy = updatedEnemy,
            coins = state.coins + coinReward + comboBonusCoins,
            matchDamageDealt = state.matchDamageDealt + damageInt,
            matchMaxCombo = maxOf(state.matchMaxCombo, newCombo),
            matchCoinsEarned = state.matchCoinsEarned + coinReward + comboBonusCoins,
            sparks = (state.sparks + newSparks).takeLast(45),
            slashTrails = (state.slashTrails + newTrails).takeLast(14),
            floatingTexts = (state.floatingTexts + dmgText).takeLast(10),
            screenShake = if (p.action == FighterAction.CLAW) 14f else if (p.action == FighterAction.KICK) 6f else 3f,
            hitStopTimer = if (p.action == FighterAction.CLAW) 0.055f else 0.02f,
        )
    }

    private fun resolveEnemyHit(
        state: GameUiState,
        sounds: MutableList<FightSound>,
        random: Random,
    ): GameUiState {
        val e = state.enemy
        val p = state.player
        if (e.hitRegistered || p.action == FighterAction.KO || p.action == FighterAction.DASH) return state

        val progress = e.actionProgress
        val (inWindow, reach, dmgMult) = when (e.action) {
            FighterAction.PUNCH -> Triple(progress in 0.52f..0.82f, 0.21f, 1.0f)
            FighterAction.KICK -> Triple(progress in 0.54f..0.84f, 0.24f, 1.35f)
            FighterAction.CLAW -> Triple(progress in 0.50f..0.85f, 0.29f, 1.75f)
            else -> return state
        }

        if (!inWindow) return state

        val dist = abs(e.x - p.x)
        if (dist > reach || p.yOffset > 0.16f) return state

        val blocked = p.action == FighterAction.BLOCK
        val rawDmg = state.enemyProfile.baseDamage * dmgMult
        val finalDmg = if (blocked) rawDmg * 0.25f else rawDmg
        val damageInt = finalDmg.toInt().coerceAtLeast(1)

        val pushDir = if (e.facingRight) 1f else -1f
        val hitX = p.x
        val hitY = 0.58f - p.yOffset

        sounds.add(if (blocked) FightSound.BLOCK else FightSound.HIT_TAKEN)

        val newSparks = spawnSparks(
            x = hitX,
            y = hitY,
            color = if (blocked) Color.White else CrimsonEnemy,
            count = if (blocked) 7 else 10,
            random = random,
        )

        val dmgText = FloatingDamageText(
            id = nextId(),
            x = hitX,
            y = hitY - 0.12f,
            text = if (blocked) "GUARD -$damageInt" else "-$damageInt",
            color = if (blocked) Color(0xFF94A3B8) else CrimsonEnemy,
            life = 0.65f,
            maxLife = 0.65f,
        )

        val updatedPlayer = p.copy(
            health = (p.health - finalDmg).coerceAtLeast(0f),
            x = (p.x + pushDir * (if (blocked) 0.015f else 0.035f)).coerceIn(MIN_X, MAX_X),
            flashTimer = if (blocked) 0.06f else 0.16f,
            action = if (blocked) FighterAction.BLOCK else FighterAction.HIT_STUN,
            actionTimer = 0f,
            actionDuration = if (blocked) 0.14f else 0.16f,
            shadowEnergy = (p.shadowEnergy + 8f).coerceAtMost(100f),
        )

        return state.copy(
            player = updatedPlayer,
            enemy = e.copy(hitRegistered = true),
            sparks = (state.sparks + newSparks).takeLast(45),
            floatingTexts = (state.floatingTexts + dmgText).takeLast(10),
            screenShake = if (blocked) 3f else 8f,
        )
    }

    private fun evaluateOutcome(
        state: GameUiState,
        sounds: MutableList<FightSound>,
    ): GameUiState {
        val enemyDead = state.enemy.health <= 0f
        val playerDead = state.player.health <= 0f
        val timeUp = state.timeRemaining <= 0f

        if (!enemyDead && !playerDead && !timeUp) return state

        val playerWon = when {
            enemyDead -> true
            playerDead -> false
            else -> state.player.healthFraction >= state.enemy.healthFraction
        }

        sounds.add(FightSound.KO)

        return if (playerWon) {
            val winBonus = 500 + (state.round - 1) * 100
            val nextBest = maxOf(state.bestRound, state.round + 1)
            val koText = FloatingDamageText(
                id = nextId(),
                x = state.enemy.x,
                y = 0.35f,
                text = "K.O.! +$winBonus",
                color = EmeraldWin,
                life = 1.5f,
                maxLife = 1.5f,
                isCrit = true,
            )
            state.copy(
                outcome = MatchOutcome.PLAYER_WIN,
                enemy = state.enemy.copy(action = FighterAction.KO),
                coins = state.coins + winBonus,
                bestRound = nextBest,
                totalMatchesWon = state.totalMatchesWon + 1,
                matchCoinsEarned = state.matchCoinsEarned + winBonus,
                floatingTexts = (state.floatingTexts + koText).takeLast(10),
                screenShake = 18f,
            )
        } else {
            state.copy(
                outcome = MatchOutcome.PLAYER_LOSE,
                player = state.player.copy(action = FighterAction.KO),
                screenShake = 16f,
            )
        }
    }

    private fun updateParticles(state: GameUiState, dt: Float): GameUiState {
        val sparks = state.sparks.mapNotNull { s ->
            val nl = s.life - dt
            if (nl <= 0f) null
            else s.copy(
                x = s.x + s.vx * dt,
                y = s.y + s.vy * dt,
                vy = s.vy + 0.8f * dt,
                life = nl,
            )
        }
        val texts = state.floatingTexts.mapNotNull { t ->
            val nl = t.life - dt
            if (nl <= 0f) null
            else t.copy(y = t.y - 0.11f * dt, life = nl)
        }
        val trails = state.slashTrails.mapNotNull { tr ->
            val nl = tr.life - dt
            if (nl <= 0f) null else tr.copy(life = nl)
        }
        return state.copy(sparks = sparks, floatingTexts = texts, slashTrails = trails)
    }

    private fun spawnSparks(
        x: Float,
        y: Float,
        color: Color,
        count: Int,
        random: Random,
    ): List<HitSpark> {
        return List(count) {
            val angle = random.nextFloat() * 6.28318f
            val speed = 0.15f + random.nextFloat() * 0.42f
            val maxLife = 0.22f + random.nextFloat() * 0.20f
            HitSpark(
                id = nextId(),
                x = x,
                y = y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed - 0.12f,
                life = maxLife,
                maxLife = maxLife,
                color = color,
                radius = 4f + random.nextFloat() * 5f,
            )
        }
    }

    private data class AttackSpec(
        val inWindow: Boolean,
        val reach: Float,
        val baseDamage: Float,
        val coinReward: Int,
        val sound: FightSound,
    )
}
