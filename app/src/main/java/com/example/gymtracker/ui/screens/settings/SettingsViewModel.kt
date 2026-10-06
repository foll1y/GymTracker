package com.example.gymtracker.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.backup.BackupManager
import com.example.gymtracker.data.health.HealthConnectManager
import com.example.gymtracker.data.online.OnlineExerciseCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val healthConnectManager: HealthConnectManager,
    private val backupManager: BackupManager,
    private val onlineCatalog: OnlineExerciseCatalog
) : ViewModel() {

    private val _isHealthConnected = MutableStateFlow(false)
    val isHealthConnected = _isHealthConnected.asStateFlow()

    private val _isImportingCatalog = MutableStateFlow(false)
    val isImportingCatalog = _isImportingCatalog.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    init {
        checkPermissions()
    }

    fun checkPermissions() {
        viewModelScope.launch {
            _isHealthConnected.value = healthConnectManager.hasAllPermissions()
        }
    }

    fun importFullCatalog() {
        viewModelScope.launch {
            _isImportingCatalog.value = true
            try {
                val count = onlineCatalog.importFullCatalog()
                _statusMessage.value = if (count > 0) {
                    "Добавлено $count новых упражнений в ваш каталог!"
                } else {
                    "Все упражнения каталога уже добавлены в базу!"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Ошибка импорта: ${e.localizedMessage}"
            } finally {
                _isImportingCatalog.value = false
            }
        }
    }

    fun exportBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = backupManager.exportBackup(context, uri)
            _statusMessage.value = if (result.isSuccess) {
                "Резервная копия сохранена! (тренировок: ${result.getOrNull()})"
            } else {
                "Ошибка экспорта: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun importBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = backupManager.importBackup(context, uri)
            _statusMessage.value = if (result.isSuccess) {
                "Успешно восстановлено тренировок: ${result.getOrNull()}!"
            } else {
                "Ошибка восстановления: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
