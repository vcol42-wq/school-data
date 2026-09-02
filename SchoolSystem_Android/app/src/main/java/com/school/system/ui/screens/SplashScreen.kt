package com.school.system.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

@Composable
fun SplashScreen(
    onAnimationFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimation = true
        // Stay on splash screen for 3.0 seconds then transition
        delay(3000)
        onAnimationFinished()
    }

    // Infinite transitions for ongoing cosmic life & hovering
    val infiniteTransition = rememberInfiniteTransition(label = "cosmicLife")

    // 1. Nebula Clouds Rotation & Swirl
    val nebulaAngle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "nebula1"
    )
    val nebulaAngle2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "nebula2"
    )
    val nebulaPulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nebulaPulse"
    )

    // 2. Graduation Cap Rising Animation (Upwards from bottom)
    val capRiseProgress by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1300,
            delayMillis = 200,
            easing = FastOutSlowInEasing
        ),
        label = "capRiseProgress"
    )

    // Cap Gentle Hovering / Levitation once risen
    val capHoverOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "capHover"
    )

    // Cap Tilt & Stabilization
    val capTiltAngle by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 24f,
        animationSpec = tween(
            durationMillis = 1400,
            delayMillis = 150,
            easing = FastOutSlowInEasing
        ),
        label = "capTilt"
    )

    // 3. Tassel Swaying in Cosmic Gravity
    val tasselSwayAngle by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tasselSway"
    )

    // 4. Star Shimmer & Twinkling Animation
    val starGlowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starGlow"
    )

    // Specular Shine sweep across the cap
    val shineSweepProgress by animateFloatAsState(
        targetValue = if (capRiseProgress > 0.8f) 1f else 0f,
        animationSpec = tween(1000, delayMillis = 600, easing = LinearOutSlowInEasing),
        label = "shineSweep"
    )

    // 5. Logo & Typography Entrance
    val contentAlpha by animateFloatAsState(
        targetValue = if (capRiseProgress > 0.6f) 1f else 0f,
        animationSpec = tween(800, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "contentAlpha"
    )
    val contentSlideY by animateFloatAsState(
        targetValue = if (capRiseProgress > 0.6f) 0f else 35f,
        animationSpec = tween(800, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "contentSlideY"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712)) // Deep space base
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onAnimationFinished()
            },
        contentAlignment = Alignment.Center
    ) {
        // LAYER 1: Dynamic Nebula Swirls & Cosmic Glows
        CosmicNebulaBackground(
            nebulaAngle1 = nebulaAngle1,
            nebulaAngle2 = nebulaAngle2,
            nebulaPulse = nebulaPulse
        )

        // LAYER 2: Twinkling Stars & Cosmic Sparkles Canvas
        TwinklingStarsLayer(
            starGlowPulse = starGlowPulse,
            capRiseProgress = capRiseProgress
        )

        // LAYER 3: Main Central Composition
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Rising Graduation Cap + Glowing Aura Stage
            val capYOffset = (1f - capRiseProgress) * 260f + (if (capRiseProgress > 0.95f) capHoverOffset else 0f)
            val capScale = 0.5f + capRiseProgress * 0.55f

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .offset(y = capYOffset.dp)
                    .scale(capScale)
                    .rotate(capTiltAngle),
                contentAlignment = Alignment.Center
            ) {
                // Radiant Backlight / Divine Cosmic Halo behind Cap
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(nebulaPulse)
                        .alpha(0.6f * capRiseProgress)
                        .blur(50.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFBBF24).copy(alpha = 0.8f),
                                    Color(0xFF8B5CF6).copy(alpha = 0.6f),
                                    Color(0xFF3B82F6).copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // The Magnificent Vector Graduation Cap
                GraduationCapNebulaVector(
                    tasselAngle = tasselSwayAngle,
                    shineProgress = shineSweepProgress
                )

                // Surrounding Sparkling 4-point Stars directly orbiting the Cap
                OrbitingCapStars(
                    starGlowPulse = starGlowPulse,
                    visibleAlpha = capRiseProgress
                )
            }

            Spacer(Modifier.height(16.dp))

            // App Icon Badge & Branding (Elevates with cosmic aura)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(contentAlpha)
                    .offset(y = contentSlideY.dp)
            ) {
                // App Logo Badge with Glassmorphic Border
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF1E1B4B).copy(alpha = 0.7f),
                    shadowElevation = 14.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFFFDE68A), Color(0xFF818CF8), Color(0xFF38BDF8))
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    listOf(Color(0xFF3B82F6).copy(alpha = 0.4f), Color(0xFF0F172A))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "شعار التطبيق",
                            modifier = Modifier.size(38.dp),
                            tint = Color(0xFFFDE68A)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Title with Gold Shimmer Sparkles
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "سجل المدرس الذكي",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "نظام الإدارة والتقييم المدرسي المتكامل",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(18.dp))

                // Cosmic Pill Badge
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, 
                        Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4)))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF10B981), CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "سحابة المنظومة المدرسية الفورية ✦",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }

        // Bottom Cosmic Sparkle Indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .alpha(contentAlpha)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(7.dp).scale(starGlowPulse).background(Color(0xFF38BDF8), CircleShape))
                Box(Modifier.size(7.dp).scale(1.4f - starGlowPulse * 0.4f).background(Color(0xFFA855F7), CircleShape))
                Box(Modifier.size(7.dp).scale(starGlowPulse).background(Color(0xFFFBBF24), CircleShape))
            }
        }
    }
}

