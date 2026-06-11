package io.github.prplegryn.katch

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.prplegryn.katch.data.AppTab
import io.github.prplegryn.katch.data.AppUiState
import io.github.prplegryn.katch.data.JobStage
import io.github.prplegryn.katch.data.MediaFormat
import io.github.prplegryn.katch.data.PreferencesRepository
import io.github.prplegryn.katch.data.ThemeMode
import io.github.prplegryn.katch.domain.LinkExtractor
import io.github.prplegryn.katch.python.YtDlpBridge
import io.github.prplegryn.katch.storage.StorageCoordinator
import io.github.prplegryn.katch.web.CookieExporter
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = PreferencesRepository(application)
    private val storage = StorageCoordinator(application)
    private val cookies = CookieExporter(application)
    private val bridge by lazy { YtDlpBridge(application) }

    private val _uiState = MutableStateFlow(
        AppUiState(
            tab = if (cookies.hasCookieFile()) AppTab.Home else AppTab.Browser,
            settings = preferences.settings.value,
            inputText = preferences.settings.value.lastInput,
            cookiePath = cookies.cookieFile.takeIf { it.exists() }?.absolutePath,
            cookieUpdatedAt = cookies.cookieFile.takeIf { it.exists() }?.lastModified(),
            defaultDirectory = storage.defaultDirectory.absolutePath,
            hasAllFilesAccess = storage.hasAllFilesAccess(),
        ),
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        if (storage.hasAllFilesAccess()) {
            storage.ensureDefaultDirectory()
        }
        viewModelScope.launch {
            preferences.settings.collect { settings ->
                _uiState.update { current ->
                    current.copy(
                        settings = settings,
                        inputText = if (current.inputText.isBlank()) settings.lastInput else current.inputText,
                    )
                }
            }
        }
        refreshEnvironment()
    }

    fun refreshEnvironment() {
        if (storage.hasAllFilesAccess()) {
            storage.ensureDefaultDirectory()
        }
        _uiState.update {
            it.copy(
                cookiePath = cookies.cookieFile.takeIf { file -> file.exists() }?.absolutePath,
                cookieUpdatedAt = cookies.cookieFile.takeIf { file -> file.exists() }?.lastModified(),
                defaultDirectory = storage.defaultDirectory.absolutePath,
                hasAllFilesAccess = storage.hasAllFilesAccess(),
            )
        }
    }

    fun markStoragePromptShown() {
        preferences.setStoragePromptShown()
    }

    fun openTab(tab: AppTab) {
        _uiState.update { it.copy(tab = tab, message = null) }
    }

    fun updateInput(text: String) {
        preferences.setLastInput(text)
        _uiState.update {
            it.copy(
                inputText = text,
                extractedUrl = LinkExtractor.extract(text),
                message = null,
            )
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        preferences.setThemeMode(mode)
    }

    fun setDownloadTree(uri: Uri) {
        getApplication<Application>().contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        preferences.setDownloadTree(uri, storage.treeLabel(uri))
        _uiState.update { it.copy(message = "下载目录已更新") }
    }

    fun clearDownloadTree() {
        preferences.clearDownloadTree()
        _uiState.update { it.copy(message = "已恢复默认目录") }
    }

    fun saveCookies() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { cookies.exportForYtDlp() }
                .onSuccess { file ->
                    _uiState.update {
                        it.copy(
                            cookiePath = file.absolutePath,
                            cookieUpdatedAt = file.lastModified(),
                            message = "Cookie 已保存，可返回解析链接",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(stage = JobStage.Error, message = error.message ?: "Cookie 保存失败") }
                }
        }
    }

    fun clearCookies() {
        viewModelScope.launch(Dispatchers.IO) {
            cookies.clearCookies()
            _uiState.update {
                it.copy(
                    cookiePath = null,
                    cookieUpdatedAt = null,
                    message = "Cookie 与 WebView 数据已清空",
                )
            }
        }
    }

    fun analyze() {
        val raw = uiState.value.inputText
        val url = LinkExtractor.extract(raw)
        if (url == null) {
            _uiState.update {
                it.copy(
                    stage = JobStage.Error,
                    message = "没有找到小红书或 xhslink 链接",
                    videoInfo = null,
                    selectedFormatUid = null,
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    stage = JobStage.Extracting,
                    extractedUrl = url,
                    message = "正在通过 yt-dlp 与页面源码探测全部格式",
                    videoInfo = null,
                    selectedFormatUid = null,
                    downloadedFiles = emptyList(),
                )
            }
            runCatching {
                bridge.probe(url, cookies.cookieFile.takeIf(File::exists)?.absolutePath)
            }.onSuccess { info ->
                _uiState.update {
                    it.copy(
                        stage = JobStage.Ready,
                        videoInfo = info,
                        selectedFormatUid = info.formats.firstOrNull()?.uid,
                        message = "找到 ${info.formatCount} 个格式条目，未去重",
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        stage = JobStage.Error,
                        message = error.message ?: "解析失败",
                        videoInfo = null,
                        selectedFormatUid = null,
                    )
                }
            }
        }
    }

    fun selectFormat(uid: String) {
        _uiState.update { it.copy(selectedFormatUid = uid, message = null) }
    }

    fun download(format: MediaFormat? = null) {
        val snapshot = uiState.value
        val targetFormat = format
            ?: snapshot.videoInfo?.formats?.firstOrNull { it.uid == snapshot.selectedFormatUid }
        val url = snapshot.extractedUrl
        if (targetFormat == null || url == null) {
            _uiState.update { it.copy(stage = JobStage.Error, message = "请先解析并选择一个格式") }
            return
        }
        if (snapshot.settings.downloadTreeUri == null && !storage.hasAllFilesAccess()) {
            _uiState.update {
                it.copy(
                    stage = JobStage.Error,
                    message = "默认目录需要所有文件访问权限；也可以在设置里选择一个下载目录",
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val tempDir = storage.newTempJobDirectory()
            _uiState.update {
                it.copy(
                    stage = JobStage.Downloading,
                    selectedFormatUid = targetFormat.uid,
                    message = "正在下载 ${targetFormat.displayResolution}",
                    downloadedFiles = emptyList(),
                )
            }
            runCatching {
                val tempFiles = bridge.download(
                    url = url,
                    format = targetFormat,
                    outputDir = tempDir,
                    cookiePath = cookies.cookieFile.takeIf(File::exists)?.absolutePath,
                    onProgress = { progress ->
                        _uiState.update { state -> state.copy(progress = progress) }
                    },
                )
                storage.publishFiles(tempFiles, snapshot.settings.downloadTreeUri)
            }.onSuccess { published ->
                tempDir.deleteRecursively()
                _uiState.update {
                    it.copy(
                        stage = JobStage.Done,
                        message = "下载完成",
                        downloadedFiles = published,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        stage = JobStage.Error,
                        message = error.message ?: "下载失败",
                    )
                }
            }
        }
    }
}
