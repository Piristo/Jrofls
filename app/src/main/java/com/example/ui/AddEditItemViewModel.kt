package com.example.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditUiState(
  val isEditing: Boolean = false,
  val isLoaded: Boolean = false,
  val name: String = "",
  val location: String = "",
  val note: String = "",
  val existingPhotoPath: String? = null,
  val pendingPhotoUri: Uri? = null,
  val removeExistingPhoto: Boolean = false,
  val nameError: Boolean = false,
  val locationError: Boolean = false,
  val isSaving: Boolean = false,
  val allRecentLocations: List<String> = emptyList(),
) {
  val activePhotoModel: Any?
    get() =
      when {
        pendingPhotoUri != null -> pendingPhotoUri
        !removeExistingPhoto && !existingPhotoPath.isNullOrBlank() -> existingPhotoPath
        else -> null
      }

  val filteredLocationSuggestions: List<String>
    get() {
      val currentNorm = ItemRepository.normalizeForSearch(location)
      return allRecentLocations
        .filter { suggestion ->
          val sugNorm = ItemRepository.normalizeForSearch(suggestion)
          sugNorm != currentNorm &&
            (currentNorm.isEmpty() || sugNorm.contains(currentNorm))
        }
        .take(8)
    }
}

class AddEditItemViewModel(
  private val repository: ItemRepository,
  private val itemId: Long?,
  private val prefilledLocationId: Long? = null,
) : ViewModel() {

  private val _uiState =
    MutableStateFlow(
      AddEditUiState(
        isEditing = itemId != null,
        isLoaded = itemId == null && prefilledLocationId == null,
      )
    )
  val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      repository.recentLocations.collect { locations ->
        _uiState.update { it.copy(allRecentLocations = locations) }
      }
    }
    if (itemId != null) {
      viewModelScope.launch {
        val item = repository.getItem(itemId)
        _uiState.update { state ->
          if (item != null) {
            state.copy(
              isLoaded = true,
              name = item.name,
              location = item.location,
              note = item.note,
              existingPhotoPath = item.photoPath,
            )
          } else {
            state.copy(isLoaded = true)
          }
        }
      }
    } else if (prefilledLocationId != null) {
      viewModelScope.launch {
        val locationEntity = repository.getLocation(prefilledLocationId)
        _uiState.update { state ->
          state.copy(
            isLoaded = true,
            location = locationEntity?.name ?: state.location,
          )
        }
      }
    }
  }

  fun onNameChange(value: String) {
    _uiState.update { it.copy(name = value, nameError = false) }
  }

  fun onLocationChange(value: String) {
    _uiState.update { it.copy(location = value, locationError = false) }
  }

  fun onLocationSuggestionClick(suggestion: String) {
    _uiState.update { it.copy(location = suggestion, locationError = false) }
  }

  fun onNoteChange(value: String) {
    _uiState.update { it.copy(note = value) }
  }

  fun onPhotoSelected(uri: Uri) {
    _uiState.update { it.copy(pendingPhotoUri = uri, removeExistingPhoto = false) }
  }

  fun onRemovePhoto() {
    _uiState.update { it.copy(pendingPhotoUri = null, removeExistingPhoto = true) }
  }

  fun save(onSaved: () -> Unit) {
    val current = _uiState.value
    if (current.isSaving) return

    val nameMissing = current.name.trim().isEmpty()
    val locationMissing = current.location.trim().isEmpty()
    if (nameMissing || locationMissing) {
      _uiState.update { it.copy(nameError = nameMissing, locationError = locationMissing) }
      return
    }

    _uiState.update { it.copy(isSaving = true) }
    viewModelScope.launch {
      if (itemId == null) {
        repository.addItem(
          name = current.name,
          location = current.location,
          note = current.note,
          photoUri = current.pendingPhotoUri,
          preferredLocationId = prefilledLocationId,
        )
      } else {
        repository.updateItem(
          id = itemId,
          name = current.name,
          location = current.location,
          note = current.note,
          newPhotoUri = current.pendingPhotoUri,
          removeExistingPhoto = current.removeExistingPhoto,
        )
      }
      _uiState.update { it.copy(isSaving = false) }
      onSaved()
    }
  }

  class Factory(
    private val repository: ItemRepository,
    private val itemId: Long?,
    private val prefilledLocationId: Long? = null,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return AddEditItemViewModel(repository, itemId, prefilledLocationId) as T
    }
  }
}
