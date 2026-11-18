package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object HistoryEventType {
  const val CREATED = "CREATED"
  const val MOVED = "MOVED"
  const val BOX_RENAMED = "BOX_RENAMED"
}

@Entity(
  tableName = "item_history",
  foreignKeys =
    [
      ForeignKey(
        entity = ItemEntity::class,
        parentColumns = ["id"],
        childColumns = ["itemId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("itemId")],
)
data class ItemHistoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val itemId: Long,
  val eventType: String,
  val fromLocation: String?,
  val toLocation: String,
  val timestamp: Long = System.currentTimeMillis(),
)