/**
 * Nebula Swirling Background with rich vibrant deep cosmic colors
 */
@Composable
fun CosmicNebulaBackground(
    nebulaAngle1: Float,
    nebulaAngle2: Float,
    nebulaPulse: Float
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Nebula Cloud 1: Magenta / Deep Violet Swirl (Top Left)
        Box(
            modifier = Modifier
                .size(380.dp)
                .align(Alignment.TopStart)
                .offset(x = (-40).dp, y = (-20).dp)
                .rotate(nebulaAngle1)
                .scale(nebulaPulse)
                .blur(90.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF9333EA).copy(alpha = 0.55f),
                            Color(0xFFC026D3).copy(alpha = 0.35f),
                            Color(0xFF4C1D95).copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Nebula Cloud 2: Cosmic Cyan / Deep Blue Swirl (Bottom Right)
        Box(
            modifier = Modifier
                .size(420.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 80.dp)
                .rotate(nebulaAngle2)
                .scale(nebulaPulse * 0.95f)
                .blur(100.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0284C7).copy(alpha = 0.50f),
                            Color(0xFF2563EB).copy(alpha = 0.35f),
                            Color(0xFF0F172A).copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Nebula Center Core: Warm Gold / Amber Accent
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.Center)
                .scale(nebulaPulse * 1.1f)
                .blur(80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFD97706).copy(alpha = 0.25f),
                            Color(0xFF7C3AED).copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

/**
 * Deep Space Twinkling Stars & Cosmic Stardust
 */
@Composable
fun TwinklingStarsLayer(
    starGlowPulse: Float,
    capRiseProgress: Float
) {
    val randomStars = remember {
        List(40) {
            Triple(
                Random.nextFloat(), // x percentage
                Random.nextFloat(), // y percentage
                Random.nextFloat() * 0.8f + 0.2f // scale/brightness factor
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Draw background static + twinkling starfield
        randomStars.forEachIndexed { idx, (px, py, factor) ->
            val starX = px * w
            val starY = py * h
            val phase = (idx % 3)
            val dynamicAlpha = when (phase) {
                0 -> (starGlowPulse * factor).coerceIn(0.2f, 0.95f)
                1 -> ((1.2f - starGlowPulse) * factor).coerceIn(0.2f, 0.95f)
                else -> factor * 0.6f
            }

            val starColor = when (idx % 4) {
                0 -> Color(0xFFFDE68A) // Soft Gold
                1 -> Color(0xFFBAE6FD) // Light Cyan
                2 -> Color(0xFFE9D5FF) // Light Purple
                else -> Color.White
            }

            drawCircle(
                color = starColor.copy(alpha = dynamicAlpha),
                radius = if (idx % 5 == 0) 2.6f * factor else 1.5f * factor,
                center = Offset(starX, starY)
            )
        }
    }
}

/**
 * 4-Point Shimmering Star Crosses Orbiting the Graduation Cap
 */
@Composable
fun OrbitingCapStars(
    starGlowPulse: Float,
    visibleAlpha: Float
) {
    Canvas(modifier = Modifier.fillMaxSize().alpha(visibleAlpha)) {
        val cx = size.width / 2
        val cy = size.height / 2

        // Coordinates of 6 major sparkling stars around the cap
        val starPoints = listOf(
            Offset(cx - 75f, cy - 45f), // Top left star
            Offset(cx + 80f, cy - 50f), // Top right star
            Offset(cx - 95f, cy + 20f), // Mid left star
            Offset(cx + 90f, cy + 15f), // Mid right star
            Offset(cx + 10f, cy - 70f), // Apex star
            Offset(cx + 65f, cy + 60f)  // Tassel tip glow
        )

        starPoints.forEachIndexed { i, pt ->
            val scale = (if (i % 2 == 0) starGlowPulse else (1.2f - starGlowPulse * 0.4f)).coerceIn(0.5f, 1.4f)
            val starSize = (14f + (i % 3) * 6f) * scale

            // Draw 4-point Diamond Star Cross
            val starPath = Path().apply {
                moveTo(pt.x, pt.y - starSize)
                quadraticBezierTo(pt.x, pt.y, pt.x + starSize, pt.y)
                quadraticBezierTo(pt.x, pt.y, pt.x, pt.y + starSize)
                quadraticBezierTo(pt.x, pt.y, pt.x - starSize, pt.y)
                quadraticBezierTo(pt.x, pt.y, pt.x, pt.y - starSize)
                close()
            }

            // Outer star glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFDE68A).copy(alpha = 0.7f * scale),
                        Color(0xFFF59E0B).copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = pt,
                    radius = starSize * 1.6f
                ),
                radius = starSize * 1.6f,
                center = pt
            )

            // Inner crisp star
            drawPath(
                path = starPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White, Color(0xFFFDE68A), Color(0xFFF59E0B))
                )
            )
        }
    }
}

