package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToString
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.HistoryEventType
import com.example.data.ItemRepository
import com.example.data.LocationEntity
import com.example.data.PhotoStorageManager
import com.example.data.QrPdfManager
import com.example.ui.GdeLezhitNavGraph
import com.example.ui.theme.GdeLezhitTheme
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class GdeLezhitV2FeaturesTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val dbName = "test_v2_features.db"
  private var currentDb: AppDatabase? = null

  @After
  fun tearDown() {
    currentDb?.close()
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.deleteDatabase(dbName)
  }

  private fun waitForUi(timeoutMs: Long = 5_000, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!condition()) {
      if (System.currentTimeMillis() > deadline) {
        val tree = runCatching { composeTestRule.onRoot().printToString() }.getOrElse { it.toString() }
        throw AssertionError("UI condition not met within ${timeoutMs}ms.\nTree:\n$tree")
      }
      Shadows.shadowOf(Looper.getMainLooper()).idle()
      composeTestRule.mainClock.advanceTimeBy(50)
      Thread.sleep(15)
    }
    Shadows.shadowOf(Looper.getMainLooper()).idle()
  }

  private fun createRepository(): ItemRepository {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db =
      Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .allowMainThreadQueries()
        .build()
        .also { currentDb = it }
    return ItemRepository(db, db.itemDao(), db.locationDao(), PhotoStorageManager(context))
  }

  @Test
  fun `locations tab displays counts creates empty location deletes empty and opens pdf dialog`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = createRepository()
    val photoStorageManager = PhotoStorageManager(context)

    val box1 = runBlocking { repository.getOrCreateLocation("Антресоль → синяя коробка") }
    runBlocking {
      repository.addItem("Фонарик", box1.name, "", null, preferredLocationId = box1.id)
      repository.addItem("Батарейки", box1.name, "", null, preferredLocationId = box1.id)
    }

    composeTestRule.setContent {
      GdeLezhitTheme {
        GdeLezhitNavGraph(repository = repository, photoStorageManager = photoStorageManager)
      }
    }

    // Switch to "Места" tab
    waitForUi {
      composeTestRule.onAllNodesWithTag("tab_locations").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("tab_locations").performClick()

    waitForUi {
      composeTestRule.onAllNodesWithTag("locations_list").fetchSemanticsNodes().isNotEmpty() &&
        composeTestRule.onAllNodesWithText("Вещей: 2").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Антресоль → синяя коробка").assertIsDisplayed()
    composeTestRule.onNodeWithText("Вещей: 2").assertIsDisplayed()

    // Create a new empty location "Балкон → шкафчик" via "+ Новое место"
    composeTestRule.onNodeWithTag("add_location_button").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithTag("create_location_input").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("create_location_input").performTextInput("Балкон → шкафчик")
    composeTestRule.onNodeWithTag("confirm_create_location_button").performClick()

    // Navigates straight into LocationDetailScreen for the new empty box
    waitForUi {
      composeTestRule.onAllNodesWithTag("location_title").fetchSemanticsNodes().isNotEmpty() &&
        composeTestRule.onAllNodesWithText("Балкон → шкафчик").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("location_back_button").performClick()

    // Back on Locations tab: should show empty badge and allow multi-select PDF generation
    waitForUi {
      composeTestRule.onAllNodesWithTag("locations_list").fetchSemanticsNodes().isNotEmpty() &&
        composeTestRule.onAllNodesWithText("Пусто").fetchSemanticsNodes().isNotEmpty()
    }

    // Enter multi-select PDF mode and select all 2 locations
    composeTestRule.onNodeWithTag("pdf_sheet_mode_button").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithTag("select_all_pdf_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("select_all_pdf_button").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithText("Выбрано коробок: 2").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("generate_pdf_button").performClick()

    waitForUi {
      composeTestRule.onAllNodesWithTag("pdf_summary_text").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("save_pdf_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("share_pdf_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("close_pdf_dialog_button").performClick()

    // Exit PDF mode and safely delete the empty location
    composeTestRule.onNodeWithTag("cancel_pdf_selection_button").performClick()
    val balconyLoc = runBlocking {
      repository.allLocations.first().first { it.name == "Балкон → шкафчик" }
    }
    waitForUi {
      composeTestRule
        .onAllNodesWithTag("delete_empty_location_${balconyLoc.id}")
        .fetchSemanticsNodes()
        .isNotEmpty()
    }
    composeTestRule.onNodeWithTag("delete_empty_location_${balconyLoc.id}").performClick()
    waitForUi {
      composeTestRule
        .onAllNodesWithTag("confirm_delete_location_button")
        .fetchSemanticsNodes()
        .isNotEmpty()
    }
    composeTestRule.onNodeWithTag("confirm_delete_location_button").performClick()

    waitForUi {
      composeTestRule.onAllNodesWithText("Балкон → шкафчик").fetchSemanticsNodes().isEmpty()
    }
  }

  @Test
  fun `item movement history records creation move and box rename inside detail screen`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = createRepository()
    val photoStorageManager = PhotoStorageManager(context)

    val itemId = runBlocking {
      repository.addItem("Дрель", "Антресоль → синяя коробка", "", null)
    }

    composeTestRule.setContent {
      GdeLezhitTheme {
        GdeLezhitNavGraph(repository = repository, photoStorageManager = photoStorageManager)
      }
    }

    // Open item detail and check initial creation entry
    waitForUi {
      composeTestRule.onAllNodesWithTag("item_card_$itemId").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("item_card_$itemId").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithTag("item_history_section").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Добавлено в «Антресоль → синяя коробка»").assertIsDisplayed()

    // Move item to "Шкаф" via UI dialog
    composeTestRule.onNodeWithTag("move_button").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithTag("move_location_input").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("move_location_input").performTextClearance()
    composeTestRule.onNodeWithTag("move_location_input").performTextInput("Шкаф")
    composeTestRule.onNodeWithTag("confirm_move_button").performClick()

    waitForUi {
      composeTestRule
        .onAllNodesWithText("Перемещено: «Антресоль → синяя коробка» → «Шкаф»")
        .fetchSemanticsNodes()
        .isNotEmpty()
    }

    // Rename/move entire box "Шкаф" -> "Гараж → стеллаж" and verify BOX_RENAMED history entry
    val updatedItem = runBlocking { repository.getItem(itemId)!! }
    runBlocking { repository.renameOrMoveLocation(updatedItem.locationId, "Гараж → стеллаж") }

    waitForUi {
      composeTestRule
        .onAllNodesWithText("Коробка перенесена: «Шкаф» → «Гараж → стеллаж»")
        .fetchSemanticsNodes()
        .isNotEmpty()
    }

    val historyEntries = runBlocking { repository.observeItemHistory(itemId).first() }
    assertEquals(
      listOf(HistoryEventType.BOX_RENAMED, HistoryEventType.MOVED, HistoryEventType.CREATED),
      historyEntries.map { it.eventType },
    )
  }

  @Test
  fun `qr pdf manager builds valid multipage a4 pdf stream`() {
    assertEquals(1, QrPdfManager.calculatePageCount(8))
    assertEquals(2, QrPdfManager.calculatePageCount(9))
    assertEquals(2, QrPdfManager.calculatePageCount(16))

    val sampleLocations =
      (1..10).map { idx ->
        LocationEntity(id = idx.toLong(), name = "Коробка #$idx", qrCodeId = "qr-uuid-$idx")
      }
    val out = ByteArrayOutputStream()
    val generated = QrPdfManager.generateQrSheetPdf(sampleLocations, out)
    assertTrue(generated)

    val pdfBytes = out.toByteArray()
    assertTrue(pdfBytes.size > 4_000)
    val pdfAscii = String(pdfBytes, Charsets.ISO_8859_1)
    assertTrue(pdfAscii.startsWith("%PDF-"))
    assertTrue(pdfAscii.contains("/Count 2"))
  }

  @Test
  fun `about and privacy dialog opens from home header and closes cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = createRepository()
    val photoStorageManager = PhotoStorageManager(context)

    composeTestRule.setContent {
      GdeLezhitTheme {
        GdeLezhitNavGraph(repository = repository, photoStorageManager = photoStorageManager)
      }
    }

    waitForUi {
      composeTestRule.onAllNodesWithTag("about_app_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("about_app_button").performClick()

    waitForUi {
      composeTestRule.onAllNodesWithTag("about_dialog_title").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Конфиденциальность и данные").assertIsDisplayed()
    composeTestRule.onNodeWithText("Доступ к камере и фото").assertIsDisplayed()

    composeTestRule.onNodeWithTag("close_about_dialog_button").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithTag("about_dialog_title").fetchSemanticsNodes().isEmpty()
    }
  }
}
