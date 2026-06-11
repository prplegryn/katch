package io.github.prplegryn.katch.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class Glyph {
    Home,
    Browser,
    Settings,
    Save,
    Search,
    Download,
    Folder,
    Cookie,
    Back,
    Forward,
    Refresh,
    Close,
    Check,
    Spark,
    Lock,
}

@Composable
fun AkText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = AkTheme.type.body,
    color: Color = AkTheme.colors.textPrimary,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        style = style.copy(color = color, textAlign = textAlign ?: TextAlign.Unspecified),
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 28.dp,
    padding: Dp = 18.dp,
    borderAlpha: Float = 1f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AkTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(colors.surface)
            .border(1.dp, colors.border.copy(alpha = borderAlpha), RoundedCornerShape(radius))
            .padding(padding),
        content = content,
    )
}

@Composable
fun ActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glyph: Glyph? = null,
    secondary: Boolean = false,
) {
    val colors = AkTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(if (enabled) 1f else 0.98f, label = "buttonScale")
    val background = if (secondary) {
        Brush.linearGradient(listOf(colors.surfaceStrong, colors.surfaceStrong))
    } else {
        colors.accentBrush
    }
    val contentColor = if (secondary) colors.textPrimary else Color.White
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 50.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) background else Brush.linearGradient(listOf(colors.surfaceSoft, colors.surfaceSoft)))
            .border(
                1.dp,
                if (secondary) colors.border else Color.White.copy(alpha = 0.26f),
                RoundedCornerShape(18.dp),
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != null) {
            GlyphIcon(glyph = glyph, color = contentColor, size = 20.dp)
            Spacer(Modifier.width(8.dp))
        }
        AkText(text = label, style = AkTheme.type.label, color = contentColor)
    }
}

@Composable
fun IconButton(
    glyph: Glyph,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AkTheme.colors
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(colors.surfaceStrong)
            .border(1.dp, colors.border, CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph = glyph, color = if (enabled) colors.textPrimary else colors.textMuted, size = 22.dp)
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = AkTheme.colors
    val background by animateColorAsState(
        if (selected) colors.accent.copy(alpha = 0.17f) else colors.surfaceSoft,
        label = "pillBg",
    )
    val border by animateColorAsState(
        if (selected) colors.accent.copy(alpha = 0.74f) else colors.border,
        label = "pillBorder",
    )
    val interactionSource = remember { MutableInteractionSource() }
    val clickable = if (onClick == null) {
        Modifier
    } else {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick,
        )
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .then(clickable)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        AkText(
            text = text,
            style = AkTheme.type.tiny,
            color = if (selected) colors.accent else colors.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
fun AkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 4,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = AkTheme.colors
    Column(modifier = modifier) {
        AkText(text = label, style = AkTheme.type.label, color = colors.textSecondary)
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = AkTheme.type.body.copy(color = colors.textPrimary),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = VisualTransformation.None,
            modifier = Modifier.fillMaxWidth(),
            minLines = minLines,
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.surfaceStrong)
                        .border(1.dp, colors.border, RoundedCornerShape(22.dp))
                        .padding(16.dp),
                ) {
                    if (value.isBlank()) {
                        AkText(
                            text = placeholder,
                            style = AkTheme.type.body,
                            color = colors.textMuted,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
fun ProgressStrip(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val colors = AkTheme.colors
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progress")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(colors.surfaceSoft),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(RoundedCornerShape(999.dp))
                .background(colors.accentBrush),
        )
    }
}

@Composable
fun AnimatedStatus(
    visible: Boolean,
    text: String?,
    modifier: Modifier = Modifier,
) {
    val colors = AkTheme.colors
    AnimatedVisibility(
        visible = visible && !text.isNullOrBlank(),
        enter = fadeIn(tween(180)) + expandVertically(tween(220, easing = FastOutSlowInEasing)),
        exit = fadeOut(tween(120)) + shrinkVertically(tween(160, easing = FastOutSlowInEasing)),
        modifier = modifier,
    ) {
        GlassCard(radius = 20.dp, padding = 14.dp, borderAlpha = 0.72f) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
                Spacer(Modifier.width(10.dp))
                AkText(text = text.orEmpty(), style = AkTheme.type.body, color = colors.textSecondary)
            }
        }
    }
}

