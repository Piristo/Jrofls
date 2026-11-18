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
import com.example.data.ItemRepository
import com.example.data.PhotoStorageManager
import com.example.data.QrCodeManager
import com.example.ui.GdeLezhitNavGraph
import com.example.ui.QrScanUiState
import com.example.ui.QrScannerViewModel
import com.example.ui.theme.GdeLezhitTheme
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
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class GdeLezhitLocationsAndQrTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val dbName = "test_locations_qr.db"
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
  fun `clickable location add here and move single item between locations`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = createRepository()
    val photoStorageManager = PhotoStorageManager(context)

    composeTestRule.setContent {
      GdeLezhitTheme {
        GdeLezhitNavGraph(repository = repository, photoStorageManager = photoStorageManager)
      }
    }

    // Add "Гирлянда" to "Антресоль → синяя коробка"
    waitForUi {
      composeTestRule.onAllNodesWithTag("add_item_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("add_item_button").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithTag("name_input").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("name_input").performTextInput("Гирлянда")
    composeTestRule.onNodeWithTag("location_input").performTextInput("Антресоль → синяя коробка")
    composeTestRule.onNodeWithTag("save_button").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithTag("items_list").fetchSemanticsNodes().isNotEmpty() &&
        composeTestRule.onAllNodesWithText("Гирлянда").fetchSemanticsNodes().isNotEmpty()
    }

    // Add "Ёлочные игрушки" using live location suggestion chip
    composeTestRule.onNodeWithTag("add_item_button").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithTag("name_input").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("name_input").performTextInput("Ёлочные игрушки")
    composeTestRule.onNodeWithTag("location_input").performTextInput("антресоль")
    waitForUi {
      composeTestRule.onAllNodesWithText("Антресоль → синяя коробка").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Антресоль → синяя коробка").performClick()
    composeTestRule.onNodeWithTag("save_button").performClick()

    waitForUi {
      composeTestRule.onAllNodesWithTag("items_list").fetchSemanticsNodes().isNotEmpty() &&
        composeTestRule.onAllNodesWithText("Ёлочные игрушки").fetchSemanticsNodes().isNotEmpty()
    }

    val garlandItem = runBlocking { repository.allItems.first().first { it.name == "Гирлянда" } }

    // Open "Гирлянда" card and click its storage location
    composeTestRule.onNodeWithTag("item_card_${garlandItem.id}").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithTag("detail_location_card").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("detail_location_card").performClick()

    // Verify both items are shown on Location screen
    waitForUi {
      composeTestRule.onAllNodesWithTag("location_items_list").fetchSemanticsNodes().isNotEmpty() &&
        composeTestRule.onAllNodesWithText("Вещей здесь: 2").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Гирлянда").assertIsDisplayed()
    composeTestRule.onNodeWithText("Ёлочные игрушки").assertIsDisplayed()

    // Tap "Добавить сюда" and add "Мишура"
    composeTestRule.onNodeWithTag("add_here_button").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithTag("name_input").fetchSemanticsNodes().isNotEmpty() &&
        composeTestRule.onAllNodesWithText("Антресоль → синяя коробка").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("name_input").performTextInput("Мишура")
    composeTestRule.onNodeWithTag("save_button").performClick()

    waitForUi {
      composeTestRule.onAllNodesWithTag("location_items_list").fetchSemanticsNodes().isNotEmpty() &&
        composeTestRule.onAllNodesWithText("Мишура").fetchSemanticsNodes().isNotEmpty()
    }

    // Move "Гирлянда" to "Шкаф" and check box vs wardrobe lists
    composeTestRule.onNodeWithTag("item_card_${garlandItem.id}").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithTag("move_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("move_button").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithTag("move_location_input").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("move_location_input").performTextClearance()
    composeTestRule.onNodeWithTag("move_location_input").performTextInput("Шкаф")
    composeTestRule.onNodeWithTag("confirm_move_button").performClick()

    waitForUi {
      composeTestRule.onAllNodesWithText("Шкаф").fetchSemanticsNodes().isNotEmpty()
    }

    // Open "Шкаф" from garland card -> contains "Гирлянда"
    composeTestRule.onNodeWithTag("detail_location_card").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithText("Вещей здесь: 1").fetchSemanticsNodes().isNotEmpty() &&
        composeTestRule.onAllNodesWithText("Гирлянда").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Гирлянда").assertIsDisplayed()

    // Blue box now has 2 items ("Мишура", "Ёлочные игрушки") without "Гирлянда"
    val blueBoxItems = runBlocking { repository.observeItemsInLocation(garlandItem.locationId).first() }
    assertEquals(listOf("Мишура", "Ёлочные игрушки"), blueBoxItems.map { it.name })
  }

  @Test
  fun `qr sticker encodes only id survives rename and handles unknown codes`() {
    val repository = createRepository()

    // Create location "Антресоль → синяя коробка"
    val box = runBlocking { repository.getOrCreateLocation("Антресоль → синяя коробка") }
    val payload = QrCodeManager.qrPayloadForLocation(box)

    // QR payload must NOT contain location name, only persistent ID
    assertFalse(payload.contains("синяя"))
    assertTrue(payload.contains(box.qrCodeId))

    // Render printable PNG sticker bitmap and decode it offline
    val stickerBitmap = QrCodeManager.createPrintableStickerBitmap(box.name, payload)
    val decodedFromSticker = QrCodeManager.decodeQrFromBitmap(stickerBitmap)
    stickerBitmap.recycle()
    assertEquals(payload, decodedFromSticker)

    // Unknown QR code resolves to NotFound ("Место для этой наклейки не найдено")
    val scannerVm = QrScannerViewModel(repository)
    scannerVm.onQrPayloadDetected("gdelezhit://loc/unknown-id")
    waitForUi { scannerVm.scanState.value is QrScanUiState.NotFound }
    assertTrue(scannerVm.scanState.value is QrScanUiState.NotFound)
    scannerVm.resetScan()

    // Scan valid sticker -> resolves box.id, add "Мишура"
    scannerVm.onQrPayloadDetected(decodedFromSticker!!)
    waitForUi { scannerVm.scanState.value is QrScanUiState.Resolved }
    val resolved = scannerVm.scanState.value as QrScanUiState.Resolved
    assertEquals(box.id, resolved.locationId)

    runBlocking {
      repository.addItem("Мишура", box.name, "", null, preferredLocationId = resolved.locationId)
      repository.renameOrMoveLocation(box.id, "Новогодняя коробка")
    }

    // Scan the SAME old sticker payload -> still finds the renamed box with "Мишура"
    val resolvedLocationAfterRename = runBlocking {
      repository.findLocationByQrPayload(decodedFromSticker)
    }
    assertNotNull(resolvedLocationAfterRename)
    assertEquals(box.id, resolvedLocationAfterRename!!.id)
    assertEquals("Новогодняя коробка", resolvedLocationAfterRename.name)

    val itemsAfterRename = runBlocking {
      repository.observeItemsInLocation(resolvedLocationAfterRename.id).first()
    }
    assertEquals(listOf("Мишура"), itemsAfterRename.map { it.name })
  }

  @Test
  fun `batch add and move box updates all items paths while keeping qr working`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = createRepository()
    val photoStorageManager = PhotoStorageManager(context)

    val box = runBlocking { repository.getOrCreateLocation("Антресоль → синяя коробка") }
    val oldQrPayload = QrCodeManager.qrPayloadForLocation(box)

    // Batch add three items to the box
    val added = runBlocking {
      repository.addMultipleItemsToLocation(box.id, "Гирлянда\n\nМишура\nИгрушки\n")
    }
    assertEquals(3, added)

    // Move entire box to "Гараж → стеллаж"
    runBlocking { repository.renameOrMoveLocation(box.id, "Гараж → стеллаж") }

    composeTestRule.setContent {
      GdeLezhitTheme {
        GdeLezhitNavGraph(repository = repository, photoStorageManager = photoStorageManager)
      }
    }

    // Search for "гирлянда" on HomeScreen -> displays new path "Гараж → стеллаж"
    waitForUi {
      composeTestRule.onAllNodesWithTag("items_list").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("search_input").performTextInput("гирлянда")
    waitForUi {
      composeTestRule.onAllNodesWithText("Гирлянда").fetchSemanticsNodes().isNotEmpty() &&
        composeTestRule.onAllNodesWithText("Гараж → стеллаж").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Гирлянда").assertIsDisplayed()
    composeTestRule.onNodeWithText("Гараж → стеллаж").assertIsDisplayed()

    // Old QR sticker still opens the same box with all 3 items
    val resolvedBox = runBlocking { repository.findLocationByQrPayload(oldQrPayload) }
    assertNotNull(resolvedBox)
    assertEquals("Гараж → стеллаж", resolvedBox!!.name)
    val boxItems = runBlocking { repository.observeItemsInLocation(resolvedBox.id).first() }
    assertEquals(setOf("Гирлянда", "Мишура", "Игрушки"), boxItems.map { it.name }.toSet())
  }

  @Test
  fun `location detail ui opens qr sticker batch add and move box dialogs cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = createRepository()
    val photoStorageManager = PhotoStorageManager(context)

    val box = runBlocking { repository.getOrCreateLocation("Антресоль → синяя коробка") }
    runBlocking {
      repository.addItem("Фонарик", box.name, "", null, preferredLocationId = box.id)
    }

    composeTestRule.setContent {
      GdeLezhitTheme {
        GdeLezhitNavGraph(repository = repository, photoStorageManager = photoStorageManager)
      }
    }

    // Open item -> open location screen
    waitForUi {
      composeTestRule.onAllNodesWithTag("items_list").fetchSemanticsNodes().isNotEmpty() &&
        composeTestRule.onAllNodesWithText("Фонарик").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Фонарик").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithTag("detail_location_card").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("detail_location_card").performClick()

    // Open QR sticker dialog and verify sticker preview + label
    waitForUi {
      composeTestRule.onAllNodesWithTag("qr_sticker_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("qr_sticker_button").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithTag("qr_sticker_image").fetchSemanticsNodes().isNotEmpty() &&
        composeTestRule.onAllNodesWithTag("save_qr_png_button").fetchSemanticsNodes().isNotEmpty() &&
        composeTestRule.onAllNodesWithTag("share_qr_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("close_qr_dialog_button").performClick()

    // Batch add items via dialog
    waitForUi {
      composeTestRule.onAllNodesWithTag("batch_add_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("batch_add_button").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithTag("batch_add_input").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("batch_add_input").performTextInput("Батарейки\nЗарядка")
    composeTestRule.onNodeWithTag("confirm_batch_add_button").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithText("Вещей здесь: 3").fetchSemanticsNodes().isNotEmpty()
    }

    // Move entire box via dialog
    composeTestRule.onNodeWithTag("move_box_button").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithTag("location_path_input").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("location_path_input").performTextClearance()
    composeTestRule.onNodeWithTag("location_path_input").performTextInput("Кладовка → полка")
    composeTestRule.onNodeWithTag("confirm_location_path_button").performClick()
    waitForUi {
      composeTestRule.onAllNodesWithText("Кладовка → полка").fetchSemanticsNodes().isNotEmpty()
    }
  }
}
