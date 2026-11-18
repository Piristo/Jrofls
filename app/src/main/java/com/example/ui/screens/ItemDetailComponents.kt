package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.HistoryEventType
import com.example.data.ItemHistoryEntity
import com.example.data.ItemRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ItemHistorySection(history: List<ItemHistoryEntity>) {
  if (history.isEmpty()) return

  val dateFormatter =
    remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag("ru-RU")) }

  Surface(
    modifier = Modifier.fillMaxWidth().testTag("item_history_section"),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Rounded.History,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.history_section_title),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        history.forEachIndexed { index, entry ->
          val isLatest = index == 0
          val hasNext = index < history.lastIndex
          val eventText =
            when (entry.eventType) {
              HistoryEventType.MOVED ->
                stringResource(
                  R.string.history_moved_event,
                  entry.fromLocation.orEmpty(),
                  entry.toLocation,
                )
              HistoryEventType.BOX_RENAMED ->
                stringResource(
                  R.string.history_box_renamed_event,
                  entry.fromLocation.orEmpty(),
                  entry.toLocation,
                )
              else -> stringResource(R.string.history_created_event, entry.toLocation)
            }
          val formattedDate =
            remember(entry.timestamp) { dateFormatter.format(Date(entry.timestamp)) }

          Row(
            modifier = Modifier.fillMaxWidth().testTag("history_entry_$index"),
            verticalAlignment = Alignment.Top,
          ) {
            Column(
              modifier = Modifier.width(20.dp).padding(top = 5.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Box(
                modifier =
                  Modifier.size(10.dp)
                    .clip(CircleShape)
                    .then(
                      if (isLatest) {
                        Modifier.background(MaterialTheme.colorScheme.primary)
                      } else {
                        Modifier.border(
                          width = 2.dp,
                          color = MaterialTheme.colorScheme.outline,
                          shape = CircleShape,
                        )
                      }
                    )
              )
              if (hasNext) {
                Box(
                  modifier =
                    Modifier.padding(top = 4.dp)
                      .width(2.dp)
                      .height(24.dp)
                      .background(MaterialTheme.colorScheme.outlineVariant)
                )
              }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = eventText,
                style =
                  MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isLatest) FontWeight.SemiBold else FontWeight.Normal
                  ),
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoveLocationDialog(
  initialLocation: String,
  recentLocations: List<String>,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var locationInput by remember(initialLocation) { mutableStateOf(initialLocation) }
  var hasError by remember { mutableStateOf(false) }

  val currentNorm = ItemRepository.normalizeForSearch(locationInput)
  val suggestions =
    remember(locationInput, recentLocations) {
      recentLocations
        .filter {
          val norm = ItemRepository.normalizeForSearch(it)
          norm != currentNorm && (currentNorm.isEmpty() || norm.contains(currentNorm))
        }
        .take(6)
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).widthIn(max = 460.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = stringResource(R.string.move_dialog_title),
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = locationInput,
          onValueChange = {
            locationInput = it
            hasError = false
          },
          modifier = Modifier.fillMaxWidth().testTag("move_location_input"),
          label = { Text(stringResource(R.string.move_dialog_new_location_label)) },
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

        if (suggestions.isNotEmpty()) {
          Text(
            text = stringResource(R.string.location_suggestions_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            suggestions.forEach { suggestion ->
              SuggestionChip(
                onClick = {
                  locationInput = suggestion
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
          if (locationInput.trim().isEmpty()) {
            hasError = true
          } else {
            onConfirm(locationInput)
          }
        },
        modifier = Modifier.testTag("confirm_move_button"),
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
      TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_move_button")) {
        Text(stringResource(R.string.cancel_button))
      }
    },
  )
}
