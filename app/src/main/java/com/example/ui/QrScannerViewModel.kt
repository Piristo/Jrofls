package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface QrScanUiState {
  data object Scanning : QrScanUiState

  data class Resolved(val locationId: Long) : QrScanUiState

  data object NotFound : QrScanUiState
}

class QrScannerViewModel(private val repository: ItemRepository) : ViewModel() {

  private val _scanState = MutableStateFlow<QrScanUiState>(QrScanUiState.Scanning)
  val scanState: StateFlow<QrScanUiState> = _scanState.asStateFlow()

  private var isResolving = false

  fun onQrPayloadDetected(rawPayload: String) {
    if (isResolving || _scanState.value !is QrScanUiState.Scanning) return
    isResolving = true
    viewModelScope.launch {
      val location = repository.findLocationByQrPayload(rawPayload)
      _scanState.value =
        if (location != null) {
          QrScanUiState.Resolved(location.id)
        } else {
          QrScanUiState.NotFound
        }
      isResolving = false
    }
  }

  fun resetScan() {
    isResolving = false
    _scanState.value = QrScanUiState.Scanning
  }

  class Factory(private val repository: ItemRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return QrScannerViewModel(repository) as T
    }
  }
}