/**
 * High-Definition Graduation Mortarboard Hat (قبعة التخرج السديمية المتألقة)
 */
@Composable
fun GraduationCapNebulaVector(
    tasselAngle: Float,
    shineProgress: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Cap Base/Crown (قاعدة القبعة تحت اللوح الماسي)
        val skullPath = Path().apply {
            moveTo(w * 0.30f, h * 0.48f)
            quadraticBezierTo(w * 0.50f, h * 0.74f, w * 0.70f, h * 0.48f)
            lineTo(w * 0.70f, h * 0.58f)
            quadraticBezierTo(w * 0.50f, h * 0.84f, w * 0.30f, h * 0.58f)
            close()
        }
        drawPath(
            path = skullPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1E1B4B), Color(0xFF0F172A), Color(0xFF020617))
            )
        )
        // Base Gold Trim
        val trimPath = Path().apply {
            moveTo(w * 0.30f, h * 0.58f)
            quadraticBezierTo(w * 0.50f, h * 0.84f, w * 0.70f, h * 0.58f)
        }
        drawPath(
            path = trimPath,
            brush = Brush.horizontalGradient(
                listOf(Color(0xFFD97706), Color(0xFFFDE68A), Color(0xFFD97706))
            ),
            style = Stroke(width = 2.5f)
        )

        // 2. Diamond Mortarboard Top (لوح القبعة الماسي ذو المنظور الثلاثي الأبعاد)
        val boardPath = Path().apply {
            moveTo(w * 0.50f, h * 0.10f) // Top vertex
            lineTo(w * 0.95f, h * 0.35f) // Right vertex
            lineTo(w * 0.50f, h * 0.60f) // Bottom vertex
            lineTo(w * 0.05f, h * 0.35f) // Left vertex
            close()
        }

        // Diamond Shadow / 3D Thickness Edge
        val edgePath = Path().apply {
            moveTo(w * 0.05f, h * 0.35f)
            lineTo(w * 0.50f, h * 0.60f)
            lineTo(w * 0.95f, h * 0.35f)
            lineTo(w * 0.95f, h * 0.41f)
            lineTo(w * 0.50f, h * 0.66f)
            lineTo(w * 0.05f, h * 0.41f)
            close()
        }
        drawPath(
            path = edgePath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
            )
        )

        // Top Surface of Mortarboard with Deep Celestial Midnight Blue/Indigo Gradient
        drawPath(
            path = boardPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF312E81),
                    Color(0xFF1E1B4B),
                    Color(0xFF0F172A)
                ),
                start = Offset(w * 0.50f, h * 0.10f),
                end = Offset(w * 0.50f, h * 0.60f)
            )
        )

        // Golden Celestial Edge Trim around the diamond
        drawPath(
            path = boardPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFDE68A),
                    Color(0xFFF59E0B),
                    Color(0xFFB45309),
                    Color(0xFFFDE68A)
                )
            ),
            style = Stroke(width = 2.2f)
        )

        // Specular Sweep Highlight (شعاع البريق المتحرك عبر القبعة)
        if (shineProgress > 0f && shineProgress < 1f) {
            val shineX = w * (shineProgress * 1.2f - 0.1f)
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.7f), Color.Transparent),
                    startX = shineX - 30f,
                    endX = shineX + 30f
                ),
                start = Offset(shineX - 20f, h * 0.15f),
                end = Offset(shineX + 20f, h * 0.55f),
                strokeWidth = 14f
            )
        }

        // 3. Center Golden Button (الزر المركزي المرصع)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFFBEB), Color(0xFFFBBF24), Color(0xFFB45309))
            ),
            radius = 6.5f,
            center = Offset(w * 0.50f, h * 0.35f)
        )

        // 4. Golden Celestial Tassel Ribbon & Flowing Charm (الشرابة الذهبية المتأرجحة)
        val tasselPivot = Offset(w * 0.50f, h * 0.35f)
        val tasselRibbonEnd = Offset(w * 0.83f, h * 0.46f)

        // Ribbon string from button to corner
        val ribbonPath = Path().apply {
            moveTo(tasselPivot.x, tasselPivot.y)
            quadraticBezierTo(w * 0.68f, h * 0.37f, tasselRibbonEnd.x, tasselRibbonEnd.y)
        }
        drawPath(
            path = ribbonPath,
            brush = Brush.linearGradient(listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))),
            style = Stroke(width = 3.0f, cap = StrokeCap.Round)
        )

        // Hanging Tassel Body (swings dynamically with tasselAngle in zero-gravity)
        val swayRad = (tasselAngle * (PI / 180f)).toFloat()
        val tasselLength = h * 0.35f
        val tasselBottomX = tasselRibbonEnd.x + sin(swayRad) * tasselLength
        val tasselBottomY = tasselRibbonEnd.y + cos(swayRad) * tasselLength

        // Tassel Ring
        drawCircle(
            color = Color(0xFFD97706),
            radius = 4.0f,
            center = tasselRibbonEnd
        )

        // Tassel Golden Threads
        drawLine(
            brush = Brush.linearGradient(listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))),
            start = tasselRibbonEnd,
            end = Offset(tasselBottomX, tasselBottomY),
            strokeWidth = 5.5f,
            cap = StrokeCap.Round
        )

        // Tassel Flare Glow at bottom tip
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFFBEB), Color(0xFFFBBF24), Color(0xFFD97706), Color.Transparent)
            ),
            radius = 6.5f,
            center = Offset(tasselBottomX, tasselBottomY)
        )
    }
}
