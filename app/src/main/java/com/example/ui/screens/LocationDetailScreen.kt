package com.example.ui.screens

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ItemRepository
import com.example.data.LocationEntity
import com.example.data.QrCodeManager
import com.example.ui.LocationDetailUiState
import com.example.ui.LocationDetailViewModel
import com.example.ui.theme.LocalCraftPalette
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LocationDetailScreen(
  viewModel: LocationDetailViewModel,
  onNavigateBack: () -> Unit,
  onAddHereClick: (Long) -> Unit,
  onItemClick: (Long) -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  var showBatchAddDialog by remember { mutableStateOf(false) }
  var showMoveBoxDialog by remember { mutableStateOf(false) }
  var showRenameDialog by remember { mutableStateOf(false) }
  var showQrStickerDialog by remember { mutableStateOf(false) }

  val qrSavedMsg = stringResource(R.string.qr_saved_success)
  val qrSaveErrorMsg = stringResource(R.string.qr_save_error)

  val createPngDocumentLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
      val currentLoc = (uiState as? LocationDetailUiState.Success)?.location
      if (uri != null && currentLoc != null) {
        scope.launch {
          val ok =
            QrCodeManager.writeStickerToUri(
              context = context,
              targetUri = uri,
              locationName = currentLoc.name,
              qrPayload = QrCodeManager.qrPayloadForLocation(currentLoc),
            )
          snackbarHostState.showSnackbar(if (ok) qrSavedMsg else qrSaveErrorMsg)
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
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(
            text = stringResource(R.string.location_screen_title),
            style = MaterialTheme.typography.titleLarge,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("location_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.back_cd),
            )
          }
        },
        actions = {
          if (uiState is LocationDetailUiState.Success) {
            IconButton(
              onClick = { showRenameDialog = true },
              modifier = Modifier.testTag("rename_location_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.rename_location_cd),
              )
            }
          }
        },
        colors =
          TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground,
          ),
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      when (val state = uiState) {
        LocationDetailUiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          }
        }

        LocationDetailUiState.NotFound -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
              text = stringResource(R.string.qr_not_found_error),
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        is LocationDetailUiState.Success -> {
          val craftPalette = LocalCraftPalette.current
          Column(
            modifier = Modifier.fillMaxSize().widthIn(max = 600.dp),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
              ) {
                Column(modifier = Modifier.padding(16.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier =
                        Modifier.size(8.dp)
                          .clip(CircleShape)
                          .background(craftPalette.brassEyelet)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                      imageVector = Icons.Rounded.Place,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = state.location.name,
                      style = MaterialTheme.typography.headlineMedium,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.testTag("location_title"),
                    )
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = stringResource(R.string.location_items_count, state.items.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                  )
                }
              }

              FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedButton(
                  onClick = { showBatchAddDialog = true },
                  modifier = Modifier.heightIn(min = 48.dp).testTag("batch_add_button"),
                  shape = RoundedCornerShape(14.dp),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = stringResource(R.string.add_multiple_button),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                  )
                }

                OutlinedButton(
                  onClick = { showQrStickerDialog = true },
                  modifier = Modifier.heightIn(min = 48.dp).testTag("qr_sticker_button"),
                  shape = RoundedCornerShape(14.dp),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.QrCode2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = stringResource(R.string.qr_sticker_button),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                  )
                }

                OutlinedButton(
                  onClick = { showMoveBoxDialog = true },
                  modifier = Modifier.heightIn(min = 48.dp).testTag("move_box_button"),
                  shape = RoundedCornerShape(14.dp),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.DriveFileMove,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = stringResource(R.string.move_box_button),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                  )
                }
              }
            }

            if (state.items.isEmpty()) {
              IllustratedEmptyState(
                illustrationRes = R.drawable.img_empty_locations,
                title = stringResource(R.string.location_empty_title),
                subtitle = stringResource(R.string.location_empty_subtitle),
                modifier = Modifier.weight(1f).fillMaxWidth(),
              )
            } else {
              LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("location_items_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                items(items = state.items, key = { it.id }) { item ->
                  ItemListCard(item = item, onClick = { onItemClick(item.id) })
                }
              }
            }
          }

          FloatingBottomActionPill(
            text = stringResource(R.string.add_here_button),
            icon = Icons.Rounded.Add,
            onClick = { onAddHereClick(state.location.id) },
            testTagName = "add_here_button",
            modifier = Modifier.align(Alignment.BottomCenter),
          )

          if (showBatchAddDialog) {
            BatchAddItemsDialog(
              onDismiss = { showBatchAddDialog = false },
              onConfirm = { multilineText ->
                showBatchAddDialog = false
                viewModel.addMultipleItems(multilineText) { count ->
                  if (count > 0) {
                    scope.launch {
                      snackbarHostState.showSnackbar(
                        context.getString(R.string.batch_added_message, count)
                      )
                    }
                  }
                }
              },
            )
          }

          if (showMoveBoxDialog || showRenameDialog) {
            val isMoveBox = showMoveBoxDialog
            EditLocationPathDialog(
              title =
                stringResource(
                  if (isMoveBox) R.string.move_box_dialog_title else R.string.rename_location_title
                ),
              hint = if (isMoveBox) stringResource(R.string.move_box_dialog_hint) else null,
              label = stringResource(R.string.move_box_new_path_label),
              placeholder = stringResource(R.string.move_box_new_path_placeholder),
              initialValue = state.location.name,
              recentLocations = state.recentLocations,
              onDismiss = {
                showMoveBoxDialog = false
                showRenameDialog = false
              },
              onConfirm = { newPath ->
                showMoveBoxDialog = false
                showRenameDialog = false
                viewModel.renameOrMoveBox(newPath)
              },
            )
          }

          if (showQrStickerDialog) {
            QrStickerDialog(
              location = state.location,
              onDismiss = { showQrStickerDialog = false },
              onSavePng = {
                val payload = QrCodeManager.qrPayloadForLocation(state.location)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                  scope.launch {
                    val ok =
                      QrCodeManager.saveStickerToMediaStore(
                        context = context,
                        locationName = state.location.name,
                        qrPayload = payload,
                      )
                    snackbarHostState.showSnackbar(if (ok) qrSavedMsg else qrSaveErrorMsg)
                  }
                } else {
                  createPngDocumentLauncher.launch("qr_${state.location.id}.png")
                }
              },
              onShare = {
                scope.launch {
                  val payload = QrCodeManager.qrPayloadForLocation(state.location)
                  val shareUri =
                    QrCodeManager.createShareableStickerUri(
                      context = context,
                      locationId = state.location.id,
                      locationName = state.location.name,
                      qrPayload = payload,
                    )
                  if (shareUri != null) {
                    val shareIntent =
                      Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, shareUri)
                        putExtra(Intent.EXTRA_TEXT, state.location.name)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                      }
                    context.startActivity(
                      Intent.createChooser(shareIntent, context.getString(R.string.qr_share_chooser))
                    )
                  } else {
                    snackbarHostState.showSnackbar(qrSaveErrorMsg)
                  }
                }
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BatchAddItemsDialog(
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var multilineInput by remember { mutableStateOf("") }
  var hasError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).widthIn(max = 460.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = stringResource(R.string.batch_add_dialog_title),
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = multilineInput,
          onValueChange = {
            multilineInput = it
            hasError = false
          },
          modifier = Modifier.fillMaxWidth().testTag("batch_add_input"),
          label = { Text(stringResource(R.string.batch_add_label)) },
          placeholder = { Text(stringResource(R.string.batch_add_placeholder)) },
          minLines = 4,
          maxLines = 8,
          isError = hasError,
          supportingText =
            if (hasError) {
              { Text(stringResource(R.string.batch_add_error_empty)) }
            } else null,
          shape = RoundedCornerShape(14.dp),
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val nonEmptyCount = multilineInput.lineSequence().count { it.trim().isNotEmpty() }
          if (nonEmptyCount == 0) {
            hasError = true
          } else {
            onConfirm(multilineInput)
          }
        },
        modifier = Modifier.testTag("confirm_batch_add_button"),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
      ) {
        Text(stringResource(R.string.save_button))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.cancel_button))
      }
    },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditLocationPathDialog(
  title: String,
  hint: String?,
  label: String,
  placeholder: String,
  initialValue: String,
  recentLocations: List<String>,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var pathInput by remember(initialValue) { mutableStateOf(initialValue) }
  var hasError by remember { mutableStateOf(false) }

  val currentNorm = ItemRepository.normalizeForSearch(pathInput)
  val suggestions =
    remember(pathInput, recentLocations) {
      recentLocations
        .filter {
          val norm = ItemRepository.normalizeForSearch(it)
          norm != currentNorm && (currentNorm.isEmpty() || norm.contains(currentNorm))
        }
        .take(5)
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).widthIn(max = 460.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (hint != null) {
          Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        OutlinedTextField(
          value = pathInput,
          onValueChange = {
            pathInput = it
            hasError = false
          },
          modifier = Modifier.fillMaxWidth().testTag("location_path_input"),
          label = { Text(label) },
          placeholder = { Text(placeholder) },
          isError = hasError,
          supportingText =
            if (hasError) {
              { Text(stringResource(R.string.error_location_required)) }
            } else null,
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        if (suggestions.isNotEmpty()) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            suggestions.forEach { suggestion ->
              SuggestionChip(
                onClick = {
                  pathInput = suggestion
                  hasError = false
                },
                label = { Text(suggestion, style = MaterialTheme.typography.bodyMedium) },
                colors =
                  SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                  ),
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (pathInput.trim().isEmpty()) {
            hasError = true
          } else {
            onConfirm(pathInput)
          }
        },
        modifier = Modifier.testTag("confirm_location_path_button"),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
      ) {
        Text(stringResource(R.string.save_button))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.cancel_button))
      }
    },
  )
}

