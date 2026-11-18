package com.example.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QrCodeManager {
  private const val QR_PREFIX = "gdelezhit://loc/"
  private const val STICKERS_CACHE_DIR = "qr_stickers"

  fun qrPayloadForLocation(location: LocationEntity): String = "$QR_PREFIX${location.qrCodeId}"

  fun parseQrCodeId(rawPayload: String?): String? {
    val trimmed = rawPayload?.trim() ?: return null
    if (trimmed.startsWith(QR_PREFIX, ignoreCase = true)) {
      val id = trimmed.substring(QR_PREFIX.length).trim()
      return id.takeIf { it.isNotEmpty() }
    }
    return null
  }

  fun generateQrBitmap(payload: String, sizePx: Int = 640): Bitmap {
    val hints =
      mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 1,
        EncodeHintType.CHARACTER_SET to "UTF-8",
      )
    val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val width = matrix.width
    val height = matrix.height
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
      val offset = y * width
      for (x in 0 until width) {
        pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
      }
    }
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
      setPixels(pixels, 0, width, 0, 0, width, height)
    }
  }

  fun createPrintableStickerBitmap(locationName: String, qrPayload: String): Bitmap {
    val width = 1080
    val qrSize = 640
    val horizontalMargin = 88f

    val titlePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F241E")
        textSize = 52f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }

    val textWidth = (width - horizontalMargin * 2).toInt()
    val titleLayout =
      StaticLayout.Builder.obtain(locationName, 0, locationName.length, titlePaint, textWidth)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setLineSpacing(6f, 1.0f)
        .setMaxLines(3)
        .build()

    val topPadding = 64
    val headerPillHeight = 76
    val headerToQrSpacing = 54
    val qrBoxPadding = 28
    val qrFrameSize = qrSize + qrBoxPadding * 2
    val qrToDividerSpacing = 44
    val dividerToTextSpacing = 36
    val textToCaptionSpacing = 22
    val captionHeight = 32
    val bottomPadding = 64
    val height =
      topPadding +
        headerPillHeight +
        headerToQrSpacing +
        qrFrameSize +
        qrToDividerSpacing +
        dividerToTextSpacing +
        titleLayout.height +
        textToCaptionSpacing +
        captionHeight +
        bottomPadding

    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawColor(Color.WHITE)

    val cutBorderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.parseColor("#C4BCA9")
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(22f, 16f), 0f)
      }
    val outerRect = RectF(28f, 28f, (width - 28).toFloat(), (height - 28).toFloat())
    canvas.drawRoundRect(outerRect, 48f, 48f, cutBorderPaint)

    val pillWidth = 440f
    val pillLeft = (width - pillWidth) / 2f
    val pillTop = topPadding.toFloat()
    val pillRect = RectF(pillLeft, pillTop, pillLeft + pillWidth, pillTop + headerPillHeight)
    val pillPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#2E6F40")
      }
    canvas.drawRoundRect(pillRect, 38f, 38f, pillPaint)

    val eyeletPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FBF7EF")
      }
    canvas.drawCircle(pillLeft + 34f, pillTop + headerPillHeight / 2f, 8f, eyeletPaint)
    canvas.drawCircle(
      pillLeft + pillWidth - 34f,
      pillTop + headerPillHeight / 2f,
      8f,
      eyeletPaint,
    )

    val headerTextPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 31f
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
    val headerBaseline = pillTop + headerPillHeight / 2f + 11f
    canvas.drawText("ГДЕ ЛЕЖИТ?", width / 2f, headerBaseline, headerTextPaint)

    val frameTop = (topPadding + headerPillHeight + headerToQrSpacing).toFloat()
    val frameLeft = (width - qrFrameSize) / 2f
    val frameRight = frameLeft + qrFrameSize
    val frameBottom = frameTop + qrFrameSize

    val bracketPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 9f
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#2E6F40")
      }
    val arm = 54f
    // Top-left bracket
    canvas.drawLine(frameLeft, frameTop + arm, frameLeft, frameTop, bracketPaint)
    canvas.drawLine(frameLeft, frameTop, frameLeft + arm, frameTop, bracketPaint)
    // Top-right bracket
    canvas.drawLine(frameRight - arm, frameTop, frameRight, frameTop, bracketPaint)
    canvas.drawLine(frameRight, frameTop, frameRight, frameTop + arm, bracketPaint)
    // Bottom-left bracket
    canvas.drawLine(frameLeft, frameBottom - arm, frameLeft, frameBottom, bracketPaint)
    canvas.drawLine(frameLeft, frameBottom, frameLeft + arm, frameBottom, bracketPaint)
    // Bottom-right bracket
    canvas.drawLine(frameRight - arm, frameBottom, frameRight, frameBottom, bracketPaint)
    canvas.drawLine(frameRight, frameBottom - arm, frameRight, frameBottom, bracketPaint)

    val qrBitmap = generateQrBitmap(qrPayload, qrSize)
    canvas.drawBitmap(qrBitmap, frameLeft + qrBoxPadding, frameTop + qrBoxPadding, null)
    qrBitmap.recycle()

    val dividerY = frameBottom + qrToDividerSpacing
    val dividerPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#E2D8C5")
      }
    canvas.drawLine(140f, dividerY, (width - 140).toFloat(), dividerY, dividerPaint)

    val titleTop = dividerY + dividerToTextSpacing
    canvas.save()
    canvas.translate(horizontalMargin, titleTop)
    titleLayout.draw(canvas)
    canvas.restore()

    val captionPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5E6156")
        textSize = 26f
        textAlign = Paint.Align.CENTER
      }
    val captionY = titleTop + titleLayout.height + textToCaptionSpacing + 22f
    canvas.drawText("Отсканируйте код в приложении", width / 2f, captionY, captionPaint)

    return output
  }

  suspend fun saveStickerToMediaStore(
    context: Context,
    locationName: String,
    qrPayload: String,
  ): Boolean =
    withContext(Dispatchers.IO) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext false
      val stickerBitmap = createPrintableStickerBitmap(locationName, qrPayload)
      try {
        val fileName = "qr_${System.currentTimeMillis()}.png"
        val values =
          ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
              MediaStore.Images.Media.RELATIVE_PATH,
              "${Environment.DIRECTORY_PICTURES}/Где лежит",
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
          }
        val resolver = context.contentResolver
        val uri =
          resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext false

        val written =
          resolver.openOutputStream(uri)?.use { out ->
            stickerBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
          } ?: false

        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        written
      } finally {
        stickerBitmap.recycle()
      }
    }

  suspend fun writeStickerToUri(
    context: Context,
    targetUri: Uri,
    locationName: String,
    qrPayload: String,
  ): Boolean =
    withContext(Dispatchers.IO) {
      val stickerBitmap = createPrintableStickerBitmap(locationName, qrPayload)
      try {
        context.contentResolver.openOutputStream(targetUri)?.use { out ->
          stickerBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } ?: false
      } finally {
        stickerBitmap.recycle()
      }
    }

  suspend fun createShareableStickerUri(
    context: Context,
    locationId: Long,
    locationName: String,
    qrPayload: String,
  ): Uri? =
    withContext(Dispatchers.IO) {
      val dir = File(context.cacheDir, STICKERS_CACHE_DIR).apply { if (!exists()) mkdirs() }
      val file = File(dir, "qr_location_$locationId.png")
      val stickerBitmap = createPrintableStickerBitmap(locationName, qrPayload)
      try {
        val ok = FileOutputStream(file).use { out ->
          stickerBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        if (!ok) return@withContext null
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
      } finally {
        stickerBitmap.recycle()
      }
    }

  fun decodeQrFromBitmap(bitmap: Bitmap): String? {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) return null
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val source = RGBLuminanceSource(width, height, pixels)
    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
    val hints =
      mapOf(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
      )
    return runCatching { MultiFormatReader().decode(binaryBitmap, hints).text }.getOrNull()
  }

  suspend fun decodeQrFromUri(context: Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
      val bytes =
        runCatching {
            if (uri.scheme == "file" && uri.path != null) {
              File(uri.path!!).readBytes()
            } else {
              context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
          }
          .getOrNull() ?: return@withContext null

      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
      var sampleSize = 1
      while (bounds.outWidth / sampleSize > 1600 || bounds.outHeight / sampleSize > 1600) {
        sampleSize *= 2
      }
      val bitmap =
        BitmapFactory.decodeByteArray(
          bytes,
          0,
          bytes.size,
          BitmapFactory.Options().apply { inSampleSize = sampleSize },
        ) ?: return@withContext null

      try {
        decodeQrFromBitmap(bitmap)
      } finally {
        bitmap.recycle()
      }
    }
}
