package com.anonymoustools.app.features.vault

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anonymoustools.app.core.model.CaptureItem
import com.anonymoustools.app.core.vault.PrivateVaultManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class VaultUiState(
    val items: List<CaptureItem> = emptyList(),
    val isLoading: Boolean = false,
    val selectedFilter: Int = 0, // 0: All, 1: Snapshots, 2: Videos
    val message: String? = null
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VaultUiState(isLoading = true))
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    init {
        loadItems()
    }

    fun loadItems() {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                PrivateVaultManager.listVaultItems(getApplication())
            }
            _uiState.update { it.copy(items = loaded, isLoading = false) }
        }
    }

    fun setFilter(filterIndex: Int) {
        _uiState.update { it.copy(selectedFilter = filterIndex) }
    }

    fun deleteItem(item: CaptureItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                PrivateVaultManager.deleteItem(item.file)
            }
            loadItems()
            _uiState.update { it.copy(message = "Deleted ${item.name}") }
        }
    }

    fun shredVault() {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                PrivateVaultManager.shredVault(getApplication())
            }
            loadItems()
            _uiState.update { it.copy(message = "Shredded $count files from vault!") }
        }
    }

    fun exportItem(item: CaptureItem) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                PrivateVaultManager.exportToPublicGallery(getApplication(), item)
            }
            _uiState.update {
                it.copy(message = if (success) "Exported ${item.name} to Gallery!" else "Export failed")
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
