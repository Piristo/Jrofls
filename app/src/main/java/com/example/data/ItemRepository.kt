package com.example.data

import android.net.Uri
import androidx.room.withTransaction
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ItemRepository(
  private val database: AppDatabase,
  private val itemDao: ItemDao,
  private val locationDao: LocationDao,
  private val photoStorageManager: PhotoStorageManager,
  private val clock: () -> Long = { System.currentTimeMillis() },
  private val itemHistoryDao: ItemHistoryDao = database.itemHistoryDao(),
) {
  val allItems: Flow<List<ItemEntity>> = itemDao.observeAllItems()

  val allLocations: Flow<List<LocationEntity>> = locationDao.observeAllLocations()

  val locationSummaries: Flow<List<LocationSummary>> = locationDao.observeLocationSummaries()

  val recentLocations: Flow<List<String>> =
    allLocations.map { locations ->
      locations.asSequence().map { it.name.trim() }.filter { it.isNotEmpty() }.toList()
    }

  fun observeItem(id: Long): Flow<ItemEntity?> = itemDao.observeItemById(id)

  suspend fun getItem(id: Long): ItemEntity? = itemDao.getItemById(id)

  fun observeItemHistory(itemId: Long): Flow<List<ItemHistoryEntity>> =
    itemHistoryDao.observeHistoryForItem(itemId)

  fun observeLocation(locationId: Long): Flow<LocationEntity?> =
    locationDao.observeLocationById(locationId)

  suspend fun getLocation(locationId: Long): LocationEntity? =
    locationDao.getLocationById(locationId)

  fun observeItemsInLocation(locationId: Long): Flow<List<ItemEntity>> =
    itemDao.observeItemsByLocationId(locationId)

  suspend fun findLocationByQrPayload(rawPayload: String): LocationEntity? {
    val qrId = QrCodeManager.parseQrCodeId(rawPayload) ?: return null
    return locationDao.getLocationByQrCodeId(qrId)
  }

  suspend fun getOrCreateLocation(rawName: String): LocationEntity {
    val trimmed = rawName.trim()
    val normalizedTarget = normalizeForSearch(trimmed)
    val existing =
      locationDao.getAllLocationsOnce().firstOrNull {
        normalizeForSearch(it.name) == normalizedTarget
      }
    if (existing != null) return existing

    val now = clock()
    val newLocation =
      LocationEntity(
        name = trimmed,
        qrCodeId = UUID.randomUUID().toString(),
        createdAt = now,
        updatedAt = now,
      )
    val newId = locationDao.insertLocation(newLocation)
    return newLocation.copy(id = newId)
  }

  suspend fun createLocation(rawName: String): LocationEntity? {
    val trimmed = rawName.trim()
    if (trimmed.isEmpty()) return null
    return getOrCreateLocation(trimmed)
  }

  suspend fun deleteEmptyLocation(locationId: Long): Boolean {
    return locationDao.deleteEmptyLocationById(locationId) > 0
  }

  suspend fun addItem(
    name: String,
    location: String,
    note: String,
    photoUri: Uri?,
    preferredLocationId: Long? = null,
  ): Long {
    val now = clock()
    val savedPhotoPath = photoUri?.let { photoStorageManager.savePhotoCopyFromUri(it) }
    return database.withTransaction {
      val resolvedLocation = resolveLocationForSave(location, preferredLocationId)
      val item =
        ItemEntity(
          name = name.trim(),
          locationId = resolvedLocation.id,
          location = resolvedLocation.name,
          note = note.trim(),
          photoPath = savedPhotoPath,
          createdAt = now,
          updatedAt = now,
        )
      val insertedId = itemDao.insertItem(item)
      itemHistoryDao.insertHistory(
        ItemHistoryEntity(
          itemId = insertedId,
          eventType = HistoryEventType.CREATED,
          fromLocation = null,
          toLocation = resolvedLocation.name,
          timestamp = now,
        )
      )
      insertedId
    }
  }

  suspend fun addMultipleItemsToLocation(locationId: Long, rawMultilineNames: String): Int {
    val locationEntity = locationDao.getLocationById(locationId) ?: return 0
    val names =
      rawMultilineNames
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toList()
    if (names.isEmpty()) return 0

    val baseTime = clock()
    return database.withTransaction {
      val historyEntries = ArrayList<ItemHistoryEntity>(names.size)
      names.forEachIndexed { index, itemName ->
        val ts = baseTime + index
        val insertedId =
          itemDao.insertItem(
            ItemEntity(
              name = itemName,
              locationId = locationEntity.id,
              location = locationEntity.name,
              note = "",
              photoPath = null,
              createdAt = ts,
              updatedAt = ts,
            )
          )
        historyEntries.add(
          ItemHistoryEntity(
            itemId = insertedId,
            eventType = HistoryEventType.CREATED,
            fromLocation = null,
            toLocation = locationEntity.name,
            timestamp = ts,
          )
        )
      }
      itemHistoryDao.insertHistoryEntries(historyEntries)
      names.size
    }
  }

  suspend fun updateItem(
    id: Long,
    name: String,
    location: String,
    note: String,
    newPhotoUri: Uri?,
    removeExistingPhoto: Boolean,
  ) {
    val existing = itemDao.getItemById(id) ?: return
    val oldPhotoPath = existing.photoPath
    val newlyCopiedPhotoPath =
      if (newPhotoUri != null) photoStorageManager.savePhotoCopyFromUri(newPhotoUri) else null

    val finalPhotoPath =
      when {
        newPhotoUri != null -> newlyCopiedPhotoPath ?: if (removeExistingPhoto) null else oldPhotoPath
        removeExistingPhoto -> null
        else -> oldPhotoPath
      }

    val now = clock()
    database.withTransaction {
      val resolvedLocation =
        if (normalizeForSearch(existing.location) == normalizeForSearch(location)) {
          locationDao.getLocationById(existing.locationId) ?: getOrCreateLocation(location)
        } else {
          getOrCreateLocation(location)
        }

      itemDao.updateItem(
        existing.copy(
          name = name.trim(),
          locationId = resolvedLocation.id,
          location = resolvedLocation.name,
          note = note.trim(),
          photoPath = finalPhotoPath,
          updatedAt = now,
        )
      )

      if (
        existing.locationId != resolvedLocation.id ||
          normalizeForSearch(existing.location) != normalizeForSearch(resolvedLocation.name)
      ) {
        itemHistoryDao.insertHistory(
          ItemHistoryEntity(
            itemId = existing.id,
            eventType = HistoryEventType.MOVED,
            fromLocation = existing.location,
            toLocation = resolvedLocation.name,
            timestamp = now,
          )
        )
      }
    }

    if (
      (newlyCopiedPhotoPath != null && oldPhotoPath != null) ||
        (newPhotoUri == null && removeExistingPhoto && oldPhotoPath != null)
    ) {
      photoStorageManager.deletePhotoFile(oldPhotoPath)
    }
  }

  suspend fun moveItem(id: Long, newLocation: String) {
    val trimmed = newLocation.trim()
    if (trimmed.isEmpty()) return
    val now = clock()
    database.withTransaction {
      val existing = itemDao.getItemById(id) ?: return@withTransaction
      val targetLocation = getOrCreateLocation(trimmed)
      itemDao.updateItemLocation(
        id = id,
        locationId = targetLocation.id,
        location = targetLocation.name,
        updatedAt = now,
      )
      if (
        existing.locationId != targetLocation.id ||
          normalizeForSearch(existing.location) != normalizeForSearch(targetLocation.name)
      ) {
        itemHistoryDao.insertHistory(
          ItemHistoryEntity(
            itemId = existing.id,
            eventType = HistoryEventType.MOVED,
            fromLocation = existing.location,
            toLocation = targetLocation.name,
            timestamp = now,
          )
        )
      }
    }
  }

  suspend fun renameOrMoveLocation(locationId: Long, newLocationName: String) {
    val trimmed = newLocationName.trim()
    if (trimmed.isEmpty()) return
    val now = clock()
    database.withTransaction {
      val existingLocation = locationDao.getLocationById(locationId) ?: return@withTransaction
      val affectedItems = itemDao.getItemsByLocationIdOnce(locationId)
      locationDao.updateLocationName(id = locationId, name = trimmed, updatedAt = now)
      itemDao.updateLocationNameForItems(
        locationId = locationId,
        newLocationName = trimmed,
        updatedAt = now,
      )
      if (existingLocation.name.trim() != trimmed && affectedItems.isNotEmpty()) {
        val entries =
          affectedItems.map { item ->
            ItemHistoryEntity(
              itemId = item.id,
              eventType = HistoryEventType.BOX_RENAMED,
              fromLocation = existingLocation.name,
              toLocation = trimmed,
              timestamp = now,
            )
          }
        itemHistoryDao.insertHistoryEntries(entries)
      }
    }
  }

  suspend fun deleteItem(item: ItemEntity) {
    itemDao.deleteItemById(item.id)
    photoStorageManager.deletePhotoFile(item.photoPath)
  }

  private suspend fun resolveLocationForSave(
    rawName: String,
    preferredLocationId: Long?,
  ): LocationEntity {
    if (preferredLocationId != null) {
      val preferred = locationDao.getLocationById(preferredLocationId)
      if (preferred != null && normalizeForSearch(preferred.name) == normalizeForSearch(rawName)) {
        return preferred
      }
    }
    return getOrCreateLocation(rawName)
  }

  companion object {
    private val RU_LOCALE = Locale.forLanguageTag("ru-RU")

    fun normalizeForSearch(text: String): String =
      text.trim().lowercase(RU_LOCALE).replace('ё', 'е')

    fun matchesQuery(item: ItemEntity, rawQuery: String): Boolean {
      val query = normalizeForSearch(rawQuery)
      if (query.isEmpty()) return true
      return normalizeForSearch(item.name).contains(query) ||
        normalizeForSearch(item.location).contains(query) ||
        normalizeForSearch(item.note).contains(query)
    }

    fun matchesLocationQuery(location: LocationSummary, rawQuery: String): Boolean {
      val query = normalizeForSearch(rawQuery)
      if (query.isEmpty()) return true
      return normalizeForSearch(location.name).contains(query)
    }
  }
}
