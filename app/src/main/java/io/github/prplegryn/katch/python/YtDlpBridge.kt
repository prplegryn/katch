package io.github.prplegryn.katch.python

import android.content.Context
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import io.github.prplegryn.katch.data.DownloadProgress
import io.github.prplegryn.katch.data.MediaFormat
import io.github.prplegryn.katch.data.VideoInfo
import io.github.prplegryn.katch.web.CookieExporter
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

class YtDlpBridge(context: Context) {
    private val appContext = context.applicationContext

    init {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(appContext))
        }
    }

    private val module: PyObject
        get() = Python.getInstance().getModule("downloader")

    fun probe(url: String, cookiePath: String?): VideoInfo {
        val json = module.callAttr(
            "probe",
            url,
            cookiePath.orEmpty(),
            CookieExporter.DESKTOP_USER_AGENT,
        ).toString()
        return parseVideoInfo(JSONObject(json))
    }

    fun download(
        url: String,
        format: MediaFormat,
        outputDir: File,
        cookiePath: String?,
        onProgress: (DownloadProgress) -> Unit,
    ): List<File> {
        outputDir.mkdirs()
        val callback = ProgressCallback(onProgress)
        val result = module.callAttr(
            "download",
            url,
            format.formatId,
            format.url.orEmpty(),
            outputDir.absolutePath,
            cookiePath.orEmpty(),
            CookieExporter.DESKTOP_USER_AGENT,
            callback,
        ).toString()
        val files = JSONObject(result).optJSONArray("files") ?: JSONArray()
        return buildList {
            for (i in 0 until files.length()) {
                val file = File(files.getString(i))
                if (file.exists() && file.isFile) add(file)
            }
        }
    }

    private fun parseVideoInfo(root: JSONObject): VideoInfo {
        val formats = root.optJSONArray("formats") ?: JSONArray()
        val parsed = buildList {
            for (i in 0 until formats.length()) {
                val item = formats.getJSONObject(i)
                val id = item.optString("format_id", "format-$i")
                val url = item.optString("url").takeIf { it.isNotBlank() }
                add(
                    MediaFormat(
                        uid = "$i:$id:${url.hashCode()}",
                        formatId = id,
                        source = item.optString("akatcha_source", "yt-dlp"),
                        extension = item.optNullableString("ext"),
                        resolution = item.optNullableString("resolution"),
                        width = item.optNullableInt("width"),
                        height = item.optNullableInt("height"),
                        fps = item.optNullableDouble("fps"),
                        videoCodec = item.optNullableString("vcodec"),
                        audioCodec = item.optNullableString("acodec"),
                        dynamicRange = item.optNullableString("dynamic_range"),
                        filesizeBytes = item.optNullableLong("filesize")
                            ?: item.optNullableLong("filesize_approx"),
                        bitrateKbps = item.optNullableDouble("tbr"),
                        protocol = item.optNullableString("protocol"),
                        note = item.optNullableString("format_note")
                            ?: item.optNullableString("format"),
                        url = url,
                    ),
                )
            }
        }
        return VideoInfo(
            title = root.optString("title", "小红书内容"),
            webpageUrl = root.optString("webpage_url", ""),
            extractor = root.optString("extractor", ""),
            thumbnailUrl = root.optNullableString("thumbnail"),
            durationSeconds = root.optNullableDouble("duration"),
            formatCount = parsed.size,
            formats = parsed,
        )
    }

    class ProgressCallback(private val onProgress: (DownloadProgress) -> Unit) {
        @Suppress("unused")
        fun onProgress(
            percent: Double,
            downloadedBytes: Long,
            totalBytes: Long,
            speedBytesPerSecond: Double,
            etaSeconds: Double,
            label: String,
        ) {
            onProgress(
                DownloadProgress(
                    percent = percent.coerceIn(0.0, 100.0),
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes.takeIf { it > 0L },
                    speedBytesPerSecond = speedBytesPerSecond.takeIf { it > 0.0 },
                    etaSeconds = etaSeconds.takeIf { it > 0.0 },
                    label = label,
                ),
            )
        }
    }

    private fun JSONObject.optNullableString(name: String): String? =
        if (has(name) && !isNull(name)) optString(name).takeIf { it.isNotBlank() } else null

    private fun JSONObject.optNullableInt(name: String): Int? =
        if (has(name) && !isNull(name)) optInt(name) else null

    private fun JSONObject.optNullableLong(name: String): Long? =
        if (has(name) && !isNull(name)) optLong(name) else null

    private fun JSONObject.optNullableDouble(name: String): Double? =
        if (has(name) && !isNull(name)) optDouble(name) else null
}