@Composable
private fun QrStickerDialog(
  location: LocationEntity,
  onDismiss: () -> Unit,
  onSavePng: () -> Unit,
  onShare: () -> Unit,
) {
  val payload = remember(location.qrCodeId) { QrCodeManager.qrPayloadForLocation(location) }
  val stickerBitmap =
    remember(location.name, payload) {
      QrCodeManager.createPrintableStickerBitmap(location.name, payload)
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).widthIn(max = 460.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = stringResource(R.string.qr_sticker_title),
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = stringResource(R.string.qr_sticker_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Surface(
          shape = RoundedCornerShape(18.dp),
          color = Color.White,
          shadowElevation = 3.dp,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Image(
            bitmap = stickerBitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.qr_image_cd, location.name),
            contentScale = ContentScale.Fit,
            modifier =
              Modifier.fillMaxWidth()
                .heightIn(max = 280.dp)
                .padding(8.dp)
                .background(Color.White)
                .testTag("qr_sticker_image"),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = onSavePng,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("save_qr_png_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.SaveAlt,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = stringResource(R.string.qr_save_png_button),
              style = MaterialTheme.typography.labelMedium,
            )
          }

          Button(
            onClick = onShare,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("share_qr_button"),
            shape = RoundedCornerShape(12.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
          ) {
            Icon(
              imageVector = Icons.Rounded.Share,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = stringResource(R.string.qr_share_button),
              style = MaterialTheme.typography.labelMedium,
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.testTag("close_qr_dialog_button")) {
        Text(stringResource(R.string.cancel_button))
      }
    },
  )
}
