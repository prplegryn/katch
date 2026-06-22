package io.github.prplegryn.katch.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
    Link,
    List,
    Paste,
    Info,
    Storage,
}

enum class ActionTone {
    Primary,
    Secondary,
    Danger,
    Quiet,
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
fun WorkPanel(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AkTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .padding(padding),
        content = content,
    )
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AkTheme.colors.divider),
    )
}

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
) {
    val colors = AkTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AkText(text = title, style = AkTheme.type.section, color = colors.textPrimary)
        Spacer(Modifier.weight(1f))
        if (!meta.isNullOrBlank()) {
            AkText(text = meta, style = AkTheme.type.label, color = colors.textMuted, maxLines = 1)
        }
    }
}

@Composable
fun ActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glyph: Glyph? = null,
    tone: ActionTone = ActionTone.Primary,
) {
    val colors = AkTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val background by animateColorAsState(
        when {
            !enabled -> colors.surfaceMuted
            tone == ActionTone.Primary -> colors.accent
            tone == ActionTone.Danger -> colors.dangerSoft
            tone == ActionTone.Quiet -> Color.Transparent
            else -> colors.surfaceRaised
        },
        label = "buttonBackground",
    )
    val border by animateColorAsState(
        when {
            !enabled -> colors.border
            tone == ActionTone.Primary -> colors.accent
            tone == ActionTone.Danger -> colors.danger
            tone == ActionTone.Quiet -> Color.Transparent
            else -> colors.border
        },
        label = "buttonBorder",
    )
    val contentColor = when {
        !enabled -> colors.textMuted
        tone == ActionTone.Primary -> colors.onAccent
        tone == ActionTone.Danger -> colors.danger
        else -> colors.textPrimary
    }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != null) {
            GlyphIcon(glyph = glyph, color = contentColor, size = 19.dp)
            Spacer(Modifier.width(8.dp))
        }
        AkText(
            text = label,
            style = AkTheme.type.label,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun IconButton(
    glyph: Glyph,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
) {
    val colors = AkTheme.colors
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.selected else colors.surfaceRaised)
            .border(1.dp, if (selected) colors.accent else colors.border, RoundedCornerShape(8.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(
            glyph = glyph,
            color = when {
                !enabled -> colors.textMuted
                selected -> colors.accent
                else -> colors.textPrimary
            },
            size = 21.dp,
        )
    }
}

@Composable
fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    warning: Boolean = false,
) {
    val colors = AkTheme.colors
    val background = when {
        active -> colors.successSoft
        warning -> colors.warningSoft
        else -> colors.surfaceMuted
    }
    val foreground = when {
        active -> colors.success
        warning -> colors.warning
        else -> colors.textSecondary
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(foreground),
        )
        Spacer(Modifier.width(7.dp))
        AkText(text = text, style = AkTheme.type.tiny, color = foreground, maxLines = 1)
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
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = (minLines * 28).dp + 32.dp),
            minLines = minLines,
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.input)
                        .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                        .padding(14.dp),
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
            .height(7.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(colors.surfaceMuted),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(RoundedCornerShape(999.dp))
                .background(colors.accent),
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceMuted)
                .border(1.dp, colors.divider, RoundedCornerShape(8.dp))
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlyphIcon(glyph = Glyph.Info, color = colors.accent, size = 18.dp)
            Spacer(Modifier.width(10.dp))
            AkText(text = text.orEmpty(), style = AkTheme.type.body, color = colors.textSecondary)
        }
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
            width = size.toPx() * 0.085f,
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
                drawCircle(color, radius = w * 0.32f, center = p(0.5f, 0.5f), style = stroke)
                drawLine(color, p(0.2f, 0.5f), p(0.8f, 0.5f), stroke.width, StrokeCap.Round)
                drawArc(color, 75f, 210f, false, topLeft = p(0.28f, 0.2f), size = Size(w * 0.44f, h * 0.6f), style = stroke)
                drawArc(color, -105f, 210f, false, topLeft = p(0.28f, 0.2f), size = Size(w * 0.44f, h * 0.6f), style = stroke)
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
            Glyph.Folder, Glyph.Storage -> {
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
                drawLine(color, p(0.5f, 0.18f), p(0.5f, 0.82f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.18f, 0.5f), p(0.82f, 0.5f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.31f, 0.31f), p(0.69f, 0.69f), stroke.width * 0.75f, StrokeCap.Round)
                drawLine(color, p(0.69f, 0.31f), p(0.31f, 0.69f), stroke.width * 0.75f, StrokeCap.Round)
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
            Glyph.Link -> {
                drawArc(color, 130f, 260f, false, topLeft = p(0.12f, 0.28f), size = Size(w * 0.46f, h * 0.44f), style = stroke)
                drawArc(color, -50f, 260f, false, topLeft = p(0.42f, 0.28f), size = Size(w * 0.46f, h * 0.44f), style = stroke)
                drawLine(color, p(0.42f, 0.5f), p(0.58f, 0.5f), stroke.width, StrokeCap.Round)
            }
            Glyph.List -> {
                repeat(3) { index ->
                    val y = 0.3f + index * 0.2f
                    drawCircle(color, radius = w * 0.035f, center = p(0.22f, y))
                    drawLine(color, p(0.34f, y), p(0.82f, y), stroke.width, StrokeCap.Round)
                }
            }
            Glyph.Paste -> {
                drawRoundRect(
                    color = color,
                    topLeft = p(0.25f, 0.24f),
                    size = Size(w * 0.5f, h * 0.58f),
                    style = stroke,
                )
                drawLine(color, p(0.38f, 0.18f), p(0.62f, 0.18f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.38f, 0.35f), p(0.62f, 0.35f), stroke.width, StrokeCap.Round)
            }
            Glyph.Info -> {
                drawCircle(color, radius = w * 0.32f, center = p(0.5f, 0.5f), style = stroke)
                drawLine(color, p(0.5f, 0.46f), p(0.5f, 0.68f), stroke.width, StrokeCap.Round)
                drawCircle(color, radius = w * 0.035f, center = p(0.5f, 0.32f))
            }
        }
    }
}