@Composable
fun FloatingOrbs(modifier: Modifier = Modifier) {
    val colors = AkTheme.colors
    val transition = rememberInfiniteTransition(label = "orbs")
    val driftOne by transition.animateFloat(
        initialValue = -24f,
        targetValue = 34f,
        animationSpec = infiniteRepeatable(tween(5200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orbOne",
    )
    val driftTwo by transition.animateFloat(
        initialValue = 28f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(tween(6800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orbTwo",
    )
    Box(modifier = modifier) {
        Box(
            Modifier
                .size(220.dp)
                .graphicsLayer { translationX = driftOne; translationY = driftTwo }
                .blur(54.dp)
                .background(colors.blobOne, CircleShape),
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(190.dp)
                .graphicsLayer { translationX = driftTwo; translationY = driftOne }
                .blur(58.dp)
                .background(colors.blobTwo, CircleShape),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .size(260.dp)
                .graphicsLayer { translationX = driftOne * -0.7f }
                .blur(70.dp)
                .background(colors.blobThree, CircleShape),
        )
    }
}

@Composable
fun GlyphIcon(
    glyph: Glyph,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(
            width = size.minDimension.toPx() * 0.085f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val w = this.size.width
        val h = this.size.height
        fun p(x: Float, y: Float) = Offset(w * x, h * y)
        when (glyph) {
            Glyph.Home -> {
                val path = Path().apply {
                    moveTo(w * 0.18f, h * 0.48f)
                    lineTo(w * 0.5f, h * 0.2f)
                    lineTo(w * 0.82f, h * 0.48f)
                    lineTo(w * 0.82f, h * 0.82f)
                    lineTo(w * 0.6f, h * 0.82f)
                    lineTo(w * 0.6f, h * 0.62f)
                    lineTo(w * 0.4f, h * 0.62f)
                    lineTo(w * 0.4f, h * 0.82f)
                    lineTo(w * 0.18f, h * 0.82f)
                    close()
                }
                drawPath(path, color, style = stroke)
            }
            Glyph.Browser -> {
                drawCircle(color, radius = w * 0.33f, center = p(0.5f, 0.5f), style = stroke)
                drawLine(color, p(0.2f, 0.5f), p(0.8f, 0.5f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.5f, 0.18f), p(0.5f, 0.82f), stroke.width, StrokeCap.Round)
                drawArc(color, 75f, 210f, false, topLeft = p(0.28f, 0.2f), size = Size(w * 0.44f, h * 0.6f), style = stroke)
            }
            Glyph.Settings -> {
                drawCircle(color, radius = w * 0.14f, center = p(0.5f, 0.5f), style = stroke)
                repeat(6) { i ->
                    val angle = Math.toRadians((i * 60.0) - 90.0)
                    val inner = p(0.5f + kotlin.math.cos(angle).toFloat() * 0.25f, 0.5f + kotlin.math.sin(angle).toFloat() * 0.25f)
                    val outer = p(0.5f + kotlin.math.cos(angle).toFloat() * 0.38f, 0.5f + kotlin.math.sin(angle).toFloat() * 0.38f)
                    drawLine(color, inner, outer, stroke.width, StrokeCap.Round)
                }
            }
            Glyph.Save, Glyph.Download -> {
                drawLine(color, p(0.5f, 0.18f), p(0.5f, 0.62f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.31f, 0.45f), p(0.5f, 0.64f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.69f, 0.45f), p(0.5f, 0.64f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.24f, 0.82f), p(0.76f, 0.82f), stroke.width, StrokeCap.Round)
            }
            Glyph.Search -> {
                drawCircle(color, radius = w * 0.24f, center = p(0.43f, 0.43f), style = stroke)
                drawLine(color, p(0.6f, 0.6f), p(0.82f, 0.82f), stroke.width, StrokeCap.Round)
            }
            Glyph.Folder -> {
                val path = Path().apply {
                    moveTo(w * 0.16f, h * 0.32f)
                    lineTo(w * 0.4f, h * 0.32f)
                    lineTo(w * 0.48f, h * 0.42f)
                    lineTo(w * 0.84f, h * 0.42f)
                    lineTo(w * 0.78f, h * 0.78f)
                    lineTo(w * 0.18f, h * 0.78f)
                    close()
                }
                drawPath(path, color, style = stroke)
            }
            Glyph.Cookie -> {
                drawCircle(color, radius = w * 0.32f, center = p(0.5f, 0.5f), style = stroke)
                drawCircle(color, radius = w * 0.035f, center = p(0.38f, 0.42f))
                drawCircle(color, radius = w * 0.035f, center = p(0.58f, 0.33f))
                drawCircle(color, radius = w * 0.035f, center = p(0.6f, 0.61f))
            }
            Glyph.Back -> {
                drawLine(color, p(0.72f, 0.2f), p(0.32f, 0.5f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.32f, 0.5f), p(0.72f, 0.8f), stroke.width, StrokeCap.Round)
            }
            Glyph.Forward -> {
                drawLine(color, p(0.28f, 0.2f), p(0.68f, 0.5f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.68f, 0.5f), p(0.28f, 0.8f), stroke.width, StrokeCap.Round)
            }
            Glyph.Refresh -> {
                drawArc(color, -35f, 285f, false, topLeft = p(0.22f, 0.22f), size = Size(w * 0.56f, h * 0.56f), style = stroke)
                drawLine(color, p(0.75f, 0.25f), p(0.78f, 0.48f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.75f, 0.25f), p(0.55f, 0.31f), stroke.width, StrokeCap.Round)
            }
            Glyph.Close -> {
                drawLine(color, p(0.28f, 0.28f), p(0.72f, 0.72f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.72f, 0.28f), p(0.28f, 0.72f), stroke.width, StrokeCap.Round)
            }
            Glyph.Check -> {
                drawLine(color, p(0.23f, 0.52f), p(0.43f, 0.72f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.43f, 0.72f), p(0.78f, 0.28f), stroke.width, StrokeCap.Round)
            }
            Glyph.Spark -> {
                drawLine(color, p(0.5f, 0.14f), p(0.5f, 0.86f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.14f, 0.5f), p(0.86f, 0.5f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.28f, 0.28f), p(0.72f, 0.72f), stroke.width * 0.75f, StrokeCap.Round)
                drawLine(color, p(0.72f, 0.28f), p(0.28f, 0.72f), stroke.width * 0.75f, StrokeCap.Round)
            }
            Glyph.Lock -> {
                drawRoundRect(
                    color = color,
                    topLeft = p(0.24f, 0.43f),
                    size = Size(w * 0.52f, h * 0.38f),
                    style = stroke,
                )
                drawArc(color, 180f, 180f, false, topLeft = p(0.34f, 0.2f), size = Size(w * 0.32f, h * 0.42f), style = stroke)
            }
        }
    }
}

private val Dp.minDimension: Dp
    get() = this
