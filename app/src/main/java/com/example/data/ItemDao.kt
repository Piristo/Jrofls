package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
  @Query("SELECT * FROM items ORDER BY updatedAt DESC, id DESC")
  fun observeAllItems(): Flow<List<ItemEntity>>

  @Query("SELECT * FROM items WHERE locationId = :locationId ORDER BY updatedAt DESC, id DESC")
  fun observeItemsByLocationId(locationId: Long): Flow<List<ItemEntity>>

  @Query("SELECT * FROM items WHERE locationId = :locationId ORDER BY updatedAt DESC, id DESC")
  suspend fun getItemsByLocationIdOnce(locationId: Long): List<ItemEntity>

  @Query("SELECT * FROM items WHERE id = :id")
  fun observeItemById(id: Long): Flow<ItemEntity?>

  @Query("SELECT * FROM items WHERE id = :id")
  suspend fun getItemById(id: Long): ItemEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertItem(item: ItemEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertItems(items: List<ItemEntity>)

  @Update
  suspend fun updateItem(item: ItemEntity)

  @Query(
    "UPDATE items SET locationId = :locationId, location = :location, updatedAt = :updatedAt WHERE id = :id"
  )
  suspend fun updateItemLocation(id: Long, locationId: Long, location: String, updatedAt: Long)

  @Query("UPDATE items SET location = :newLocationName, updatedAt = :updatedAt WHERE locationId = :locationId")
  suspend fun updateLocationNameForItems(locationId: Long, newLocationName: String, updatedAt: Long)

  @Query("DELETE FROM items WHERE id = :id")
  suspend fun deleteItemById(id: Long)
}
