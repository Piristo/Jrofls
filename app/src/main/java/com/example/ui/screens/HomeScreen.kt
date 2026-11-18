package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ItemEntity
import com.example.data.LocationEntity
import com.example.data.LocationSummary
import com.example.data.QrPdfManager
import com.example.ui.HomeScreenState
import com.example.ui.HomeTab
import com.example.ui.HomeViewModel
import com.example.ui.ItemsTabState
import com.example.ui.theme.LocalCraftPalette
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
  viewModel: HomeViewModel,
  onAddItemClick: () -> Unit,
  onScanQrClick: () -> Unit,
  onItemClick: (Long) -> Unit,
  onLocationClick: (Long) -> Unit,
) {
  val screenState by viewModel.uiState.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val focusManager = LocalFocusManager.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val craftPalette = LocalCraftPalette.current

  var showCreateLocationDialog by remember { mutableStateOf(false) }
  var showAboutDialog by remember { mutableStateOf(false) }
  var pendingDeleteLocation by remember { mutableStateOf<LocationSummary?>(null) }
  var pdfLocationsToExport by remember { mutableStateOf<List<LocationEntity>?>(null) }

  val scanQrCd = stringResource(R.string.scan_qr_button)
  val pdfSavedMsg = stringResource(R.string.pdf_saved_success)
  val pdfSaveErrorMsg = stringResource(R.string.pdf_save_error)

  BackHandler(
    enabled = screenState.isPdfSelectionMode || screenState.activeTab == HomeTab.LOCATIONS
  ) {
    if (screenState.isPdfSelectionMode) {
      viewModel.exitPdfSelectionMode()
    } else {
      viewModel.selectTab(HomeTab.ITEMS)
    }
  }

  val createPdfDocumentLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
      val locations = pdfLocationsToExport
      if (uri != null && !locations.isNullOrEmpty()) {
        scope.launch {
          val ok = QrPdfManager.writeQrSheetPdfToUri(context, uri, locations)
          pdfLocationsToExport = null
          viewModel.exitPdfSelectionMode()
          snackbarHostState.showSnackbar(if (ok) pdfSavedMsg else pdfSaveErrorMsg)
        }
      }
    }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = {
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(bottom = 84.dp),
      )
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .drawBehind {
            drawRect(
              brush =
                Brush.radialGradient(
                  colors = listOf(craftPalette.headerGlow.copy(alpha = 0.35f), Color.Transparent),
                  center = Offset(size.width * 0.88f, 0f),
                  radius = size.width * 0.9f,
                )
            )
          }
          .padding(innerPadding)
    ) {
      Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Column(
          modifier =
            Modifier.fillMaxWidth().widthIn(max = 600.dp).padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isNarrowHeader = maxWidth < 350.dp
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isNarrowHeader) 8.dp else 10.dp),
              ) {
                Surface(
                  modifier = Modifier.size(if (isNarrowHeader) 38.dp else 44.dp),
                  shape = RoundedCornerShape(13.dp),
                  color = craftPalette.paperTint,
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                  Image(
                    painter = painterResource(id = R.drawable.img_home_header),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(13.dp)),
                  )
                }
                Text(
                  text = stringResource(R.string.app_name),
                  style =
                    if (isNarrowHeader) {
                      MaterialTheme.typography.headlineMedium
                    } else {
                      MaterialTheme.typography.headlineLarge
                    },
                  color = MaterialTheme.colorScheme.onBackground,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                HeaderCountBadge(screenState = screenState)
                IconButton(
                  onClick = { showAboutDialog = true },
                  modifier = Modifier.testTag("about_app_button"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.about_app_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          HomeTabSwitcher(
            activeTab = screenState.activeTab,
            onTabSelect = viewModel::selectTab,
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = viewModel::onSearchQueryChange,
              modifier = Modifier.weight(1f).testTag("search_input"),
              placeholder = {
                Text(
                  text =
                    stringResource(
                      if (screenState.activeTab == HomeTab.ITEMS) {
                        R.string.search_placeholder
                      } else {
                        R.string.search_locations_placeholder
                      }
                    ),
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Rounded.Search,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
              },
              trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                  IconButton(
                    onClick = { viewModel.clearSearch() },
                    modifier = Modifier.testTag("clear_search_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Close,
                      contentDescription = stringResource(R.string.clear_search_cd),
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              },
              singleLine = true,
              textStyle = MaterialTheme.typography.bodyLarge,
              shape = RoundedCornerShape(16.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = MaterialTheme.colorScheme.surface,
                  unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                  focusedBorderColor = MaterialTheme.colorScheme.primary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
              keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            )

            FilledTonalButton(
              onClick = onScanQrClick,
              modifier =
                Modifier.heightIn(min = 56.dp)
                  .semantics { contentDescription = scanQrCd }
                  .testTag("scan_qr_button"),
              shape = RoundedCornerShape(16.dp),
              contentPadding = PaddingValues(horizontal = 14.dp),
              colors =
                ButtonDefaults.filledTonalButtonColors(
                  containerColor = MaterialTheme.colorScheme.primaryContainer,
                  contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
              Icon(
                imageVector = Icons.Rounded.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = "QR", style = MaterialTheme.typography.labelLarge)
            }
          }
        }

        Box(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          contentAlignment = Alignment.TopCenter,
        ) {
          when (screenState.activeTab) {
            HomeTab.ITEMS ->
              ItemsTabBody(
                state = screenState.itemsState,
                onClearSearch = viewModel::clearSearch,
                onItemClick = onItemClick,
              )

            HomeTab.LOCATIONS ->
              LocationsTabBody(
                state = screenState.locationsState,
                isPdfSelectionMode = screenState.isPdfSelectionMode,
                selectedLocationIds = screenState.selectedLocationIds,
                onClearSearch = viewModel::clearSearch,
                onLocationClick = onLocationClick,
                onDeleteEmptyLocationRequest = { pendingDeleteLocation = it },
                onEnterPdfSelectionMode = { viewModel.enterPdfSelectionMode() },
                onExitPdfSelectionMode = viewModel::exitPdfSelectionMode,
                onToggleLocationForPdf = viewModel::toggleLocationForPdf,
                onSelectAllVisibleForPdf = viewModel::selectAllVisibleLocationsForPdf,
                onClearSelectionForPdf = viewModel::clearLocationsForPdf,
              )
          }
        }
      }

      when {
        screenState.activeTab == HomeTab.ITEMS -> {
          FloatingBottomActionPill(
            text = stringResource(R.string.add_item_button).removePrefix("+ "),
            icon = Icons.Rounded.Add,
            onClick = onAddItemClick,
            testTagName = "add_item_button",
            modifier = Modifier.align(Alignment.BottomCenter),
          )
        }

        screenState.isPdfSelectionMode -> {
          val selectedCount = screenState.selectedLocationIds.size
          FloatingBottomActionPill(
            text = stringResource(R.string.pdf_generate_button, selectedCount),
            icon = Icons.Rounded.Print,
            enabled = selectedCount > 0,
            onClick = {
              val selected = screenState.selectedLocationsForPdf()
              if (selected.isNotEmpty()) {
                pdfLocationsToExport = selected
              }
            },
            testTagName = "generate_pdf_button",
            modifier = Modifier.align(Alignment.BottomCenter),
          )
        }

        else -> {
          FloatingBottomActionPill(
            text = stringResource(R.string.add_location_button).removePrefix("+ "),
            icon = Icons.Rounded.Add,
            onClick = { showCreateLocationDialog = true },
            testTagName = "add_location_button",
            modifier = Modifier.align(Alignment.BottomCenter),
          )
        }
      }
    }

    if (showCreateLocationDialog) {
      CreateLocationDialog(
        onDismiss = { showCreateLocationDialog = false },
        onConfirm = { newName ->
          showCreateLocationDialog = false
          viewModel.createLocation(newName) { createdId -> onLocationClick(createdId) }
        },
      )
    }

    val locToDelete = pendingDeleteLocation
    if (locToDelete != null) {
      DeleteEmptyLocationDialog(
        location = locToDelete,
        onDismiss = { pendingDeleteLocation = null },
        onConfirm = {
          pendingDeleteLocation = null
          viewModel.deleteEmptyLocation(locToDelete.id)
        },
      )
    }

    val pdfLocations = pdfLocationsToExport
    if (!pdfLocations.isNullOrEmpty()) {
      PdfSheetExportDialog(
        locations = pdfLocations,
        onDismiss = { pdfLocationsToExport = null },
        onSavePdf = { createPdfDocumentLauncher.launch("qr_stickers_gdelezhit.pdf") },
        onSharePdf = {
          scope.launch {
            val uri = QrPdfManager.createShareableQrSheetPdfUri(context, pdfLocations)
            if (uri != null) {
              val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                  type = "application/pdf"
                  putExtra(Intent.EXTRA_STREAM, uri)
                  addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
              context.startActivity(
                Intent.createChooser(shareIntent, context.getString(R.string.pdf_share_chooser))
              )
              pdfLocationsToExport = null
              viewModel.exitPdfSelectionMode()
            } else {
              snackbarHostState.showSnackbar(pdfSaveErrorMsg)
            }
          }
        },
      )
    }

    if (showAboutDialog) {
      AboutAndPrivacyDialog(onDismiss = { showAboutDialog = false })
    }
  }
}

@Composable
private fun HeaderCountBadge(screenState: HomeScreenState) {
  val labelText =
    when (screenState.activeTab) {
      HomeTab.ITEMS ->
        when (val s = screenState.itemsState) {
          is ItemsTabState.Loaded ->
            if (s.isSearchActive) {
              stringResource(R.string.search_count_label, s.items.size)
            } else {
              stringResource(R.string.item_count_label, s.totalCount)
            }
          else -> null
        }

      HomeTab.LOCATIONS ->
        when (val s = screenState.locationsState) {
          is com.example.ui.LocationsTabState.Loaded ->
            if (s.isSearchActive) {
              stringResource(R.string.locations_search_count_label, s.locations.size)
            } else {
              stringResource(R.string.locations_count_label, s.totalCount)
            }
          else -> null
        }
    }

  if (labelText != null) {
    Surface(
      color = MaterialTheme.colorScheme.primaryContainer,
      shape = RoundedCornerShape(999.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
    ) {
      Text(
        text = labelText,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      )
    }
  }
}

@Composable
private fun HomeTabSwitcher(
  activeTab: HomeTab,
  onTabSelect: (HomeTab) -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      HomeTabSegment(
        label = stringResource(R.string.tab_items),
        icon = Icons.Outlined.Inventory2,
        selected = activeTab == HomeTab.ITEMS,
        onClick = { onTabSelect(HomeTab.ITEMS) },
        modifier = Modifier.weight(1f).testTag("tab_items"),
      )
      HomeTabSegment(
        label = stringResource(R.string.tab_locations),
        icon = Icons.Outlined.Place,
        selected = activeTab == HomeTab.LOCATIONS,
        onClick = { onTabSelect(HomeTab.LOCATIONS) },
        modifier = Modifier.weight(1f).testTag("tab_locations"),
      )
    }
  }
}

