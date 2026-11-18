package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Database(
  entities = [LocationEntity::class, ItemEntity::class, ItemHistoryEntity::class],
  version = 3,
  exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun itemDao(): ItemDao

  abstract fun locationDao(): LocationDao

  abstract fun itemHistoryDao(): ItemHistoryDao

  companion object {
    @Volatile private var INSTANCE: AppDatabase? = null

    val MIGRATION_1_2 =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `locations` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `name` TEXT NOT NULL,
              `qrCodeId` TEXT NOT NULL,
              `createdAt` INTEGER NOT NULL,
              `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
          )
          db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_locations_qrCodeId` ON `locations` (`qrCodeId`)"
          )

          val locationMap = mutableMapOf<String, Long>()
          val cursor = db.query("SELECT DISTINCT location, createdAt, updatedAt FROM items")
          cursor.use { c ->
            while (c.moveToNext()) {
              val locName = c.getString(0).trim()
              val created = c.getLong(1)
              val updated = c.getLong(2)
              if (locName.isNotEmpty() && !locationMap.containsKey(locName)) {
                val qrId = UUID.randomUUID().toString()
                db.execSQL(
                  "INSERT INTO locations (name, qrCodeId, createdAt, updatedAt) VALUES (?, ?, ?, ?)",
                  arrayOf<Any>(locName, qrId, created, updated),
                )
                val idCursor = db.query("SELECT last_insert_rowid()")
                idCursor.use { ic ->
                  if (ic.moveToFirst()) {
                    locationMap[locName] = ic.getLong(0)
                  }
                }
              }
            }
          }

          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `items_new` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `name` TEXT NOT NULL,
              `locationId` INTEGER NOT NULL,
              `location` TEXT NOT NULL,
              `note` TEXT NOT NULL,
              `photoPath` TEXT,
              `createdAt` INTEGER NOT NULL,
              `updatedAt` INTEGER NOT NULL,
              FOREIGN KEY(`locationId`) REFERENCES `locations`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent()
          )

          val itemsCursor =
            db.query("SELECT id, name, location, note, photoPath, createdAt, updatedAt FROM items")
          itemsCursor.use { ic ->
            while (ic.moveToNext()) {
              val id = ic.getLong(0)
              val name = ic.getString(1)
              val loc = ic.getString(2).trim()
              val note = ic.getString(3)
              val photoPath = if (ic.isNull(4)) null else ic.getString(4)
              val created = ic.getLong(5)
              val updated = ic.getLong(6)
              val locId = locationMap[loc] ?: 1L
              db.execSQL(
                """
                INSERT INTO items_new (id, name, locationId, location, note, photoPath, createdAt, updatedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(id, name, locId, loc, note, photoPath, created, updated),
              )
            }
          }

          db.execSQL("DROP TABLE items")
          db.execSQL("ALTER TABLE items_new RENAME TO items")
          db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_locationId` ON `items` (`locationId`)")
        }
      }

    val MIGRATION_2_3 =
      object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `item_history` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `itemId` INTEGER NOT NULL,
              `eventType` TEXT NOT NULL,
              `fromLocation` TEXT,
              `toLocation` TEXT NOT NULL,
              `timestamp` INTEGER NOT NULL,
              FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_item_history_itemId` ON `item_history` (`itemId`)"
          )
          db.execSQL(
            """
            INSERT INTO `item_history` (`itemId`, `eventType`, `fromLocation`, `toLocation`, `timestamp`)
            SELECT `id`, 'CREATED', NULL, `location`, `createdAt` FROM `items`
            """.trimIndent()
          )
        }
      }

    fun getInstance(context: Context): AppDatabase {
      return INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "gde_lezhit.db",
              )
              .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
              .build()
              .also { INSTANCE = it }
        }
    }
  }
}
