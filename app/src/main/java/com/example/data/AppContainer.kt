package com.example.data

import android.content.Context

class AppContainer(context: Context) {
  private val appContext = context.applicationContext
  val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }
  val photoStorageManager: PhotoStorageManager by lazy { PhotoStorageManager(appContext) }
  val itemRepository: ItemRepository by lazy {
    ItemRepository(
      database = database,
      itemDao = database.itemDao(),
      locationDao = database.locationDao(),
      photoStorageManager = photoStorageManager,
      itemHistoryDao = database.itemHistoryDao(),
    )
  }
}
