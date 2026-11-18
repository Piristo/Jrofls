package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ItemEntity
import com.example.data.ItemRepository
import com.example.data.LocationEntity
import com.example.data.LocationSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HomeTab {
  ITEMS,
  LOCATIONS,
}

sealed interface ItemsTabState {
  data object Loading : ItemsTabState

  data object EmptyCatalog : ItemsTabState

  data class EmptySearchResult(val query: String) : ItemsTabState

  data class Loaded(
    val items: List<ItemEntity>,
    val totalCount: Int,
    val isSearchActive: Boolean,
  ) : ItemsTabState
}

sealed interface LocationsTabState {
  data object Loading : LocationsTabState

  data object Empty : LocationsTabState

  data class EmptySearchResult(val query: String) : LocationsTabState

  data class Loaded(
    val locations: List<LocationSummary>,
    val totalCount: Int,
    val isSearchActive: Boolean,
  ) : LocationsTabState
}

data class HomeScreenState(
  val activeTab: HomeTab = HomeTab.ITEMS,
  val itemsState: ItemsTabState = ItemsTabState.Loading,
  val locationsState: LocationsTabState = LocationsTabState.Loading,
  val allLocationSummaries: List<LocationSummary> = emptyList(),
  val isPdfSelectionMode: Boolean = false,
  val selectedLocationIds: Set<Long> = emptySet(),
) {
  fun selectedLocationsForPdf(): List<LocationEntity> =
    allLocationSummaries
      .filter { it.id in selectedLocationIds }
      .map { it.toLocationEntity() }
}

class HomeViewModel(private val repository: ItemRepository) : ViewModel() {

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _activeTab = MutableStateFlow(HomeTab.ITEMS)
  private val _isPdfSelectionMode = MutableStateFlow(false)
  private val _selectedLocationIds = MutableStateFlow<Set<Long>>(emptySet())

  private data class SelectionUiParams(
    val query: String,
    val activeTab: HomeTab,
    val isPdfSelectionMode: Boolean,
    val selectedIds: Set<Long>,
  )

  private val selectionParams =
    combine(
      _searchQuery,
      _activeTab,
      _isPdfSelectionMode,
      _selectedLocationIds,
    ) { query, tab, isPdfMode, selectedIds ->
      SelectionUiParams(
        query = query,
        activeTab = tab,
        isPdfSelectionMode = isPdfMode,
        selectedIds = selectedIds,
      )
    }

  val uiState: StateFlow<HomeScreenState> =
    combine(repository.allItems, repository.locationSummaries, selectionParams) {
        items,
        locationSummaries,
        params ->
        val trimmedQuery = params.query.trim()
        val itemsState =
          when {
            items.isEmpty() -> ItemsTabState.EmptyCatalog
            trimmedQuery.isEmpty() ->
              ItemsTabState.Loaded(
                items = items,
                totalCount = items.size,
                isSearchActive = false,
              )
            else -> {
              val filtered = items.filter { ItemRepository.matchesQuery(it, trimmedQuery) }
              if (filtered.isEmpty()) {
                ItemsTabState.EmptySearchResult(trimmedQuery)
              } else {
                ItemsTabState.Loaded(
                  items = filtered,
                  totalCount = items.size,
                  isSearchActive = true,
                )
              }
            }
          }

        val locationsState =
          when {
            locationSummaries.isEmpty() -> LocationsTabState.Empty
            trimmedQuery.isEmpty() ->
              LocationsTabState.Loaded(
                locations = locationSummaries,
                totalCount = locationSummaries.size,
                isSearchActive = false,
              )
            else -> {
              val filtered =
                locationSummaries.filter {
                  ItemRepository.matchesLocationQuery(it, trimmedQuery)
                }
              if (filtered.isEmpty()) {
                LocationsTabState.EmptySearchResult(trimmedQuery)
              } else {
                LocationsTabState.Loaded(
                  locations = filtered,
                  totalCount = locationSummaries.size,
                  isSearchActive = true,
                )
              }
            }
          }

        val existingIds = locationSummaries.mapTo(HashSet()) { it.id }
        val sanitizedSelection = params.selectedIds.intersect(existingIds)

        HomeScreenState(
          activeTab = params.activeTab,
          itemsState = itemsState,
          locationsState = locationsState,
          allLocationSummaries = locationSummaries,
          isPdfSelectionMode = params.isPdfSelectionMode && locationSummaries.isNotEmpty(),
          selectedLocationIds = sanitizedSelection,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeScreenState(),
      )

  fun onSearchQueryChange(newQuery: String) {
    _searchQuery.value = newQuery
  }

  fun clearSearch() {
    _searchQuery.value = ""
  }

  fun selectTab(tab: HomeTab) {
    _activeTab.value = tab
    if (tab != HomeTab.LOCATIONS) {
      _isPdfSelectionMode.value = false
      _selectedLocationIds.value = emptySet()
    }
  }

  fun createLocation(rawName: String, onCreated: (Long) -> Unit = {}) {
    val trimmed = rawName.trim()
    if (trimmed.isEmpty()) return
    viewModelScope.launch {
      val created = repository.createLocation(trimmed)
      if (created != null) {
        onCreated(created.id)
      }
    }
  }

  fun deleteEmptyLocation(locationId: Long) {
    viewModelScope.launch {
      repository.deleteEmptyLocation(locationId)
      _selectedLocationIds.value = _selectedLocationIds.value - locationId
    }
  }

  fun enterPdfSelectionMode(preselectIds: Set<Long> = emptySet()) {
    _activeTab.value = HomeTab.LOCATIONS
    _isPdfSelectionMode.value = true
    _selectedLocationIds.value = preselectIds
  }

  fun exitPdfSelectionMode() {
    _isPdfSelectionMode.value = false
    _selectedLocationIds.value = emptySet()
  }

  fun toggleLocationForPdf(locationId: Long) {
    val current = _selectedLocationIds.value
    _selectedLocationIds.value =
      if (locationId in current) current - locationId else current + locationId
  }

  fun selectAllVisibleLocationsForPdf(ids: List<Long>) {
    _selectedLocationIds.value = _selectedLocationIds.value + ids
  }

  fun clearLocationsForPdf() {
    _selectedLocationIds.value = emptySet()
  }

  class Factory(private val repository: ItemRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return HomeViewModel(repository) as T
    }
  }
}
