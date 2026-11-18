package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.ItemRepository
import com.example.data.PhotoStorageManager
import com.example.ui.GdeLezhitNavGraph
import com.example.ui.theme.GdeLezhitTheme
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GdeLezhitScenarioTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val dbName = "test_gde_lezhit.db"
  private var currentDb: AppDatabase? = null

  @After
  fun tearDown() {
    currentDb?.close()
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.deleteDatabase(dbName)
    File(context.filesDir, "item_photos").deleteRecursively()
  }

  private fun waitForUi(timeoutMs: Long = 5_000, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!condition()) {
      if (System.currentTimeMillis() > deadline) {
        throw AssertionError("UI condition not met within ${timeoutMs}ms")
      }
      Shadows.shadowOf(Looper.getMainLooper()).idle()
      composeTestRule.mainClock.advanceTimeBy(50)
      Thread.sleep(15)
    }
    Shadows.shadowOf(Looper.getMainLooper()).idle()
  }

  private fun createSampleJpegFile(context: Context, name: String): File {
    val file = File(context.cacheDir, name)
    val bitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
    bitmap.recycle()
    return file
  }

  @Test
  fun `full core scenario add search move and persist after restart`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val photoStorageManager = PhotoStorageManager(context)
    val db =
      Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .allowMainThreadQueries()
        .build()
        .also { currentDb = it }
    val repository = ItemRepository(db, db.itemDao(), db.locationDao(), photoStorageManager)

    val originalGalleryPhoto = createSampleJpegFile(context, "original_garland.jpg")
    assertTrue(originalGalleryPhoto.exists())

    composeTestRule.setContent {
      GdeLezhitTheme {
        GdeLezhitNavGraph(
          repository = repository,
          photoStorageManager = photoStorageManager,
        )
      }
    }

    // Empty state is displayed initially
    waitForUi {
      composeTestRule.onAllNodesWithText("Добавьте первую вещь").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Добавьте первую вещь").assertIsDisplayed()

    // 1. Add "Гирлянда" with location "Антресоль, синяя коробка" via UI
    composeTestRule.onNodeWithTag("add_item_button").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithTag("name_input").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag("name_input").performTextInput("Гирлянда")
    composeTestRule.onNodeWithTag("location_input").performTextInput("Антресоль, синяя коробка")
    composeTestRule.onNodeWithTag("save_button").performClick()

    waitForUi {
      composeTestRule.onAllNodesWithTag("items_list").fetchSemanticsNodes().isNotEmpty() &&
        composeTestRule.onAllNodesWithText("Гирлянда").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Гирлянда").assertIsDisplayed()
    composeTestRule.onNodeWithText("Антресоль, синяя коробка").assertIsDisplayed()

    // Attach local photo copy to the created item and delete the original gallery file
    val createdItem = runBlocking { repository.allItems.first().first() }
    runBlocking {
      repository.updateItem(
        id = createdItem.id,
        name = createdItem.name,
        location = createdItem.location,
        note = createdItem.note,
        newPhotoUri = Uri.fromFile(originalGalleryPhoto),
        removeExistingPhoto = false,
      )
    }
    assertTrue(originalGalleryPhoto.delete())
    assertFalse(originalGalleryPhoto.exists())

    val itemWithPhoto = runBlocking { repository.getItem(createdItem.id) }
    assertNotNull(itemWithPhoto?.photoPath)
    val internalPhotoPath = itemWithPhoto!!.photoPath!!
    assertTrue(File(internalPhotoPath).exists())

    // 2. Search case-insensitively in Russian: "ГИРЛ" and "синяя"
    composeTestRule.onNodeWithTag("search_input").performTextInput("ГИРЛ")
    waitForUi {
      composeTestRule.onAllNodesWithText("Гирлянда").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Гирлянда").assertIsDisplayed()

    composeTestRule.onNodeWithTag("search_input").performTextClearance()
    composeTestRule.onNodeWithTag("search_input").performTextInput("синяя")
    waitForUi {
      composeTestRule.onAllNodesWithText("Гирлянда").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Гирлянда").assertIsDisplayed()

    // Empty search state check
    composeTestRule.onNodeWithTag("search_input").performTextClearance()
    composeTestRule.onNodeWithTag("search_input").performTextInput("Дрель")
    waitForUi {
      composeTestRule.onAllNodesWithText("Ничего не найдено").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("reset_search_button").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithTag("item_card_${createdItem.id}").fetchSemanticsNodes().isNotEmpty()
    }

    // 3. Open item detail and move to "Шкаф, верхняя полка"
    composeTestRule.onNodeWithTag("item_card_${createdItem.id}").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithTag("move_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("move_button").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithTag("move_location_input").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag("move_location_input").performTextClearance()
    composeTestRule.onNodeWithTag("move_location_input").performTextInput("Шкаф, верхняя полка")
    composeTestRule.onNodeWithTag("confirm_move_button").performClick()

    waitForUi {
      composeTestRule.onAllNodesWithText("Шкаф, верхняя полка").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("detail_back_button").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithTag("search_input").fetchSemanticsNodes().isNotEmpty()
    }

    // Verify search works for the new location
    composeTestRule.onNodeWithTag("search_input").performTextInput("верхняя")
    waitForUi {
      composeTestRule.onAllNodesWithText("Шкаф, верхняя полка").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Гирлянда").assertIsDisplayed()
    composeTestRule.onNodeWithText("Шкаф, верхняя полка").assertIsDisplayed()

    // 4. Simulate app restart by closing & reopening database
    db.close()
    val reopenedDb =
      Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .allowMainThreadQueries()
        .build()
        .also { currentDb = it }
    val reopenedRepo =
      ItemRepository(
        reopenedDb,
        reopenedDb.itemDao(),
        reopenedDb.locationDao(),
        PhotoStorageManager(context),
      )
    val persistedItems = runBlocking { reopenedRepo.allItems.first() }

    assertEquals(1, persistedItems.size)
    val persisted = persistedItems.first()
    assertEquals("Гирлянда", persisted.name)
    assertEquals("Шкаф, верхняя полка", persisted.location)
    assertEquals(internalPhotoPath, persisted.photoPath)
    assertTrue(File(persisted.photoPath!!).exists())
  }
}
