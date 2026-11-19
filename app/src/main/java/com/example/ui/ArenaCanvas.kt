package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.EnemyWeaponStyle
import com.example.game.FighterAction
import com.example.game.FighterState
import com.example.game.GameUiState
import com.example.ui.theme.ArenaCanvasBg
import com.example.ui.theme.ArenaFloorSlate
import com.example.ui.theme.ArenaNight
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.CrimsonEnemy
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.GoldAccent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArenaCanvas(
    state: GameUiState,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .background(ArenaCanvasBg),
    ) {
        // Moonlit Dojo Backdrop
        Image(
            painter = painterResource(id = R.drawable.img_arena_dojo),
            contentDescription = stringResource(R.string.arena_background_desc),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // Atmospheric Shadow-Fight Rim Vignette + Dynamic Combat Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val shakeX = if (state.screenShake > 0.2f) {
                sin(state.idleTime * 90f) * state.screenShake
            } else {
                0f
            }
            val shakeY = if (state.screenShake > 0.2f) {
                cos(state.idleTime * 75f) * state.screenShake * 0.6f
            } else {
                0f
            }

            translate(left = shakeX, top = shakeY) {
                val w = size.width
                val h = size.height
                val groundY = h * 0.80f

                // Atmospheric tint over background for crisp silhouette contrast
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            ArenaNight.copy(alpha = 0.28f),
                            Color.Transparent,
                            ArenaNight.copy(alpha = 0.52f),
                        ),
                    ),
                )

                // Stone dojo floor reflection band
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            ArenaFloorSlate.copy(alpha = 0.78f),
                            ArenaNight.copy(alpha = 0.94f),
                        ),
                        startY = groundY,
                        endY = h,
                    ),
                    topLeft = Offset(0f, groundY),
                    size = Size(w, h - groundY),
                )

                // Glowing horizon rim along the dojo floor edge
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            CyanGlow.copy(alpha = 0.45f),
                            state.enemyProfile.accentColor.copy(alpha = 0.45f),
                            Color.Transparent,
                        ),
                    ),
                    start = Offset(0f, groundY),
                    end = Offset(w, groundY),
                    strokeWidth = 3.5f,
                )

                // Subtle ground shadows under both fighters
                drawFighterShadow(state.player, w, groundY, CyanGlow)
                drawFighterShadow(state.enemy, w, groundY, state.enemyProfile.accentColor)

                // Draw Enemy first, then Player on top during close clashes
                drawShadowFighter(
                    fighter = state.enemy,
                    arenaWidth = w,
                    arenaHeight = h,
                    groundY = groundY,
                    bodyColor = Color(0xFF140509),
                    rimColor = state.enemyProfile.accentColor,
                    idleTime = state.idleTime,
                    isPlayer = false,
                    weaponStyle = state.enemyProfile.weaponStyle,
                )

                drawShadowFighter(
                    fighter = state.player,
                    arenaWidth = w,
                    arenaHeight = h,
                    groundY = groundY,
                    bodyColor = Color(0xFF04070F),
                    rimColor = CyanGlow,
                    idleTime = state.idleTime,
                    isPlayer = true,
                    weaponStyle = EnemyWeaponStyle.TWIN_CLAWS,
                )

                // Slash trails
                state.slashTrails.forEach { trail ->
                    val alpha = (trail.life / trail.maxLife).coerceIn(0f, 1f)
                    drawLine(
                        color = trail.color.copy(alpha = alpha),
                        start = Offset(trail.startX * w, trail.startY * h),
                        end = Offset(trail.endX * w, trail.endY * h),
                        strokeWidth = trail.strokeWidth * alpha,
                        cap = StrokeCap.Round,
                    )
                }

                // Hit sparks
                state.sparks.forEach { spark ->
                    val alpha = (spark.life / spark.maxLife).coerceIn(0f, 1f)
                    val sx = spark.x * w
                    val sy = spark.y * h
                    drawCircle(
                        color = spark.color.copy(alpha = alpha),
                        radius = spark.radius * (0.5f + 0.5f * alpha),
                        center = Offset(sx, sy),
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = alpha * 0.8f),
                        radius = spark.radius * 0.35f,
                        center = Offset(sx, sy),
                    )
                }

                // Floating damage numbers
                state.floatingTexts.forEach { item ->
                    val alpha = (item.life / item.maxLife).coerceIn(0f, 1f)
                    drawFloatingText(
                        textMeasurer = textMeasurer,
                        text = item.text,
                        center = Offset(item.x * w, item.y * h),
                        color = item.color.copy(alpha = alpha),
                        isCrit = item.isCrit,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawFighterShadow(
    fighter: FighterState,
    arenaWidth: Float,
    groundY: Float,
    glowColor: Color,
) {
    val cx = fighter.x * arenaWidth
    val scale = (1f - fighter.yOffset * 1.4f).coerceIn(0.45f, 1f)
    val shadowW = arenaWidth * 0.12f * scale
    val shadowH = 14f * scale
    drawOval(
        color = Color.Black.copy(alpha = 0.65f * scale),
        topLeft = Offset(cx - shadowW * 0.5f, groundY - shadowH * 0.3f),
        size = Size(shadowW, shadowH),
    )
    drawOval(
        color = glowColor.copy(alpha = 0.22f * scale),
        topLeft = Offset(cx - shadowW * 0.38f, groundY - shadowH * 0.15f),
        size = Size(shadowW * 0.76f, shadowH * 0.55f),
    )
}

private fun DrawScope.drawShadowFighter(
    fighter: FighterState,
    arenaWidth: Float,
    arenaHeight: Float,
    groundY: Float,
    bodyColor: Color,
    rimColor: Color,
    idleTime: Float,
    isPlayer: Boolean,
    weaponStyle: EnemyWeaponStyle,
) {
    val dir = if (fighter.facingRight) 1f else -1f
    val unit = (arenaHeight * 0.42f).coerceAtLeast(100f)
    val baseX = fighter.x * arenaWidth
    val baseY = groundY - fighter.yOffset * arenaHeight

    val bob = if (fighter.action == FighterAction.IDLE) sin(idleTime * 5f + if (isPlayer) 0f else 1.5f) * unit * 0.015f else 0f
    val walkPhase = sin(idleTime * 15f)
    val p = fighter.actionProgress

    val isHit = fighter.flashTimer > 0f
    val silhouette = if (isHit) Color(0xFF28344D) else bodyColor
    val activeGlow = if (isHit) Color.White else rimColor

    // Compute KO collapse rotation/drop
    val koProgress = if (fighter.action == FighterAction.KO) 1f else 0f
    val hipY = baseY - unit * (0.44f - koProgress * 0.34f) + bob
    val hipX = baseX - dir * koProgress * unit * 0.18f

    val torsoLean = when (fighter.action) {
        FighterAction.WALK_FORWARD -> dir * 0.12f
        FighterAction.WALK_BACKWARD -> -dir * 0.08f
        FighterAction.PUNCH -> dir * sin(p * PI.toFloat()) * 0.28f
        FighterAction.KICK -> -dir * sin(p * PI.toFloat()) * 0.22f
        FighterAction.CLAW -> dir * sin(p * PI.toFloat()) * 0.38f
        FighterAction.DASH -> dir * 0.32f
        FighterAction.HIT_STUN -> -dir * 0.25f
        FighterAction.KO -> -dir * 0.85f
        else -> dir * 0.06f
    }

    val shoulder = Offset(
        x = hipX + sin(torsoLean) * unit * 0.34f,
        y = hipY - cos(torsoLean) * unit * 0.34f,
    )
    val headCenter = Offset(
        x = shoulder.x + sin(torsoLean) * unit * 0.15f,
        y = shoulder.y - cos(torsoLean) * unit * 0.15f,
    )

    // Flowing Shadow Sash / Scarf
    val sashWave = sin(idleTime * 7f + (if (isPlayer) 0f else 2f)) * unit * 0.06f
    val sashPath = Path().apply {
        moveTo(shoulder.x - dir * unit * 0.03f, shoulder.y + unit * 0.03f)
        quadraticTo(
            shoulder.x - dir * unit * 0.18f,
            shoulder.y + unit * 0.12f + sashWave,
            shoulder.x - dir * unit * 0.28f,
            shoulder.y + unit * 0.28f - sashWave,
        )
    }
    drawPath(
        path = sashPath,
        color = activeGlow.copy(alpha = 0.38f),
        style = Stroke(width = unit * 0.065f, cap = StrokeCap.Round),
    )
    drawPath(
        path = sashPath,
        color = silhouette,
        style = Stroke(width = unit * 0.042f, cap = StrokeCap.Round),
    )

    // Legs kinematics
    val frontFoot: Offset
    val backFoot: Offset
    val frontKnee: Offset
    val backKnee: Offset

    when (fighter.action) {
        FighterAction.KICK -> {
            val kickExt = sin(p * PI.toFloat())
            backFoot = Offset(hipX - dir * unit * 0.12f, baseY)
            backKnee = Offset(hipX - dir * unit * 0.07f, hipY + unit * 0.20f)
            frontKnee = Offset(hipX + dir * unit * 0.18f * kickExt, hipY - unit * 0.06f * kickExt)
            frontFoot = Offset(
                hipX + dir * unit * (0.14f + 0.36f * kickExt),
                hipY + unit * (0.38f - 0.52f * kickExt),
            )
        }
        FighterAction.WALK_FORWARD, FighterAction.WALK_BACKWARD -> {
            val stride = walkPhase * unit * 0.14f
            frontFoot = Offset(hipX + stride, baseY - maxOf(0f, walkPhase) * unit * 0.05f)
            backFoot = Offset(hipX - stride, baseY - maxOf(0f, -walkPhase) * unit * 0.05f)
            frontKnee = Offset((hipX + frontFoot.x) * 0.5f + dir * unit * 0.05f, (hipY + frontFoot.y) * 0.5f)
            backKnee = Offset((hipX + backFoot.x) * 0.5f + dir * unit * 0.04f, (hipY + backFoot.y) * 0.5f)
        }
        FighterAction.JUMP -> {
            frontFoot = Offset(hipX + dir * unit * 0.10f, hipY + unit * 0.28f)
            backFoot = Offset(hipX - dir * unit * 0.12f, hipY + unit * 0.32f)
            frontKnee = Offset(hipX + dir * unit * 0.16f, hipY + unit * 0.12f)
            backKnee = Offset(hipX - dir * unit * 0.05f, hipY + unit * 0.16f)
        }
        FighterAction.KO -> {
            frontFoot = Offset(hipX + dir * unit * 0.32f, baseY)
            backFoot = Offset(hipX + dir * unit * 0.18f, baseY)
            frontKnee = Offset(hipX + dir * unit * 0.16f, hipY + unit * 0.05f)
            backKnee = Offset(hipX + dir * unit * 0.08f, hipY + unit * 0.06f)
        }
        else -> {
            frontFoot = Offset(hipX + dir * unit * 0.14f, baseY)
            backFoot = Offset(hipX - dir * unit * 0.13f, baseY)
            frontKnee = Offset(hipX + dir * unit * 0.12f, hipY + unit * 0.22f)
            backKnee = Offset(hipX - dir * unit * 0.07f, hipY + unit * 0.22f)
        }
    }

    // Back leg
    drawLimb(Offset(hipX, hipY), backKnee, backFoot, unit * 0.075f, silhouette, activeGlow)
    // Front leg
    drawLimb(Offset(hipX, hipY), frontKnee, frontFoot, unit * 0.08f, silhouette, activeGlow)

    // Kick trail arc
    if (fighter.action == FighterAction.KICK && p in 0.2f..0.8f) {
        drawCircle(
            color = activeGlow.copy(alpha = 0.35f),
            radius = unit * 0.09f,
            center = frontFoot,
        )
    }

    // Torso
    drawLine(
        color = activeGlow.copy(alpha = 0.5f),
        start = Offset(hipX, hipY),
        end = shoulder,
        strokeWidth = unit * 0.155f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = silhouette,
        start = Offset(hipX, hipY),
        end = shoulder,
        strokeWidth = unit * 0.135f,
        cap = StrokeCap.Round,
    )

    // Arms kinematics
    val frontHand: Offset
    val frontElbow: Offset
    val backHand: Offset
    val backElbow: Offset

    when (fighter.action) {
        FighterAction.PUNCH -> {
            val ext = sin(p * PI.toFloat())
            frontElbow = Offset(shoulder.x + dir * unit * 0.15f, shoulder.y + unit * 0.05f)
            frontHand = Offset(shoulder.x + dir * unit * (0.14f + 0.30f * ext), shoulder.y + unit * 0.02f)
            backElbow = Offset(shoulder.x - dir * unit * 0.08f, shoulder.y + unit * 0.10f)
            backHand = Offset(shoulder.x + dir * unit * 0.06f, shoulder.y + unit * 0.12f)
        }
        FighterAction.CLAW -> {
            val ext = sin(p * PI.toFloat())
            frontElbow = Offset(shoulder.x + dir * unit * 0.16f, shoulder.y - unit * 0.08f * ext)
            frontHand = Offset(shoulder.x + dir * unit * (0.18f + 0.35f * ext), shoulder.y - unit * 0.04f)
            backElbow = Offset(shoulder.x + dir * unit * 0.12f, shoulder.y + unit * 0.06f)
            backHand = Offset(shoulder.x + dir * unit * (0.14f + 0.28f * ext), shoulder.y + unit * 0.08f)
        }
        FighterAction.BLOCK -> {
            frontElbow = Offset(shoulder.x + dir * unit * 0.10f, shoulder.y + unit * 0.08f)
            frontHand = Offset(shoulder.x + dir * unit * 0.14f, shoulder.y - unit * 0.08f)
            backElbow = Offset(shoulder.x + dir * unit * 0.06f, shoulder.y + unit * 0.10f)
            backHand = Offset(shoulder.x + dir * unit * 0.11f, shoulder.y - unit * 0.04f)
        }
        else -> {
            frontElbow = Offset(shoulder.x + dir * unit * 0.08f, shoulder.y + unit * 0.12f)
            frontHand = Offset(shoulder.x + dir * unit * 0.19f, shoulder.y + unit * 0.04f)
            backElbow = Offset(shoulder.x - dir * unit * 0.07f, shoulder.y + unit * 0.13f)
            backHand = Offset(shoulder.x + dir * unit * 0.07f, shoulder.y + unit * 0.09f)
        }
    }

    // Back arm
    drawLimb(shoulder, backElbow, backHand, unit * 0.062f, silhouette, activeGlow)

    // Head + Shadow Helmet / Hat
    drawCircle(
        color = activeGlow.copy(alpha = 0.6f),
        radius = unit * 0.095f,
        center = headCenter,
    )
    drawCircle(
        color = silhouette,
        radius = unit * 0.085f,
        center = headCenter,
    )

    if (isPlayer) {
        // King of Legion horned helm crest + glowing cyan visor eyes
        val hornPath = Path().apply {
            moveTo(headCenter.x - dir * unit * 0.04f, headCenter.y - unit * 0.06f)
            lineTo(headCenter.x + dir * unit * 0.02f, headCenter.y - unit * 0.17f)
            lineTo(headCenter.x + dir * unit * 0.06f, headCenter.y - unit * 0.05f)
        }
        drawPath(hornPath, color = CyanGlow.copy(alpha = 0.85f))
        drawLine(
            color = CyanGlow,
            start = Offset(headCenter.x + dir * unit * 0.01f, headCenter.y - unit * 0.015f),
            end = Offset(headCenter.x + dir * unit * 0.068f, headCenter.y - unit * 0.025f),
            strokeWidth = 5f,
            cap = StrokeCap.Round,
        )
    } else {
        // Enemy headgear (conical hat for Hong-Joo, twin horns for others)
        if (weaponStyle == EnemyWeaponStyle.STAFF) {
            val hatPath = Path().apply {
                moveTo(headCenter.x - unit * 0.16f, headCenter.y - unit * 0.04f)
                lineTo(headCenter.x, headCenter.y - unit * 0.15f)
                lineTo(headCenter.x + unit * 0.16f, headCenter.y - unit * 0.04f)
                close()
            }
            drawPath(hatPath, color = silhouette)
            drawPath(hatPath, color = activeGlow.copy(alpha = 0.7f), style = Stroke(width = 3f, join = StrokeJoin.Round))
        }
        drawLine(
            color = activeGlow,
            start = Offset(headCenter.x + dir * unit * 0.01f, headCenter.y - unit * 0.015f),
            end = Offset(headCenter.x + dir * unit * 0.065f, headCenter.y - unit * 0.025f),
            strokeWidth = 5f,
            cap = StrokeCap.Round,
        )
    }

    // Front arm
    drawLimb(shoulder, frontElbow, frontHand, unit * 0.065f, silhouette, activeGlow)

    // Weapons & Shadow Claws
    if (isPlayer) {
        // Signature triple glowing cyan claws!
        val clawLen = if (fighter.action == FighterAction.CLAW) unit * 0.28f else unit * 0.13f
        for (i in -1..1) {
            val spreadY = i * unit * 0.028f
            drawLine(
                color = CyanGlow,
                start = Offset(frontHand.x, frontHand.y + spreadY * 0.4f),
                end = Offset(frontHand.x + dir * clawLen, frontHand.y + spreadY - unit * 0.02f),
                strokeWidth = if (fighter.action == FighterAction.CLAW) 6.5f else 4f,
                cap = StrokeCap.Round,
            )
        }
        if (fighter.action == FighterAction.CLAW) {
            drawCircle(
                color = CyanGlow.copy(alpha = 0.40f),
                radius = unit * 0.14f,
                center = Offset(frontHand.x + dir * unit * 0.12f, frontHand.y),
            )
        }
    } else {
        // Enemy weapon silhouette
        when (weaponStyle) {
            EnemyWeaponStyle.STAFF -> {
                val staffStart = Offset(backHand.x - dir * unit * 0.18f, backHand.y + unit * 0.14f)
                val staffEnd = Offset(frontHand.x + dir * unit * 0.24f, frontHand.y - unit * 0.12f)
                drawLine(color = activeGlow, start = staffStart, end = staffEnd, strokeWidth = 6f, cap = StrokeCap.Round)
                drawCircle(color = GoldAccent, radius = 6f, center = staffEnd)
            }
            EnemyWeaponStyle.TWIN_CLAWS -> {
                for (i in -1..1) {
                    drawLine(
                        color = activeGlow,
                        start = frontHand,
                        end = Offset(frontHand.x + dir * unit * 0.15f, frontHand.y + i * unit * 0.025f),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            EnemyWeaponStyle.CLEAVER -> {
                val bladeTip = Offset(frontHand.x + dir * unit * 0.24f, frontHand.y - unit * 0.08f)
                drawLine(color = activeGlow, start = frontHand, end = bladeTip, strokeWidth = 11f, cap = StrokeCap.Butt)
            }
            EnemyWeaponStyle.SHADOW_BLADES -> {
                drawLine(
                    color = activeGlow,
                    start = frontHand,
                    end = Offset(frontHand.x + dir * unit * 0.26f, frontHand.y - unit * 0.06f),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round,
                )
            }
        }

        // Red telegraph glint during enemy wind-up so player can jump or block!
        val isEnemyWindingUp = (
            fighter.action == FighterAction.PUNCH ||
                fighter.action == FighterAction.KICK ||
                fighter.action == FighterAction.CLAW
            ) && fighter.actionProgress < 0.45f
        if (isEnemyWindingUp) {
            drawCircle(
                color = CrimsonEnemy.copy(alpha = 0.75f),
                radius = unit * 0.07f,
                center = headCenter,
                style = Stroke(width = 4f),
            )
        }
    }

    // Block shield arc
    if (fighter.action == FighterAction.BLOCK) {
        drawArc(
            color = activeGlow.copy(alpha = 0.75f),
            startAngle = if (fighter.facingRight) -65f else 115f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(hipX - unit * 0.26f, shoulder.y - unit * 0.16f),
            size = Size(unit * 0.52f, unit * 0.56f),
            style = Stroke(width = 6f, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawLimb(
    start: Offset,
    mid: Offset,
    end: Offset,
    thickness: Float,
    bodyColor: Color,
    rimColor: Color,
) {
    drawLine(
        color = rimColor.copy(alpha = 0.42f),
        start = start,
        end = mid,
        strokeWidth = thickness + 4f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = rimColor.copy(alpha = 0.42f),
        start = mid,
        end = end,
        strokeWidth = thickness * 0.85f + 4f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = bodyColor,
        start = start,
        end = mid,
        strokeWidth = thickness,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = bodyColor,
        start = mid,
        end = end,
        strokeWidth = thickness * 0.85f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawFloatingText(
    textMeasurer: TextMeasurer,
    text: String,
    center: Offset,
    color: Color,
    isCrit: Boolean,
) {
    val style = TextStyle(
        fontFamily = ChakraPetchFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = if (isCrit) 18.sp else 14.sp,
        color = color,
    )
    val layout = textMeasurer.measure(text = text, style = style)
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(
            x = (center.x - layout.size.width * 0.5f).coerceIn(8f, size.width - layout.size.width - 8f),
            y = (center.y - layout.size.height * 0.5f).coerceIn(8f, size.height - layout.size.height - 8f),
        ),
    )
}
