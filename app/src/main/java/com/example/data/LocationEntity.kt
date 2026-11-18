package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
  tableName = "locations",
  indices = [Index(value = ["qrCodeId"], unique = true)],
)
data class LocationEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val name: String,
  val qrCodeId: String = UUID.randomUUID().toString(),
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
)
