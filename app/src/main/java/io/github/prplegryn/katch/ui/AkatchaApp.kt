package io.github.prplegryn.katch.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.using
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ripple.ripple
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.prplegryn.katch.MainViewModel
import io.github.prplegryn.katch.data.AppTab
import io.github.prplegryn.katch.data.AppUiState
import io.github.prplegryn.katch.data.JobStage
import io.github.prplegryn.katch.data.MediaFormat
import io.github.prplegryn.katch.data.ThemeMode
import io.github.prplegryn.katch.web.CookieExporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun AkatchaApp(
    viewModel: MainViewModel,
    requestStorageAccess: () -> Unit,
    chooseDirectory: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(
        state.hasAllFilesAccess,
        state.settings.storagePromptShown,
        state.settings.downloadTreeUri,
    ) {
        if (!state.hasAllFilesAccess &&
            !state.settings.storagePromptShown &&
            state.settings.downloadTreeUri == null
        ) {
            delay(650)
            requestStorageAccess()
        }
    }

    AkatchaTheme(themeMode = state.settings.themeMode) {
        SystemBars(themeMode = state.settings.themeMode)
        AppShell(
            state = state,
            actions = AppActions(
                openTab = viewModel::openTab,
                updateInput = viewModel::updateInput,
                analyze = viewModel::analyze,
                selectFormat = viewModel::selectFormat,
                download = viewModel::download,
                saveCookies = viewModel::saveCookies,
                clearCookies = viewModel::clearCookies,
                setThemeMode = viewModel::setThemeMode,
                chooseDirectory = chooseDirectory,
                clearDirectory = viewModel::clearDownloadTree,
                requestStorageAccess = requestStorageAccess,
            ),
        )
    }
}

@Composable
private fun SystemBars(themeMode: ThemeMode) {
    val view = LocalView.current
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }
}

private data class AppActions(
    val openTab: (AppTab) -> Unit,
    val updateInput: (String) -> Unit,
    val analyze: () -> Unit,
    val selectFormat: (String) -> Unit,
    val download: (MediaFormat?) -> Unit,
    val saveCookies: () -> Unit,
    val clearCookies: () -> Unit,
    val setThemeMode: (ThemeMode) -> Unit,
    val chooseDirectory: () -> Unit,
    val clearDirectory: () -> Unit,
    val requestStorageAccess: () -> Unit,
)

@Composable
private fun AppShell(state: AppUiState, actions: AppActions) {
    val colors = AkTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundBrush),
    ) {
        FloatingOrbs(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = state.tab,
                transitionSpec = { tabTransform(initialState, targetState) },
                label = "screen",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { tab ->
                when (tab) {
                    AppTab.Home -> HomeScreen(state = state, actions = actions)
                    AppTab.Browser -> BrowserScreen(state = state, actions = actions)
                    AppTab.Settings -> SettingsScreen(state = state, actions = actions)
                }
            }
            BottomDock(selected = state.tab, onSelect = actions.openTab)
        }
    }
}

private fun tabTransform(initial: AppTab, target: AppTab): ContentTransform {
    val direction = if (target.ordinal > initial.ordinal) 1 else -1
    return (
        slideInHorizontally(
            animationSpec = tween(260, easing = FastOutSlowInEasing),
            initialOffsetX = { it / 4 * direction },
        ) + fadeIn(tween(180))
        ).togetherWith(
        slideOutHorizontally(
            animationSpec = tween(220, easing = FastOutSlowInEasing),
            targetOffsetX = { -it / 6 * direction },
        ) + fadeOut(tween(160)),
    ).using(SizeTransform(clip = false))
}

