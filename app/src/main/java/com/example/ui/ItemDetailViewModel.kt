package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ItemEntity
import com.example.data.ItemHistoryEntity
import com.example.data.ItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ItemDetailUiState {
  data object Loading : ItemDetailUiState

  data object NotFound : ItemDetailUiState

  data class Success(
    val item: ItemEntity,
    val recentLocations: List<String>,
    val history: List<ItemHistoryEntity>,
  ) : ItemDetailUiState
}

class ItemDetailViewModel(
  private val repository: ItemRepository,
  private val itemId: Long,
) : ViewModel() {

  val uiState: StateFlow<ItemDetailUiState> =
    combine(
        repository.observeItem(itemId),
        repository.recentLocations,
        repository.observeItemHistory(itemId),
      ) { item, locations, history ->
        if (item == null) {
          ItemDetailUiState.NotFound
        } else {
          ItemDetailUiState.Success(
            item = item,
            recentLocations = locations,
            history = history,
          )
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ItemDetailUiState.Loading,
      )

  fun moveItem(newLocation: String, onMoved: () -> Unit = {}) {
    val trimmed = newLocation.trim()
    if (trimmed.isEmpty()) return
    viewModelScope.launch {
      repository.moveItem(itemId, trimmed)
      onMoved()
    }
  }

  fun deleteItem(item: ItemEntity, onDeleted: () -> Unit) {
    viewModelScope.launch {
      repository.deleteItem(item)
      onDeleted()
    }
  }

  class Factory(
    private val repository: ItemRepository,
    private val itemId: Long,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return ItemDetailViewModel(repository, itemId) as T
    }
  }
}
