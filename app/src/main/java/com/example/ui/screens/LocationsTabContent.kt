package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.LocationEntity
import com.example.data.LocationSummary
import com.example.data.QrPdfManager
import com.example.ui.LocationsTabState
import com.example.ui.theme.LocalCraftPalette

@Composable
fun LocationsTabBody(
  state: LocationsTabState,
  isPdfSelectionMode: Boolean,
  selectedLocationIds: Set<Long>,
  onClearSearch: () -> Unit,
  onLocationClick: (Long) -> Unit,
  onDeleteEmptyLocationRequest: (LocationSummary) -> Unit,
  onEnterPdfSelectionMode: () -> Unit,
  onExitPdfSelectionMode: () -> Unit,
  onToggleLocationForPdf: (Long) -> Unit,
  onSelectAllVisibleForPdf: (List<Long>) -> Unit,
  onClearSelectionForPdf: () -> Unit,
) {
  when (state) {
    LocationsTabState.Loading -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
      }
    }

    LocationsTabState.Empty -> {
      IllustratedEmptyState(
        illustrationRes = R.drawable.img_empty_locations,
        title = stringResource(R.string.locations_empty_title),
        subtitle = stringResource(R.string.locations_empty_subtitle),
        modifier = Modifier.fillMaxSize().widthIn(max = 600.dp),
      )
    }

    is LocationsTabState.EmptySearchResult -> {
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

    is LocationsTabState.Loaded -> {
      Column(modifier = Modifier.fillMaxSize().widthIn(max = 600.dp)) {
        LocationsActionRow(
          locations = state.locations,
          isPdfSelectionMode = isPdfSelectionMode,
          selectedLocationIds = selectedLocationIds,
          onEnterPdfSelectionMode = onEnterPdfSelectionMode,
          onExitPdfSelectionMode = onExitPdfSelectionMode,
          onSelectAllVisibleForPdf = onSelectAllVisibleForPdf,
          onClearSelectionForPdf = onClearSelectionForPdf,
        )

        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth().testTag("locations_list"),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(items = state.locations, key = { it.id }) { location ->
            val isSelected = location.id in selectedLocationIds
            LocationSummaryCard(
              location = location,
              isPdfSelectionMode = isPdfSelectionMode,
              isSelected = isSelected,
              onClick = {
                if (isPdfSelectionMode) {
                  onToggleLocationForPdf(location.id)
                } else {
                  onLocationClick(location.id)
                }
              },
              onDeleteEmptyClick = { onDeleteEmptyLocationRequest(location) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LocationsActionRow(
  locations: List<LocationSummary>,
  isPdfSelectionMode: Boolean,
  selectedLocationIds: Set<Long>,
  onEnterPdfSelectionMode: () -> Unit,
  onExitPdfSelectionMode: () -> Unit,
  onSelectAllVisibleForPdf: (List<Long>) -> Unit,
  onClearSelectionForPdf: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (!isPdfSelectionMode) {
      OutlinedButton(
        onClick = onEnterPdfSelectionMode,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("pdf_sheet_mode_button"),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      ) {
        Icon(
          imageVector = Icons.Rounded.Print,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.pdf_sheet_mode_button),
          color = MaterialTheme.colorScheme.onSurface,
          style = MaterialTheme.typography.labelLarge,
        )
      }
    } else {
      val visibleIds = locations.map { it.id }
      val allVisibleSelected = visibleIds.isNotEmpty() && visibleIds.all { it in selectedLocationIds }
      Text(
        text = stringResource(R.string.pdf_selected_count, selectedLocationIds.size),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.weight(1f),
      )
      TextButton(
        onClick = {
          if (allVisibleSelected) {
            onClearSelectionForPdf()
          } else {
            onSelectAllVisibleForPdf(visibleIds)
          }
        },
        modifier = Modifier.heightIn(min = 48.dp).testTag("select_all_pdf_button"),
      ) {
        Text(
          text =
            stringResource(
              if (allVisibleSelected) R.string.pdf_deselect_all_button else R.string.pdf_select_all_button
            ),
          style = MaterialTheme.typography.labelLarge,
        )
      }
      TextButton(
        onClick = onExitPdfSelectionMode,
        modifier = Modifier.heightIn(min = 48.dp).testTag("cancel_pdf_selection_button"),
      ) {
        Text(
          text = stringResource(R.string.cancel_button),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}

@Composable
private fun LocationSummaryCard(
  location: LocationSummary,
  isPdfSelectionMode: Boolean,
  isSelected: Boolean,
  onClick: () -> Unit,
  onDeleteEmptyClick: () -> Unit,
) {
  val craftPalette = LocalCraftPalette.current
  val borderColor =
    if (isPdfSelectionMode && isSelected) {
      MaterialTheme.colorScheme.primary
    } else {
      MaterialTheme.colorScheme.outlineVariant
    }

  Surface(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .clickable(onClick = onClick)
        .testTag("location_card_${location.id}"),
    shape = RoundedCornerShape(18.dp),
    color =
      if (isPdfSelectionMode && isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
      } else {
        MaterialTheme.colorScheme.surface
      },
    shadowElevation = 1.dp,
    border = BorderStroke(if (isPdfSelectionMode && isSelected) 1.5.dp else 1.dp, borderColor),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (isPdfSelectionMode) {
        Checkbox(
          checked = isSelected,
          onCheckedChange = { onClick() },
          modifier = Modifier.testTag("location_pdf_checkbox_${location.id}"),
          colors =
            CheckboxDefaults.colors(
              checkedColor = MaterialTheme.colorScheme.primary,
              uncheckedColor = MaterialTheme.colorScheme.outline,
            ),
        )
      } else {
        Surface(
          modifier = Modifier.size(46.dp),
          shape = RoundedCornerShape(13.dp),
          color = craftPalette.paperTint,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Outlined.Place,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp),
            )
          }
        }
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = location.name,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )

        Surface(
          shape = RoundedCornerShape(8.dp),
          color =
            if (location.itemCount == 0) {
              MaterialTheme.colorScheme.surfaceVariant
            } else {
              MaterialTheme.colorScheme.primaryContainer
            },
        ) {
          Text(
            text =
              if (location.itemCount == 0) {
                stringResource(R.string.location_empty_badge)
              } else {
                stringResource(R.string.location_item_count_badge, location.itemCount)
              },
            style = MaterialTheme.typography.labelMedium,
            color =
              if (location.itemCount == 0) {
                MaterialTheme.colorScheme.onSurfaceVariant
              } else {
                MaterialTheme.colorScheme.onPrimaryContainer
              },
            modifier =
              Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("location_count_${location.id}"),
          )
        }
      }

      if (!isPdfSelectionMode) {
        if (location.itemCount == 0) {
          IconButton(
            onClick = onDeleteEmptyClick,
            modifier = Modifier.testTag("delete_empty_location_${location.id}"),
          ) {
            Icon(
              imageVector = Icons.Outlined.DeleteOutline,
              contentDescription = stringResource(R.string.delete_empty_location_cd),
              tint = MaterialTheme.colorScheme.error,
            )
          }
        } else {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
fun CreateLocationDialog(
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var nameInput by remember { mutableStateOf("") }
  var hasError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).widthIn(max = 460.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = stringResource(R.string.create_location_dialog_title),
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      OutlinedTextField(
        value = nameInput,
        onValueChange = {
          nameInput = it
          hasError = false
        },
        modifier = Modifier.fillMaxWidth().testTag("create_location_input"),
        label = { Text(stringResource(R.string.create_location_label)) },
        placeholder = { Text(stringResource(R.string.field_location_placeholder)) },
        isError = hasError,
        supportingText =
          if (hasError) {
            { Text(stringResource(R.string.error_location_required)) }
          } else null,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
      )
    },
    confirmButton = {
      Button(
        onClick = {
          if (nameInput.trim().isEmpty()) {
            hasError = true
          } else {
            onConfirm(nameInput)
          }
        },
        modifier = Modifier.testTag("confirm_create_location_button"),
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
fun DeleteEmptyLocationDialog(
  location: LocationSummary,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).widthIn(max = 460.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = stringResource(R.string.delete_location_dialog_title),
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Text(
        text = stringResource(R.string.delete_location_dialog_message, location.name),
        style = MaterialTheme.typography.bodyLarge,
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        modifier = Modifier.testTag("confirm_delete_location_button"),
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
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.cancel_button))
      }
    },
  )
}

@Composable
fun PdfSheetExportDialog(
  locations: List<LocationEntity>,
  onDismiss: () -> Unit,
  onSavePdf: () -> Unit,
  onSharePdf: () -> Unit,
) {
  val pageCount = remember(locations.size) { QrPdfManager.calculatePageCount(locations.size) }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).widthIn(max = 460.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = stringResource(R.string.pdf_dialog_title),
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = stringResource(R.string.pdf_dialog_summary, locations.size, pageCount),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag("pdf_summary_text"),
        )

        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.background,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            locations.take(6).forEach { loc ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.QrCode2,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = loc.name,
                  style = MaterialTheme.typography.bodyMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            if (locations.size > 6) {
              Text(
                text = "+ ещё ${locations.size - 6}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = onSavePdf,
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("save_pdf_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.SaveAlt,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.pdf_save_button),
              style = MaterialTheme.typography.labelLarge,
            )
          }

          Button(
            onClick = onSharePdf,
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("share_pdf_button"),
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
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.pdf_share_button),
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.testTag("close_pdf_dialog_button")) {
        Text(stringResource(R.string.cancel_button))
      }
    },
  )
}
