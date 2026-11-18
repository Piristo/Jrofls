package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ItemEntity
import com.example.data.ItemRepository
import com.example.data.LocationEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LocationDetailUiState {
  data object Loading : LocationDetailUiState

  data object NotFound : LocationDetailUiState

  data class Success(
    val location: LocationEntity,
    val items: List<ItemEntity>,
    val recentLocations: List<String>,
  ) : LocationDetailUiState
}

class LocationDetailViewModel(
  private val repository: ItemRepository,
  private val locationId: Long,
) : ViewModel() {

  val uiState: StateFlow<LocationDetailUiState> =
    combine(
        repository.observeLocation(locationId),
        repository.observeItemsInLocation(locationId),
        repository.recentLocations,
      ) { location, items, recentLocations ->
        if (location == null) {
          LocationDetailUiState.NotFound
        } else {
          LocationDetailUiState.Success(
            location = location,
            items = items,
            recentLocations = recentLocations,
          )
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LocationDetailUiState.Loading,
      )

  fun addMultipleItems(rawMultilineText: String, onCompleted: (Int) -> Unit) {
    viewModelScope.launch {
      val addedCount = repository.addMultipleItemsToLocation(locationId, rawMultilineText)
      onCompleted(addedCount)
    }
  }

  fun renameOrMoveBox(newPath: String, onCompleted: () -> Unit = {}) {
    val trimmed = newPath.trim()
    if (trimmed.isEmpty()) return
    viewModelScope.launch {
      repository.renameOrMoveLocation(locationId, trimmed)
      onCompleted()
    }
  }

  class Factory(
    private val repository: ItemRepository,
    private val locationId: Long,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return LocationDetailViewModel(repository, locationId) as T
    }
  }
}
