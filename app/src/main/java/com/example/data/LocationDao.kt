package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
  @Query("SELECT * FROM locations ORDER BY updatedAt DESC, id DESC")
  fun observeAllLocations(): Flow<List<LocationEntity>>

  @Query("SELECT * FROM locations ORDER BY updatedAt DESC, id DESC")
  suspend fun getAllLocationsOnce(): List<LocationEntity>

  @Query("SELECT * FROM locations WHERE id = :id")
  fun observeLocationById(id: Long): Flow<LocationEntity?>

  @Query("SELECT * FROM locations WHERE id = :id")
  suspend fun getLocationById(id: Long): LocationEntity?

  @Query("SELECT * FROM locations WHERE qrCodeId = :qrCodeId LIMIT 1")
  suspend fun getLocationByQrCodeId(qrCodeId: String): LocationEntity?

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertLocation(location: LocationEntity): Long

  @Query(
    """
    SELECT l.id, l.name, l.qrCodeId, COUNT(i.id) AS itemCount, l.createdAt, l.updatedAt
    FROM locations l
    LEFT JOIN items i ON i.locationId = l.id
    GROUP BY l.id
    ORDER BY l.updatedAt DESC, l.id DESC
    """
  )
  fun observeLocationSummaries(): Flow<List<LocationSummary>>

  @Query("UPDATE locations SET name = :name, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateLocationName(id: Long, name: String, updatedAt: Long)

  @Query(
    "DELETE FROM locations WHERE id = :id AND NOT EXISTS (SELECT 1 FROM items WHERE locationId = :id)"
  )
  suspend fun deleteEmptyLocationById(id: Long): Int
}
