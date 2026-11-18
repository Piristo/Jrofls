package com.example.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.PhotoStorageManager
import com.example.ui.AddEditItemViewModel
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditItemScreen(
  viewModel: AddEditItemViewModel,
  photoStorageManager: PhotoStorageManager,
  onNavigateBack: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

  val cameraUnavailableMessage = stringResource(R.string.error_camera_unavailable)
  val cameraPermissionDeniedMessage = stringResource(R.string.error_camera_permission)

  val photoPickerLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        viewModel.onPhotoSelected(uri)
      }
    }

  val takePictureLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success ->
      val capturedUri = pendingCameraUri
      if (success && capturedUri != null) {
        viewModel.onPhotoSelected(capturedUri)
      }
    }

  val launchCamera = {
    try {
      val captureUri = photoStorageManager.createTempCameraCaptureUri()
      pendingCameraUri = captureUri
      takePictureLauncher.launch(captureUri)
    } catch (_: ActivityNotFoundException) {
      scope.launch { snackbarHostState.showSnackbar(cameraUnavailableMessage) }
    }
  }

  val cameraPermissionLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
      granted ->
      if (granted) {
        launchCamera()
      } else {
        scope.launch { snackbarHostState.showSnackbar(cameraPermissionDeniedMessage) }
      }
    }

  val fieldColors =
    OutlinedTextFieldDefaults.colors(
      focusedContainerColor = MaterialTheme.colorScheme.surface,
      unfocusedContainerColor = MaterialTheme.colorScheme.surface,
      focusedBorderColor = MaterialTheme.colorScheme.primary,
      unfocusedBorderColor = MaterialTheme.colorScheme.outline,
      errorContainerColor = MaterialTheme.colorScheme.surface,
    )

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(
            text =
              stringResource(
                if (uiState.isEditing) R.string.edit_screen_title else R.string.add_screen_title
              ),
            style = MaterialTheme.typography.titleLarge,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("back_button"),
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
      Surface(color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding()) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
          ) {
            Button(
              onClick = { viewModel.save(onSaved = onNavigateBack) },
              enabled = !uiState.isSaving && uiState.isLoaded,
              modifier =
                Modifier.fillMaxWidth()
                  .widthIn(max = 600.dp)
                  .heightIn(min = 56.dp)
                  .testTag("save_button"),
              shape = RoundedCornerShape(16.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
              if (uiState.isSaving) {
                CircularProgressIndicator(
                  modifier = Modifier.size(22.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.5.dp,
                )
              } else {
                Text(
                  text = stringResource(R.string.save_button),
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
      if (!uiState.isLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
      } else {
        Column(
          modifier =
            Modifier.fillMaxSize()
              .widthIn(max = 600.dp)
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::onNameChange,
            modifier = Modifier.fillMaxWidth().testTag("name_input"),
            label = { Text(stringResource(R.string.field_name_label)) },
            placeholder = { Text(stringResource(R.string.field_name_placeholder)) },
            isError = uiState.nameError,
            supportingText =
              if (uiState.nameError) {
                { Text(stringResource(R.string.error_name_required)) }
              } else null,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(16.dp),
            colors = fieldColors,
            keyboardOptions =
              KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
              ),
          )

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = uiState.location,
              onValueChange = viewModel::onLocationChange,
              modifier = Modifier.fillMaxWidth().testTag("location_input"),
              label = { Text(stringResource(R.string.field_location_label)) },
              placeholder = { Text(stringResource(R.string.field_location_placeholder)) },
              isError = uiState.locationError,
              supportingText =
                if (uiState.locationError) {
                  { Text(stringResource(R.string.error_location_required)) }
                } else null,
              singleLine = true,
              textStyle = MaterialTheme.typography.bodyLarge,
              shape = RoundedCornerShape(16.dp),
              colors = fieldColors,
              keyboardOptions =
                KeyboardOptions(
                  capitalization = KeyboardCapitalization.Sentences,
                  imeAction = ImeAction.Next,
                ),
            )

            val suggestions = uiState.filteredLocationSuggestions
            if (suggestions.isNotEmpty()) {
              Text(
                text = stringResource(R.string.location_suggestions_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                suggestions.forEach { suggestion ->
                  SuggestionChip(
                    onClick = { viewModel.onLocationSuggestionClick(suggestion) },
                    label = {
                      Text(text = suggestion, style = MaterialTheme.typography.bodyMedium)
                    },
                    colors =
                      SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                      ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                  )
                }
              }
            }
          }

          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = stringResource(R.string.photo_section_label),
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onBackground,
            )

            val activeModel =
              when (val raw = uiState.activePhotoModel) {
                is String -> File(raw).takeIf { it.exists() }
                else -> raw
              }

            if (activeModel != null) {
              Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                color = MaterialTheme.colorScheme.surface,
              ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                  AsyncImage(
                    model = activeModel,
                    contentDescription = stringResource(R.string.item_photo_cd, uiState.name),
                    contentScale = ContentScale.Crop,
                    modifier =
                      Modifier.fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("form_photo_preview"),
                  )
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                  ) {
                    TextButton(
                      onClick = viewModel::onRemovePhoto,
                      modifier = Modifier.testTag("remove_photo_button"),
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = stringResource(R.string.remove_photo_button),
                        color = MaterialTheme.colorScheme.error,
                      )
                    }
                  }
                }
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              OutlinedButton(
                onClick = {
                  val granted =
                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                      PackageManager.PERMISSION_GRANTED
                  if (granted) {
                    launchCamera()
                  } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                  }
                },
                modifier = Modifier.weight(1f).heightIn(min = 52.dp).testTag("camera_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              ) {
                Icon(
                  imageVector = Icons.Outlined.PhotoCamera,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = stringResource(R.string.take_photo_button),
                  color = MaterialTheme.colorScheme.onSurface,
                  style = MaterialTheme.typography.labelLarge,
                )
              }

              OutlinedButton(
                onClick = {
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
                modifier = Modifier.weight(1f).heightIn(min = 52.dp).testTag("gallery_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              ) {
                Icon(
                  imageVector = Icons.Outlined.PhotoLibrary,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = stringResource(R.string.pick_photo_button),
                  color = MaterialTheme.colorScheme.onSurface,
                  style = MaterialTheme.typography.labelLarge,
                )
              }
            }
          }

          OutlinedTextField(
            value = uiState.note,
            onValueChange = viewModel::onNoteChange,
            modifier = Modifier.fillMaxWidth().testTag("note_input"),
            label = { Text(stringResource(R.string.field_note_label)) },
            placeholder = { Text(stringResource(R.string.field_note_placeholder)) },
            minLines = 3,
            maxLines = 5,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(16.dp),
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
          )

          Spacer(modifier = Modifier.height(12.dp))
        }
      }
    }
  }
}