@Composable
private fun BottomDock(selected: AppTab, onSelect: (AppTab) -> Unit) {
    val colors = AkTheme.colors
    val tabs = listOf(
        Triple(AppTab.Home, Glyph.Home, "下载"),
        Triple(AppTab.Browser, Glyph.Browser, "登录"),
        Triple(AppTab.Settings, Glyph.Settings, "设置"),
    )
    Row(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.surfaceStrong)
            .border(1.dp, colors.border, RoundedCornerShape(28.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tabs.forEach { (tab, glyph, label) ->
            val active = selected == tab
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (active) colors.accent.copy(alpha = 0.16f) else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = colors.accent.copy(alpha = 0.20f)),
                        onClick = { onSelect(tab) },
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlyphIcon(
                    glyph = glyph,
                    color = if (active) colors.accent else colors.textMuted,
                    size = 20.dp,
                )
                AnimatedVisibility(visible = active) {
                    Row {
                        Spacer(Modifier.width(7.dp))
                        AkText(text = label, style = AkTheme.type.label, color = colors.accent)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeScreen(state: AppUiState, actions: AppActions) {
    val colors = AkTheme.colors
    val clipboard = LocalClipboardManager.current
    val formats = state.videoInfo?.formats.orEmpty()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pagePadding = if (maxWidth > 620.dp) AkTheme.spacing.pageLarge else AkTheme.spacing.page
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = pagePadding,
                end = pagePadding,
                top = 18.dp,
                bottom = 18.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                HeaderBlock(
                    eyebrow = "AKATCHA / XHS FORMAT SCANNER",
                    title = "把小红书链接拆成完整格式清单",
                    subtitle = "输入或粘贴分享文本，自动提取链接；使用已保存 Cookie 调用 yt-dlp，并额外扫描页面源码，不合并、不去重。",
                )
            }
            item {
                GlassCard(modifier = Modifier.widthIn(max = 860.dp).fillMaxWidth()) {
                    AkTextField(
                        value = state.inputText,
                        onValueChange = actions.updateInput,
                        label = "分享文本",
                        placeholder = "例：牙牙大屏小舞蹈 http://xhslink.com/o/AOASQXmnp3X 复制这段，去【小红书】发现更多好内容~",
                        minLines = 5,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionButton(
                            label = "粘贴",
                            glyph = Glyph.Spark,
                            secondary = true,
                            onClick = {
                                clipboard.getText()?.text?.let(actions.updateInput)
                            },
                            modifier = Modifier.weight(0.8f),
                        )
                        ActionButton(
                            label = if (state.stage == JobStage.Extracting) "探测中" else "解析格式",
                            glyph = Glyph.Search,
                            enabled = state.stage != JobStage.Extracting && state.stage != JobStage.Downloading,
                            onClick = actions.analyze,
                            modifier = Modifier.weight(1.2f),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    state.extractedUrl?.let { url ->
                        Pill(text = url, selected = true, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            item {
                EnvironmentRow(state = state, actions = actions)
            }
            item {
                AnimatedStatus(visible = true, text = state.message, modifier = Modifier.widthIn(max = 860.dp).fillMaxWidth())
            }
            if (state.stage == JobStage.Extracting) {
                item {
                    LoadingCard(text = "正在保持全部重复格式并扩展页面源码媒体链接")
                }
            }
            if (state.stage == JobStage.Downloading) {
                item {
                    DownloadProgressCard(state = state)
                }
            }
            state.videoInfo?.let { info ->
                item {
                    GlassCard(modifier = Modifier.widthIn(max = 860.dp).fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PosterTile(title = info.title)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                AkText(
                                    text = info.title,
                                    style = AkTheme.type.section,
                                    color = colors.textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(6.dp))
                                AkText(
                                    text = "${info.formatCount} 个格式条目 / ${info.extractor.ifBlank { "yt-dlp" }}",
                                    style = AkTheme.type.body,
                                    color = colors.textSecondary,
                                )
                            }
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.widthIn(max = 860.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AkText(text = "格式列表", style = AkTheme.type.section, color = colors.textPrimary)
                        AkText(text = "保留重复项", style = AkTheme.type.label, color = colors.textMuted)
                    }
                }
                itemsIndexed(formats, key = { index, item -> "${item.uid}-$index" }) { index, item ->
                    FormatCard(
                        index = index,
                        format = item,
                        selected = item.uid == state.selectedFormatUid,
                        onSelect = { actions.selectFormat(item.uid) },
                        onDownload = { actions.download(item) },
                        enabled = state.stage != JobStage.Downloading && state.stage != JobStage.Extracting,
                        modifier = Modifier
                            .widthIn(max = 860.dp)
                            .fillMaxWidth()
                            .animateItem(),
                    )
                }
            }
            if (state.downloadedFiles.isNotEmpty()) {
                item {
                    GlassCard(modifier = Modifier.widthIn(max = 860.dp).fillMaxWidth(), radius = 24.dp) {
                        AkText(text = "已保存文件", style = AkTheme.type.section, color = colors.success)
                        Spacer(Modifier.height(10.dp))
                        state.downloadedFiles.forEach { file ->
                            AkText(text = file, style = AkTheme.type.mono, color = colors.textSecondary)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderBlock(eyebrow: String, title: String, subtitle: String) {
    val colors = AkTheme.colors
    Column(Modifier.widthIn(max = 860.dp).fillMaxWidth()) {
        Pill(text = eyebrow, selected = true)
        Spacer(Modifier.height(14.dp))
        AkText(text = title, style = AkTheme.type.hero, color = colors.textPrimary)
        Spacer(Modifier.height(10.dp))
        AkText(text = subtitle, style = AkTheme.type.body, color = colors.textSecondary)
    }
}

@Composable
private fun EnvironmentRow(state: AppUiState, actions: AppActions) {
    val colors = AkTheme.colors
    GlassCard(modifier = Modifier.widthIn(max = 860.dp).fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MiniStatus(
                glyph = Glyph.Cookie,
                label = "Cookie",
                value = if (state.cookiePath != null) "已保存" else "未保存",
                active = state.cookiePath != null,
                modifier = Modifier.weight(1f),
            )
            MiniStatus(
                glyph = Glyph.Folder,
                label = "目录",
                value = state.settings.downloadTreeLabel
                    ?: if (state.hasAllFilesAccess) "Akatcha" else "待授权",
                active = state.settings.downloadTreeUri != null || state.hasAllFilesAccess,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ActionButton(
                label = "去登录页",
                glyph = Glyph.Browser,
                secondary = true,
                onClick = { actions.openTab(AppTab.Browser) },
                modifier = Modifier.weight(1f),
            )
            ActionButton(
                label = if (state.hasAllFilesAccess) "目录就绪" else "授权目录",
                glyph = Glyph.Folder,
                secondary = state.hasAllFilesAccess,
                onClick = if (state.hasAllFilesAccess) {
                    {}
                } else {
                    actions.requestStorageAccess
                },
                modifier = Modifier.weight(1f),
            )
        }
        if (!state.hasAllFilesAccess && state.settings.downloadTreeUri == null) {
            Spacer(Modifier.height(10.dp))
            AkText(
                text = "默认路径 ${state.defaultDirectory} 需要所有文件访问权限；也可以在设置页选择 SAF 目录。",
                style = AkTheme.type.tiny,
                color = colors.textMuted,
            )
        }
    }
}

@Composable
private fun MiniStatus(
    glyph: Glyph,
    label: String,
    value: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AkTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceSoft)
            .border(1.dp, colors.border, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (active) colors.accent.copy(alpha = 0.15f) else colors.surfaceStrong),
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(glyph = glyph, color = if (active) colors.accent else colors.textMuted, size = 19.dp)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            AkText(text = label, style = AkTheme.type.tiny, color = colors.textMuted)
            AkText(text = value, style = AkTheme.type.label, color = colors.textPrimary, maxLines = 1)
        }
    }
}

@Composable
private fun LoadingCard(text: String) {
    val colors = AkTheme.colors
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "loader")
    val pulse by transition.animateFloat(
        0.55f,
        1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing),
            androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    GlassCard(modifier = Modifier.widthIn(max = 860.dp).fillMaxWidth(), radius = 26.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.13f * pulse))
                    .border(1.dp, colors.accent.copy(alpha = pulse), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                GlyphIcon(glyph = Glyph.Search, color = colors.accent, size = 20.dp)
            }
            Spacer(Modifier.width(14.dp))
            AkText(text = text, style = AkTheme.type.bodyStrong, color = colors.textPrimary)
        }
    }
}

@Composable
private fun DownloadProgressCard(state: AppUiState) {
    val colors = AkTheme.colors
    val progress = (state.progress.percent / 100.0).toFloat().coerceIn(0f, 1f)
    GlassCard(modifier = Modifier.widthIn(max = 860.dp).fillMaxWidth(), radius = 26.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphIcon(glyph = Glyph.Download, color = colors.accent, size = 22.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                AkText(
                    text = "下载中 ${state.progress.percent.toInt()}%",
                    style = AkTheme.type.bodyStrong,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(8.dp))
                ProgressStrip(progress = progress)
            }
        }
    }
}

@Composable
private fun PosterTile(title: String) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(AkTheme.colors.accentBrush)
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        AkText(
            text = title.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
            style = AkTheme.type.title,
            color = Color.White,
        )
    }
}

@Composable
private fun FormatCard(
    index: Int,
    format: MediaFormat,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AkTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) colors.surfaceStrong else colors.surface)
            .border(
                1.dp,
                if (selected) colors.accent.copy(alpha = 0.78f) else colors.border,
                RoundedCornerShape(24.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.accent.copy(alpha = 0.18f)),
                onClick = onSelect,
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) colors.accent.copy(alpha = 0.16f) else colors.surfaceSoft),
                contentAlignment = Alignment.Center,
            ) {
                AkText(
                    text = "#${index + 1}",
                    style = AkTheme.type.label,
                    color = if (selected) colors.accent else colors.textMuted,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                AkText(
                    text = "${format.displayResolution} · ${format.extension ?: "media"}",
                    style = AkTheme.type.section,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                AkText(
                    text = format.codecLine,
                    style = AkTheme.type.body,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            IconButton(
                glyph = Glyph.Download,
                contentDescription = "下载格式",
                onClick = onDownload,
                enabled = enabled,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Pill(text = format.source, selected = format.source != "yt-dlp")
            Pill(text = format.sizeLine)
            Pill(text = format.protocol ?: "unknown")
        }
        Spacer(Modifier.height(10.dp))
        AkText(
            text = "format_id: ${format.formatId}",
            style = AkTheme.type.mono,
            color = colors.textMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        format.note?.let {
            Spacer(Modifier.height(5.dp))
            AkText(text = it, style = AkTheme.type.tiny, color = colors.textMuted, maxLines = 2)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BrowserScreen(state: AppUiState, actions: AppActions) {
    val colors = AkTheme.colors
    var webView by remember { mutableStateOf<WebView?>(null) }
    var title by remember { mutableStateOf("小红书") }
    var canGoBack by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        GlassCard(radius = 28.dp, padding = 14.dp, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    AkText(text = "PC UA 登录页", style = AkTheme.type.section, color = colors.textPrimary)
                    AkText(
                        text = title,
                        style = AkTheme.type.tiny,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(glyph = Glyph.Back, contentDescription = "后退", enabled = canGoBack, onClick = { webView?.goBack() })
                Spacer(Modifier.width(8.dp))
                IconButton(glyph = Glyph.Refresh, contentDescription = "刷新", onClick = { webView?.reload() })
                Spacer(Modifier.width(8.dp))
                ActionButton(label = "保存 Cookie", glyph = Glyph.Save, onClick = actions.saveCookies)
            }
            Spacer(Modifier.height(12.dp))
            ProgressStrip(progress = if (loading) 0.56f else 1f)
        }
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(colors.surfaceStrong)
                .border(1.dp, colors.border, RoundedCornerShape(28.dp)),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = false
                        settings.userAgentString = CookieExporter.DESKTOP_USER_AGENT
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            settings.safeBrowsingEnabled = true
                        }
                        CookieManager.getInstance().setAcceptCookie(true)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onReceivedTitle(view: WebView?, newTitle: String?) {
                                if (!newTitle.isNullOrBlank()) title = newTitle
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                loading = true
                                canGoBack = view?.canGoBack() == true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                loading = false
                                canGoBack = view?.canGoBack() == true
                                CookieManager.getInstance().flush()
                            }
                        }
                        loadUrl(CookieExporter.XIAOHONGSHU_URL)
                        webView = this
                    }
                },
                update = {
                    webView = it
                    canGoBack = it.canGoBack()
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        AnimatedStatus(
            visible = true,
            text = state.message ?: "登录后点右上角“保存 Cookie”，会生成 yt-dlp 可用的 Netscape cookie 文件。",
            modifier = Modifier.fillMaxWidth(),
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
        }
    }
}

@Composable
private fun SettingsScreen(state: AppUiState, actions: AppActions) {
    val colors = AkTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HeaderBlock(
            eyebrow = "SETTINGS",
            title = "记忆 Cookie、目录与显示风格",
            subtitle = "默认保存到用户目录 Akatcha；未授权时可用系统目录选择器指定位置。",
        )
        GlassCard(modifier = Modifier.widthIn(max = 860.dp).fillMaxWidth()) {
            AkText(text = "外观", style = AkTheme.type.section, color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))
            SegmentedTheme(mode = state.settings.themeMode, onChange = actions.setThemeMode)
        }
        GlassCard(modifier = Modifier.widthIn(max = 860.dp).fillMaxWidth()) {
            AkText(text = "下载目录", style = AkTheme.type.section, color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            AkText(
                text = state.settings.downloadTreeLabel ?: state.defaultDirectory,
                style = AkTheme.type.mono,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ActionButton(
                    label = "选择目录",
                    glyph = Glyph.Folder,
                    onClick = actions.chooseDirectory,
                    modifier = Modifier.weight(1f),
                )
                ActionButton(
                    label = "默认 Akatcha",
                    glyph = Glyph.Check,
                    secondary = true,
                    onClick = actions.clearDirectory,
                    modifier = Modifier.weight(1f),
                )
            }
            if (!state.hasAllFilesAccess && state.settings.downloadTreeUri == null) {
                Spacer(Modifier.height(10.dp))
                ActionButton(
                    label = "授予所有文件访问",
                    glyph = Glyph.Lock,
                    secondary = true,
                    onClick = actions.requestStorageAccess,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        GlassCard(modifier = Modifier.widthIn(max = 860.dp).fillMaxWidth()) {
            AkText(text = "Cookie", style = AkTheme.type.section, color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            AkText(
                text = state.cookiePath ?: "尚未保存 Cookie",
                style = AkTheme.type.mono,
                color = colors.textSecondary,
            )
            state.cookieUpdatedAt?.let {
                Spacer(Modifier.height(6.dp))
                AkText(text = "更新时间 ${formatTime(it)}", style = AkTheme.type.tiny, color = colors.textMuted)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ActionButton(
                    label = "打开登录页",
                    glyph = Glyph.Browser,
                    onClick = { actions.openTab(AppTab.Browser) },
                    modifier = Modifier.weight(1f),
                )
                ActionButton(
                    label = "清空 Cookie",
                    glyph = Glyph.Close,
                    secondary = true,
                    onClick = actions.clearCookies,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        GlassCard(modifier = Modifier.widthIn(max = 860.dp).fillMaxWidth()) {
            AkText(text = "抓取策略", style = AkTheme.type.section, color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            AkText(
                text = "yt-dlp: 2026.06.09\n重复格式: 保留\n页面源码探测: mp4 / m3u8 / mpd\nUser-Agent: ${CookieExporter.DESKTOP_USER_AGENT}",
                style = AkTheme.type.mono,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun SegmentedTheme(mode: ThemeMode, onChange: (ThemeMode) -> Unit) {
    val colors = AkTheme.colors
    val items = listOf(
        ThemeMode.System to "跟随",
        ThemeMode.Light to "浅色",
        ThemeMode.Dark to "深色",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surfaceSoft)
            .border(1.dp, colors.border, RoundedCornerShape(22.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        items.forEach { (item, label) ->
            val selected = mode == item
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(if (selected) colors.surfaceStrong else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = colors.accent.copy(alpha = 0.18f)),
                        onClick = { onChange(item) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AkText(
                    text = label,
                    style = AkTheme.type.label,
                    color = if (selected) colors.accent else colors.textSecondary,
                )
            }
        }
    }
}

private fun formatTime(epochMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(epochMillis))
