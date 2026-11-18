package com.example.data

data class LocationSummary(
  val id: Long,
  val name: String,
  val qrCodeId: String,
  val itemCount: Int,
  val createdAt: Long,
  val updatedAt: Long,
) {
  fun toLocationEntity(): LocationEntity =
    LocationEntity(
      id = id,
      name = name,
      qrCodeId = qrCodeId,
      createdAt = createdAt,
      updatedAt = updatedAt,
    )
}
