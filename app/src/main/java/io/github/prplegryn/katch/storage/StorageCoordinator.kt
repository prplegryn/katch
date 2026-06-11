package io.github.prplegryn.katch.storage

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StorageCoordinator(private val context: Context) {
    val defaultDirectory: File
        get() = File(Environment.getExternalStorageDirectory(), DEFAULT_FOLDER)

    fun hasAllFilesAccess(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()

    fun ensureDefaultDirectory(): Boolean {
        return if (hasAllFilesAccess()) {
            defaultDirectory.mkdirs() || defaultDirectory.isDirectory
        } else {
            false
        }
    }

    fun newTempJobDirectory(): File {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val dir = File(context.cacheDir, "downloads/job-$stamp-${System.nanoTime()}")
        dir.mkdirs()
        return dir
    }

    fun publishFiles(files: List<File>, treeUri: Uri?): List<String> {
        if (files.isEmpty()) return emptyList()
        return if (treeUri != null) {
            publishToTree(files, treeUri)
        } else {
            publishToDefault(files)
        }
    }

    fun treeLabel(uri: Uri): String = uri.lastPathSegment
        ?.substringAfter(':')
        ?.takeIf { it.isNotBlank() }
        ?: "已选择目录"

    private fun publishToDefault(files: List<File>): List<String> {
        if (!ensureDefaultDirectory()) {
            error("需要授予所有文件访问权限，才能写入 ${defaultDirectory.absolutePath}")
        }
        return files.map { source ->
            val target = nextAvailableFile(defaultDirectory, source.name)
            source.copyTo(target, overwrite = false)
            target.absolutePath
        }
    }

    private fun publishToTree(files: List<File>, treeUri: Uri): List<String> {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: error("无法打开已选择的下载目录")
        return files.map { source ->
            val name = source.name.ifBlank { "akatcha-download-${System.currentTimeMillis()}" }
            val mime = mimeFromName(name)
            val target = createUniqueDocument(root, mime, name)
                ?: error("无法在目标目录创建 $name")
            context.contentResolver.openOutputStream(target.uri, "w").use { output ->
                requireNotNull(output) { "目标目录不可写" }
                source.inputStream().use { input -> input.copyTo(output) }
            }
            target.uri.toString()
        }
    }

    private fun createUniqueDocument(root: DocumentFile, mime: String, name: String): DocumentFile? {
        val dot = name.lastIndexOf('.')
        val stem = if (dot > 0) name.substring(0, dot) else name
        val extension = if (dot > 0) name.substring(dot) else ""
        var candidate = name
        var index = 1
        while (root.findFile(candidate) != null) {
            candidate = "$stem ($index)$extension"
            index += 1
        }
        return root.createFile(mime, candidate)
    }

    private fun nextAvailableFile(dir: File, name: String): File {
        val dot = name.lastIndexOf('.')
        val stem = if (dot > 0) name.substring(0, dot) else name
        val extension = if (dot > 0) name.substring(dot) else ""
        var candidate = File(dir, name)
        var index = 1
        while (candidate.exists()) {
            candidate = File(dir, "$stem ($index)$extension")
            index += 1
        }
        return candidate
    }

    private fun mimeFromName(name: String): String {
        return when (name.substringAfterLast('.', "").lowercase(Locale.US)) {
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "m4a" -> "audio/mp4"
            "mp3" -> "audio/mpeg"
            "json" -> "application/json"
            else -> "application/octet-stream"
        }
    }

    companion object {
        const val DEFAULT_FOLDER = "Akatcha"
    }
}