@Composable
private fun HomeTabSegment(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val bgColor by
    animateColorAsState(
      targetValue =
        if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
      animationSpec = tween(durationMillis = 180),
      label = "tab_bg",
    )
  val iconTint by
    animateColorAsState(
      targetValue =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
      animationSpec = tween(durationMillis = 180),
      label = "tab_icon",
    )

  Surface(
    modifier =
      modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    color = bgColor,
    shadowElevation = if (selected) 2.dp else 0.dp,
    border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color =
          if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun ItemsTabBody(
  state: ItemsTabState,
  onClearSearch: () -> Unit,
  onItemClick: (Long) -> Unit,
) {
  when (state) {
    ItemsTabState.Loading -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
      }
    }

    ItemsTabState.EmptyCatalog -> {
      IllustratedEmptyState(
        illustrationRes = R.drawable.img_empty_items,
        title = stringResource(R.string.empty_list_title),
        subtitle = stringResource(R.string.empty_list_subtitle),
        modifier = Modifier.fillMaxSize().widthIn(max = 600.dp),
      )
    }

    is ItemsTabState.EmptySearchResult -> {
      IllustratedEmptyState(
        illustrationRes = R.drawable.img_empty_search,
        title = stringResource(R.string.empty_search_title),
        subtitle = stringResource(R.string.empty_search_subtitle),
        actionButton = {
          OutlinedButton(
            onClick = onClearSearch,
            modifier = Modifier.heightIn(min = 48.dp).testTag("reset_search_button"),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          ) {
            Text(
              text = stringResource(R.string.reset_search_button),
              style = MaterialTheme.typography.labelLarge,
            )
          }
        },
        modifier = Modifier.fillMaxSize().widthIn(max = 600.dp),
      )
    }

    is ItemsTabState.Loaded -> {
      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 600.dp).testTag("items_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items(items = state.items, key = { it.id }) { item ->
          ItemListCard(item = item, onClick = { onItemClick(item.id) })
        }
      }
    }
  }
}

@Composable
fun ItemListCard(item: ItemEntity, onClick: () -> Unit) {
  Surface(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .clickable(onClick = onClick)
        .testTag("item_card_${item.id}"),
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    shadowElevation = 1.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.name,
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(8.dp))

        CraftLocationTagPill(locationName = item.location)

        if (item.note.isNotBlank()) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = item.note,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      val photoFile = item.photoPath?.let(::File)?.takeIf { it.exists() }
      if (photoFile != null) {
        Surface(
          modifier = Modifier.size(74.dp),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
          AsyncImage(
            model = photoFile,
            contentDescription = stringResource(R.string.item_thumbnail_cd, item.name),
            contentScale = ContentScale.Crop,
            modifier =
              Modifier.fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("item_thumbnail_${item.id}"),
          )
        }
      }
    }
  }
}
