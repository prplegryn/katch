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
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        AppHeader(state = state)
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
        BottomNavigation(selected = state.tab, onSelect = actions.openTab)
    }
}

private fun tabTransform(initial: AppTab, target: AppTab): ContentTransform {
    val direction = if (target.ordinal > initial.ordinal) 1 else -1
    return (
        slideInHorizontally(
            animationSpec = tween(220, easing = FastOutSlowInEasing),
            initialOffsetX = { it / 5 * direction },
        ) + fadeIn(tween(160))
        ).togetherWith(
        slideOutHorizontally(
            animationSpec = tween(180, easing = FastOutSlowInEasing),
            targetOffsetX = { -it / 7 * direction },
        ) + fadeOut(tween(140)),
    )
}

@Composable
private fun AppHeader(state: AppUiState) {
    val colors = AkTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.nav)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                AkText(text = "Akatcha", style = AkTheme.type.appTitle, color = colors.textPrimary)
                AkText(
                    text = screenLabel(state.tab),
                    style = AkTheme.type.tiny,
                    color = colors.textMuted,
                    maxLines = 1,
                )
            }
            StatusBadge(
                text = stageLabel(state.stage),
                active = state.stage == JobStage.Ready || state.stage == JobStage.Done,
                warning = state.stage == JobStage.Extracting || state.stage == JobStage.Downloading,
            )
        }
        DividerLine()
    }
}

private fun screenLabel(tab: AppTab): String = when (tab) {
    AppTab.Home -> "下载任务"
    AppTab.Browser -> "登录与 Cookie"
    AppTab.Settings -> "设置"
}

private fun stageLabel(stage: JobStage): String = when (stage) {
    JobStage.Idle -> "待处理"
    JobStage.Extracting -> "解析中"
    JobStage.Ready -> "可下载"
    JobStage.Downloading -> "下载中"
    JobStage.Done -> "已完成"
    JobStage.Error -> "需处理"
}

