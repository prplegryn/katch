package io.github.prplegryn.katch.data

import android.net.Uri
import java.util.Locale

enum class AppTab {
    Home,
    Browser,
    Settings,
}

enum class ThemeMode {
    System,
    Light,
    Dark,
}

enum class JobStage {
    Idle,
    Extracting,
    Ready,
    Downloading,
    Done,
    Error,
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val downloadTreeUri: Uri? = null,
    val downloadTreeLabel: String? = null,
    val lastInput: String = "",
    val storagePromptShown: Boolean = false,
)

data class VideoInfo(
    val title: String,
    val webpageUrl: String,
    val extractor: String,
    val thumbnailUrl: String?,
    val durationSeconds: Double?,
    val formatCount: Int,
    val formats: List<MediaFormat>,
)

data class MediaFormat(
    val uid: String,
    val formatId: String,
    val source: String,
    val extension: String?,
    val resolution: String?,
    val width: Int?,
    val height: Int?,
    val fps: Double?,
    val videoCodec: String?,
    val audioCodec: String?,
    val dynamicRange: String?,
    val filesizeBytes: Long?,
    val bitrateKbps: Double?,
    val protocol: String?,
    val note: String?,
    val url: String?,
) {
    val displayResolution: String
        get() = resolution
            ?: when {
                width != null && height != null -> "${width}x$height"
                height != null -> "${height}p"
                else -> "未知分辨率"
            }

    val codecLine: String
        get() = listOfNotNull(
            videoCodec?.takeUnless { it == "none" }?.let { "V $it" },
            audioCodec?.takeUnless { it == "none" }?.let { "A $it" },
            dynamicRange,
        ).joinToString("  ").ifBlank { "编码信息未公开" }

    val sizeLine: String
        get() = filesizeBytes?.let(::formatBytes)
            ?: bitrateKbps?.let { "${trimNumber(it)} kbps" }
            ?: "大小未知"

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble() / 1024.0
        var index = 0
        while (value >= 1024.0 && index < units.lastIndex) {
            value /= 1024.0
            index += 1
        }
        return "${trimNumber(value)} ${units[index]}"
    }

    private fun trimNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(Locale.US, value)
}

data class DownloadProgress(
    val percent: Double = 0.0,
    val downloadedBytes: Long = 0,
    val totalBytes: Long? = null,
    val speedBytesPerSecond: Double? = null,
    val etaSeconds: Double? = null,
    val label: String = "",
)

data class AppUiState(
    val tab: AppTab = AppTab.Home,
    val settings: AppSettings = AppSettings(),
    val inputText: String = "",
    val extractedUrl: String? = null,
    val cookiePath: String? = null,
    val cookieUpdatedAt: Long? = null,
    val defaultDirectory: String = "",
    val hasAllFilesAccess: Boolean = false,
    val stage: JobStage = JobStage.Idle,
    val message: String? = null,
    val videoInfo: VideoInfo? = null,
    val selectedFormatUid: String? = null,
    val progress: DownloadProgress = DownloadProgress(),
    val downloadedFiles: List<String> = emptyList(),
)
