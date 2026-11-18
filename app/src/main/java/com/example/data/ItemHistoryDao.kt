package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemHistoryDao {
  @Query("SELECT * FROM item_history WHERE itemId = :itemId ORDER BY timestamp DESC, id DESC")
  fun observeHistoryForItem(itemId: Long): Flow<List<ItemHistoryEntity>>

  @Query("SELECT * FROM item_history WHERE itemId = :itemId ORDER BY timestamp DESC, id DESC")
  suspend fun getHistoryForItemOnce(itemId: Long): List<ItemHistoryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHistory(entry: ItemHistoryEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHistoryEntries(entries: List<ItemHistoryEntity>)
}
