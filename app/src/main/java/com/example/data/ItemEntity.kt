package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "items",
  foreignKeys =
    [
      ForeignKey(
        entity = LocationEntity::class,
        parentColumns = ["id"],
        childColumns = ["locationId"],
        onDelete = ForeignKey.RESTRICT,
      )
    ],
  indices = [Index("locationId")],
)
data class ItemEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val name: String,
  val locationId: Long,
  val location: String,
  val note: String = "",
  val photoPath: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
)
