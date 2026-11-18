package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.QrCodeManager
import com.example.ui.QrScanUiState
import com.example.ui.QrScannerViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
  viewModel: QrScannerViewModel,
  onNavigateBack: () -> Unit,
  onLocationResolved: (Long) -> Unit,
) {
  val scanState by viewModel.scanState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
      granted ->
      hasCameraPermission = granted
    }

  val galleryQrLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          val decoded = QrCodeManager.decodeQrFromUri(context, uri)
          viewModel.onQrPayloadDetected(decoded.orEmpty())
        }
      }
    }

  LaunchedEffect(Unit) {
    if (!hasCameraPermission) {
      permissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  LaunchedEffect(scanState) {
    val resolved = scanState as? QrScanUiState.Resolved
    if (resolved != null) {
      onLocationResolved(resolved.locationId)
    }
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(
            text = stringResource(R.string.qr_scanner_title),
            style = MaterialTheme.typography.titleLarge,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("scanner_back_button"),
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
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .navigationBarsPadding()
              .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          OutlinedButton(
            onClick = {
              galleryQrLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier =
              Modifier.fillMaxWidth()
                .widthIn(max = 600.dp)
                .heightIn(min = 52.dp)
                .testTag("pick_qr_image_button"),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          ) {
            Icon(
              imageVector = Icons.Outlined.PhotoLibrary,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.qr_scanner_from_gallery),
              color = MaterialTheme.colorScheme.onSurface,
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
      contentAlignment = Alignment.Center,
    ) {
      when (scanState) {
        is QrScanUiState.NotFound -> {
          Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.errorContainer,
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp),
                  )
                }
              }
              Text(
                text = stringResource(R.string.qr_not_found_error),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("qr_not_found_text"),
              )
              Button(
                onClick = viewModel::resetScan,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("qr_retry_button"),
                shape = RoundedCornerShape(14.dp),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                  ),
              ) {
                Text(stringResource(R.string.qr_retry_button))
              }
            }
          }
        }

        QrScanUiState.Scanning,
        is QrScanUiState.Resolved -> {
          Column(
            modifier = Modifier.fillMaxSize().widthIn(max = 600.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            Text(
              text = stringResource(R.string.qr_scanner_hint),
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )

            if (hasCameraPermission) {
              CameraQrPreview(
                modifier =
                  Modifier.weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("qr_camera_preview"),
                onQrDetected = viewModel::onQrPayloadDetected,
              )
            } else {
              Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              ) {
                Column(
                  modifier = Modifier.fillMaxSize().padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                  )
                  Spacer(modifier = Modifier.height(16.dp))
                  Text(
                    text = stringResource(R.string.qr_scanner_permission_needed),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                  )
                  Spacer(modifier = Modifier.height(16.dp))
                  Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.heightIn(min = 50.dp).testTag("grant_camera_button"),
                    shape = RoundedCornerShape(14.dp),
                  ) {
                    Text(stringResource(R.string.qr_scanner_grant_permission))
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraQrPreview(
  modifier: Modifier = Modifier,
  onQrDetected: (String) -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
  val scanner = remember {
    val options =
      BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    BarcodeScanning.getClient(options)
  }

  DisposableEffect(Unit) {
    onDispose {
      scanner.close()
      analysisExecutor.shutdown()
    }
  }

  AndroidView(
    modifier = modifier,
    factory = { ctx ->
      val previewView = PreviewView(ctx)
      val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
      cameraProviderFuture.addListener(
        {
          val cameraProvider = runCatching { cameraProviderFuture.get() }.getOrNull() ?: return@addListener
          val preview =
            Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }

          val imageAnalysis =
            ImageAnalysis.Builder()
              .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
              .build()

          imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
              val inputImage =
                InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
              scanner
                .process(inputImage)
                .addOnSuccessListener { barcodes ->
                  val rawValue = barcodes.firstNotNullOfOrNull { it.rawValue }
                  if (rawValue != null) {
                    onQrDetected(rawValue)
                  }
                }
                .addOnCompleteListener { imageProxy.close() }
            } else {
              imageProxy.close()
            }
          }

          runCatching {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
              lifecycleOwner,
              CameraSelector.DEFAULT_BACK_CAMERA,
              preview,
              imageAnalysis,
            )
          }
        },
        ContextCompat.getMainExecutor(ctx),
      )
      previewView
    },
  )
}