@Composable
private fun BottomNavigation(selected: AppTab, onSelect: (AppTab) -> Unit) {
    val colors = AkTheme.colors
    val tabs = listOf(
        Triple(AppTab.Home, Glyph.Home, "任务"),
        Triple(AppTab.Browser, Glyph.Browser, "登录"),
        Triple(AppTab.Settings, Glyph.Settings, "设置"),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.nav)
            .navigationBarsPadding(),
    ) {
        DividerLine()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tabs.forEach { (tab, glyph, label) ->
                val active = selected == tab
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) colors.selected else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = LocalIndication.current,
                            onClick = { onSelect(tab) },
                        ),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlyphIcon(glyph = glyph, color = if (active) colors.accent else colors.textMuted, size = 20.dp)
                    Spacer(Modifier.width(8.dp))
                    AkText(
                        text = label,
                        style = AkTheme.type.label,
                        color = if (active) colors.accent else colors.textSecondary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeScreen(state: AppUiState, actions: AppActions) {
    val formats = state.videoInfo?.formats.orEmpty()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pagePadding = if (maxWidth > 720.dp) AkTheme.spacing.pageLarge else AkTheme.spacing.page
        val wide = maxWidth > 780.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = pagePadding,
                end = pagePadding,
                top = 18.dp,
                bottom = 18.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                PageHeading(
                    title = "下载任务",
                    meta = environmentSummary(state),
                    modifier = Modifier.widthIn(max = 940.dp).fillMaxWidth(),
                )
            }
            item {
                if (wide) {
                    Row(
                        modifier = Modifier.widthIn(max = 940.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        InputPanel(
                            state = state,
                            actions = actions,
                            modifier = Modifier.weight(1.35f),
                        )
                        EnvironmentPanel(
                            state = state,
                            actions = actions,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.widthIn(max = 940.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        InputPanel(state = state, actions = actions)
                        EnvironmentPanel(state = state, actions = actions)
                    }
                }
            }
            item {
                AnimatedStatus(
                    visible = true,
                    text = state.message,
                    modifier = Modifier.widthIn(max = 940.dp).fillMaxWidth(),
                )
            }
            if (state.stage == JobStage.Extracting) {
                item {
                    ProcessPanel(
                        title = "解析队列",
                        body = "yt-dlp / 页面源码 / Manifest",
                        progress = 0.48f,
                        modifier = Modifier.widthIn(max = 940.dp).fillMaxWidth(),
                    )
                }
            }
            if (state.stage == JobStage.Downloading) {
                item {
                    DownloadProgressPanel(
                        state = state,
                        modifier = Modifier.widthIn(max = 940.dp).fillMaxWidth(),
                    )
                }
            }
            state.videoInfo?.let { info ->
                item {
                    ResultSummary(
                        title = info.title,
                        extractor = info.extractor,
                        count = info.formatCount,
                        durationSeconds = info.durationSeconds,
                        modifier = Modifier.widthIn(max = 940.dp).fillMaxWidth(),
                    )
                }
                item {
                    SectionTitle(
                        title = "格式清单",
                        meta = "${formats.size} 条，按原始顺序",
                        modifier = Modifier.widthIn(max = 940.dp).fillMaxWidth(),
                    )
                }
                itemsIndexed(formats, key = { index, item -> "${item.uid}-$index" }) { index, item ->
                    FormatRow(
                        index = index,
                        format = item,
                        selected = item.uid == state.selectedFormatUid,
                        onSelect = { actions.selectFormat(item.uid) },
                        onDownload = { actions.download(item) },
                        enabled = state.stage != JobStage.Downloading && state.stage != JobStage.Extracting,
                        modifier = Modifier
                            .widthIn(max = 940.dp)
                            .fillMaxWidth()
                            .animateItem(),
                    )
                }
            } ?: item {
                EmptyResultPanel(modifier = Modifier.widthIn(max = 940.dp).fillMaxWidth())
            }
            if (state.downloadedFiles.isNotEmpty()) {
                item {
                    SavedFilesPanel(
                        files = state.downloadedFiles,
                        modifier = Modifier.widthIn(max = 940.dp).fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PageHeading(title: String, meta: String, modifier: Modifier = Modifier) {
    val colors = AkTheme.colors
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        AkText(text = title, style = AkTheme.type.title, color = colors.textPrimary)
        Spacer(Modifier.weight(1f))
        AkText(text = meta, style = AkTheme.type.label, color = colors.textMuted, maxLines = 1)
    }
}

@Composable
private fun InputPanel(state: AppUiState, actions: AppActions, modifier: Modifier = Modifier) {
    val colors = AkTheme.colors
    val clipboard = LocalClipboardManager.current
    WorkPanel(modifier = modifier) {
        SectionTitle(title = "输入来源", meta = state.extractedUrl?.let { "已识别链接" })
        Spacer(Modifier.height(12.dp))
        AkTextField(
            value = state.inputText,
            onValueChange = actions.updateInput,
            label = "分享文本",
            placeholder = "粘贴小红书分享文本或 xhslink 链接",
            minLines = 5,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ActionButton(
                label = "粘贴",
                glyph = Glyph.Paste,
                tone = ActionTone.Secondary,
                onClick = {
                    clipboard.getText()?.text?.let(actions.updateInput)
                },
                modifier = Modifier.weight(0.9f),
            )
            ActionButton(
                label = if (state.stage == JobStage.Extracting) "解析中" else "解析格式",
                glyph = Glyph.Search,
                enabled = state.stage != JobStage.Extracting && state.stage != JobStage.Downloading,
                onClick = actions.analyze,
                modifier = Modifier.weight(1.1f),
            )
        }
        state.extractedUrl?.let { url ->
            Spacer(Modifier.height(12.dp))
            DividerLine()
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Top) {
                GlyphIcon(glyph = Glyph.Link, color = colors.accent, size = 18.dp)
                Spacer(Modifier.width(9.dp))
                AkText(
                    text = url,
                    style = AkTheme.type.mono,
                    color = colors.textSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EnvironmentPanel(state: AppUiState, actions: AppActions, modifier: Modifier = Modifier) {
    WorkPanel(modifier = modifier) {
        SectionTitle(title = "环境状态")
        Spacer(Modifier.height(10.dp))
        EnvironmentLine(
            glyph = Glyph.Cookie,
            label = "Cookie",
            value = state.cookiePath ?: "未保存",
            ok = state.cookiePath != null,
        )
        DividerLine(Modifier.padding(vertical = 10.dp))
        EnvironmentLine(
            glyph = Glyph.Folder,
            label = "输出目录",
            value = state.settings.downloadTreeLabel
                ?: if (state.hasAllFilesAccess) state.defaultDirectory else "待授权",
            ok = state.settings.downloadTreeUri != null || state.hasAllFilesAccess,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ActionButton(
                label = "登录",
                glyph = Glyph.Browser,
                tone = ActionTone.Secondary,
                onClick = { actions.openTab(AppTab.Browser) },
                modifier = Modifier.weight(1f),
            )
            ActionButton(
                label = if (state.hasAllFilesAccess) "目录就绪" else "授权",
                glyph = Glyph.Lock,
                tone = if (state.hasAllFilesAccess) ActionTone.Secondary else ActionTone.Primary,
                enabled = !state.hasAllFilesAccess,
                onClick = actions.requestStorageAccess,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EnvironmentLine(
    glyph: Glyph,
    label: String,
    value: String,
    ok: Boolean,
) {
    val colors = AkTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (ok) colors.successSoft else colors.warningSoft),
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(glyph = glyph, color = if (ok) colors.success else colors.warning, size = 19.dp)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            AkText(text = label, style = AkTheme.type.label, color = colors.textPrimary)
            AkText(
                text = value,
                style = AkTheme.type.tiny,
                color = colors.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        StatusBadge(text = if (ok) "就绪" else "待处理", active = ok, warning = !ok)
    }
}

@Composable
private fun ProcessPanel(title: String, body: String, progress: Float, modifier: Modifier = Modifier) {
    val colors = AkTheme.colors
    WorkPanel(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphIcon(glyph = Glyph.Search, color = colors.accent, size = 21.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                AkText(text = title, style = AkTheme.type.bodyStrong, color = colors.textPrimary)
                AkText(text = body, style = AkTheme.type.tiny, color = colors.textMuted)
            }
        }
        Spacer(Modifier.height(12.dp))
        ProgressStrip(progress = progress)
    }
}

@Composable
private fun DownloadProgressPanel(state: AppUiState, modifier: Modifier = Modifier) {
    val colors = AkTheme.colors
    val progress = (state.progress.percent / 100.0).toFloat().coerceIn(0f, 1f)
    WorkPanel(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphIcon(glyph = Glyph.Download, color = colors.accent, size = 21.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                AkText(
                    text = "下载中 ${state.progress.percent.toInt()}%",
                    style = AkTheme.type.bodyStrong,
                    color = colors.textPrimary,
                )
                AkText(
                    text = progressMeta(state),
                    style = AkTheme.type.tiny,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        ProgressStrip(progress = progress)
    }
}

@Composable
private fun ResultSummary(
    title: String,
    extractor: String,
    count: Int,
    durationSeconds: Double?,
    modifier: Modifier = Modifier,
) {
    val colors = AkTheme.colors
    WorkPanel(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.selected)
                    .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                GlyphIcon(glyph = Glyph.List, color = colors.accent, size = 23.dp)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                AkText(
                    text = title.ifBlank { "未命名内容" },
                    style = AkTheme.type.section,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                AkText(
                    text = listOfNotNull(
                        "$count 个格式",
                        extractor.ifBlank { "yt-dlp" },
                        durationSeconds?.let(::durationLabel),
                    ).joinToString(" / "),
                    style = AkTheme.type.body,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EmptyResultPanel(modifier: Modifier = Modifier) {
    val colors = AkTheme.colors
    WorkPanel(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphIcon(glyph = Glyph.List, color = colors.textMuted, size = 20.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                AkText(text = "解析结果", style = AkTheme.type.bodyStrong, color = colors.textPrimary)
                AkText(text = "暂无格式条目", style = AkTheme.type.tiny, color = colors.textMuted)
            }
        }
    }
}

@Composable
private fun FormatRow(
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
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.selected else colors.surface)
            .border(1.dp, if (selected) colors.accent else colors.border, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onSelect,
            )
            .padding(13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) colors.surfaceRaised else colors.surfaceMuted),
                contentAlignment = Alignment.Center,
            ) {
                AkText(
                    text = (index + 1).toString().padStart(2, '0'),
                    style = AkTheme.type.label,
                    color = if (selected) colors.accent else colors.textMuted,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                AkText(
                    text = "${format.displayResolution} · ${format.extension ?: "media"}",
                    style = AkTheme.type.bodyStrong,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AkText(
                    text = format.codecLine,
                    style = AkTheme.type.body,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            IconButton(
                glyph = Glyph.Download,
                contentDescription = "下载格式",
                onClick = onDownload,
                enabled = enabled,
                selected = selected,
            )
        }
        Spacer(Modifier.height(10.dp))
        DividerLine()
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            FormatMeta(label = "来源", value = format.source, modifier = Modifier.weight(1f))
            FormatMeta(label = "大小", value = format.sizeLine, modifier = Modifier.weight(1f))
            FormatMeta(label = "协议", value = format.protocol ?: "unknown", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        AkText(
            text = "format_id: ${format.formatId}",
            style = AkTheme.type.mono,
            color = colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        format.note?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(4.dp))
            AkText(text = it, style = AkTheme.type.tiny, color = colors.textMuted, maxLines = 2)
        }
    }
}

@Composable
private fun FormatMeta(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = AkTheme.colors
    Column(modifier = modifier) {
        AkText(text = label, style = AkTheme.type.tiny, color = colors.textMuted, maxLines = 1)
        AkText(
            text = value,
            style = AkTheme.type.label,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SavedFilesPanel(files: List<String>, modifier: Modifier = Modifier) {
    val colors = AkTheme.colors
    WorkPanel(modifier = modifier) {
        SectionTitle(title = "输出记录", meta = "${files.size} 个文件")
        Spacer(Modifier.height(10.dp))
        files.forEachIndexed { index, file ->
            if (index > 0) {
                Spacer(Modifier.height(8.dp))
                DividerLine()
                Spacer(Modifier.height(8.dp))
            }
            AkText(
                text = file,
                style = AkTheme.type.mono,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
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
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(glyph = Glyph.Back, contentDescription = "后退", enabled = canGoBack, onClick = { webView?.goBack() })
                Spacer(Modifier.width(8.dp))
                IconButton(glyph = Glyph.Refresh, contentDescription = "刷新", onClick = { webView?.reload() })
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    AkText(text = "PC UA 登录", style = AkTheme.type.label, color = colors.textPrimary)
                    AkText(
                        text = title,
                        style = AkTheme.type.tiny,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(10.dp))
                ActionButton(label = "保存 Cookie", glyph = Glyph.Save, onClick = actions.saveCookies)
            }
            ProgressStrip(progress = if (loading) 0.56f else 1f)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(colors.surfaceRaised)
                .border(1.dp, colors.border),
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
        AnimatedStatus(
            visible = true,
            text = state.message ?: "Cookie 状态：${if (state.cookiePath != null) "已保存" else "未保存"}",
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PageHeading(
            title = "设置",
            meta = "本机配置",
            modifier = Modifier.widthIn(max = 940.dp).fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        WorkPanel(modifier = Modifier.widthIn(max = 940.dp).fillMaxWidth(), padding = 0.dp) {
            SettingsBlock(title = "外观") {
                SegmentedTheme(mode = state.settings.themeMode, onChange = actions.setThemeMode)
            }
            DividerLine()
            SettingsBlock(title = "输出目录") {
                AkText(
                    text = state.settings.downloadTreeLabel ?: state.defaultDirectory,
                    style = AkTheme.type.mono,
                    color = colors.textSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
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
                        label = "默认目录",
                        glyph = Glyph.Check,
                        tone = ActionTone.Secondary,
                        onClick = actions.clearDirectory,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (!state.hasAllFilesAccess && state.settings.downloadTreeUri == null) {
                    Spacer(Modifier.height(10.dp))
                    ActionButton(
                        label = "授予所有文件访问",
                        glyph = Glyph.Lock,
                        tone = ActionTone.Secondary,
                        onClick = actions.requestStorageAccess,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            DividerLine()
            SettingsBlock(title = "Cookie") {
                AkText(
                    text = state.cookiePath ?: "未保存",
                    style = AkTheme.type.mono,
                    color = colors.textSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                state.cookieUpdatedAt?.let {
                    Spacer(Modifier.height(6.dp))
                    AkText(text = "更新时间 ${formatTime(it)}", style = AkTheme.type.tiny, color = colors.textMuted)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionButton(
                        label = "打开登录",
                        glyph = Glyph.Browser,
                        onClick = { actions.openTab(AppTab.Browser) },
                        modifier = Modifier.weight(1f),
                    )
                    ActionButton(
                        label = "清空 Cookie",
                        glyph = Glyph.Close,
                        tone = ActionTone.Danger,
                        onClick = actions.clearCookies,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            DividerLine()
            SettingsBlock(title = "抓取参数") {
                SettingDataLine(label = "yt-dlp", value = "2026.06.09")
                SettingDataLine(label = "重复格式", value = "保留")
                SettingDataLine(label = "页面源码", value = "mp4 / m3u8 / mpd")
                SettingDataLine(label = "User-Agent", value = CookieExporter.DESKTOP_USER_AGENT)
            }
        }
    }
}

@Composable
private fun SettingsBlock(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        SectionTitle(title = title)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun SettingDataLine(label: String, value: String) {
    val colors = AkTheme.colors
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        AkText(
            text = label,
            style = AkTheme.type.label,
            color = colors.textMuted,
            modifier = Modifier.width(86.dp),
            maxLines = 1,
        )
        AkText(
            text = value,
            style = AkTheme.type.mono,
            color = colors.textSecondary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(8.dp))
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
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceMuted)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { (item, label) ->
            val selected = mode == item
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) colors.surfaceRaised else Color.Transparent)
                    .border(
                        1.dp,
                        if (selected) colors.border else Color.Transparent,
                        RoundedCornerShape(6.dp),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        onClick = { onChange(item) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AkText(
                    text = label,
                    style = AkTheme.type.label,
                    color = if (selected) colors.accent else colors.textSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun environmentSummary(state: AppUiState): String {
    val cookie = if (state.cookiePath != null) "Cookie 就绪" else "Cookie 待处理"
    val directory = if (state.settings.downloadTreeUri != null || state.hasAllFilesAccess) "目录就绪" else "目录待授权"
    return "$cookie / $directory"
}

private fun progressMeta(state: AppUiState): String {
    val progress = state.progress
    return listOfNotNull(
        progress.label.takeIf { it.isNotBlank() },
        progress.totalBytes?.let { "${formatBytes(progress.downloadedBytes)} / ${formatBytes(it)}" },
        progress.speedBytesPerSecond?.let { "${formatBytes(it.toLong())}/s" },
        progress.etaSeconds?.let { "ETA ${it.toInt()}s" },
    ).joinToString(" · ").ifBlank { "等待下载器返回进度" }
}

private fun durationLabel(seconds: Double): String {
    val total = seconds.toInt().coerceAtLeast(0)
    val minutes = total / 60
    val remaining = total % 60
    return "%d:%02d".format(Locale.US, minutes, remaining)
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble() / 1024.0
    var index = 0
    while (value >= 1024.0 && index < units.lastIndex) {
        value /= 1024.0
        index += 1
    }
    return if (value % 1.0 == 0.0) {
        "${value.toInt()} ${units[index]}"
    } else {
        "%.1f %s".format(Locale.US, value, units[index])
    }
}

private fun formatTime(epochMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(epochMillis))
