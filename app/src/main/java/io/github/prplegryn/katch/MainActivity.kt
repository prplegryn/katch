package io.github.prplegryn.katch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import io.github.prplegryn.katch.ui.AkatchaApp

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val chooseTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.setDownloadTree(uri)
    }

    private val manageFiles = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refreshEnvironment()
    }

    private val writeStorage = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.refreshEnvironment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        setContent {
            AkatchaApp(
                viewModel = viewModel,
                requestStorageAccess = {
                    viewModel.markStoragePromptShown()
                    requestStorageAccess()
                },
                chooseDirectory = { chooseTree.launch(null) },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshEnvironment()
    }

    private fun requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val packageUri = Uri.parse("package:$packageName")
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, packageUri)
            runCatching { manageFiles.launch(intent) }
                .onFailure { manageFiles.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ).filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (permissions.isNotEmpty()) {
                writeStorage.launch(permissions.toTypedArray())
            } else {
                viewModel.refreshEnvironment()
            }
        } else {
            viewModel.refreshEnvironment()
        }
    }
}
