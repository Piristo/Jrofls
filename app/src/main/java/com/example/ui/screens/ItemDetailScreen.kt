package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.ItemDetailUiState
import com.example.ui.ItemDetailViewModel
import com.example.ui.theme.LocalCraftPalette
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
  viewModel: ItemDetailViewModel,
  onNavigateBack: () -> Unit,
  onEditClick: (Long) -> Unit,
  onLocationClick: (Long) -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var showMoveDialog by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(
            text = stringResource(R.string.detail_screen_title),
            style = MaterialTheme.typography.titleLarge,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("detail_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.back_cd),
            )
          }
        },
        colors =
          TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
          ),
      )
    },
    bottomBar = {
      val state = uiState as? ItemDetailUiState.Success ?: return@Scaffold
      Surface(color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .widthIn(max = 600.dp)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = { showMoveDialog = true },
              modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("move_button"),
              shape = RoundedCornerShape(16.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
              Icon(
                imageVector = Icons.Rounded.SwapHoriz,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(R.string.move_action),
                style = MaterialTheme.typography.labelLarge,
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              OutlinedButton(
                onClick = { onEditClick(state.item.id) },
                modifier = Modifier.weight(1f).heightIn(min = 50.dp).testTag("edit_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Edit,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                  tint = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = stringResource(R.string.edit_action),
                  color = MaterialTheme.colorScheme.onSurface,
                  style = MaterialTheme.typography.labelLarge,
                )
              }

              OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.weight(1f).heightIn(min = 50.dp).testTag("delete_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
              ) {
                Icon(
                  imageVector = Icons.Outlined.DeleteOutline,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                  tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = stringResource(R.string.delete_action),
                  color = MaterialTheme.colorScheme.error,
                  style = MaterialTheme.typography.labelLarge,
                )
              }
            }
          }
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      when (val state = uiState) {
        ItemDetailUiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          }
        }

        ItemDetailUiState.NotFound -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
              text = stringResource(R.string.item_not_found),
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        is ItemDetailUiState.Success -> {
          val item = state.item
          val photoFile = item.photoPath?.let(::File)?.takeIf { it.exists() }
          val craftPalette = LocalCraftPalette.current
          val formattedDate =
            remember(item.updatedAt) {
              SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.forLanguageTag("ru-RU"))
                .format(Date(item.updatedAt))
            }

          Column(
            modifier =
              Modifier.fillMaxSize()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            if (photoFile != null) {
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 1.dp,
                color = MaterialTheme.colorScheme.surface,
              ) {
                AsyncImage(
                  model = photoFile,
                  contentDescription = stringResource(R.string.item_photo_cd, item.name),
                  contentScale = ContentScale.Crop,
                  modifier =
                    Modifier.fillMaxWidth()
                      .height(250.dp)
                      .clip(RoundedCornerShape(22.dp))
                      .background(MaterialTheme.colorScheme.surfaceVariant)
                      .testTag("detail_photo"),
                )
              }
            }

            Text(
              text = item.name,
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.testTag("detail_name"),
            )

            Surface(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(18.dp))
                  .clickable { onLocationClick(item.locationId) }
                  .testTag("detail_location_card"),
              shape = RoundedCornerShape(18.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier =
                        Modifier.size(7.dp)
                          .clip(CircleShape)
                          .background(craftPalette.brassEyelet)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = stringResource(R.string.detail_location_label),
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Rounded.Place,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = item.location,
                      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.testTag("detail_location"),
                    )
                  }
                }
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }

            if (item.note.isNotBlank()) {
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              ) {
                Column(modifier = Modifier.padding(16.dp)) {
                  Text(
                    text = stringResource(R.string.detail_note_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = item.note,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("detail_note"),
                  )
                }
              }
            }

            ItemHistorySection(history = state.history)

            Text(
              text = "${stringResource(R.string.detail_updated_label)}: $formattedDate",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          if (showMoveDialog) {
            MoveLocationDialog(
              initialLocation = item.location,
              recentLocations = state.recentLocations,
              onDismiss = { showMoveDialog = false },
              onConfirm = { newLocation ->
                showMoveDialog = false
                viewModel.moveItem(newLocation)
              },
            )
          }

          if (showDeleteDialog) {
            AlertDialog(
              onDismissRequest = { showDeleteDialog = false },
              properties = DialogProperties(usePlatformDefaultWidth = false),
              modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).widthIn(max = 460.dp),
              containerColor = MaterialTheme.colorScheme.surface,
              title = {
                Text(
                  text = stringResource(R.string.delete_dialog_title),
                  style = MaterialTheme.typography.titleLarge,
                )
              },
              text = {
                Text(
                  text = stringResource(R.string.delete_dialog_message, item.name),
                  style = MaterialTheme.typography.bodyLarge,
                )
              },
              confirmButton = {
                Button(
                  onClick = {
                    showDeleteDialog = false
                    viewModel.deleteItem(item, onDeleted = onNavigateBack)
                  },
                  modifier = Modifier.testTag("confirm_delete_button"),
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.error,
                      contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                  Text(stringResource(R.string.confirm_delete_button))
                }
              },
              dismissButton = {
                TextButton(
                  onClick = { showDeleteDialog = false },
                  modifier = Modifier.testTag("cancel_delete_button"),
                ) {
                  Text(stringResource(R.string.cancel_button))
                }
              },
            )
          }
        }
      }
    }
  }
}
