package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoStorageManager(private val context: Context) {

  private val photosDir: File
    get() = File(context.filesDir, PHOTOS_DIR_NAME).apply { if (!exists()) mkdirs() }

  private val cameraTempDir: File
    get() = File(context.cacheDir, CAMERA_TEMP_DIR_NAME).apply { if (!exists()) mkdirs() }

  fun createTempCameraCaptureUri(): Uri {
    val tempFile = File(cameraTempDir, "capture_${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
  }

  suspend fun savePhotoCopyFromUri(sourceUri: Uri): String? =
    withContext(Dispatchers.IO) {
      val rawBytes =
        runCatching {
            if (sourceUri.scheme == "file" && sourceUri.path != null) {
              File(sourceUri.path!!).readBytes()
            } else {
              context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
            }
          }
          .getOrNull() ?: return@withContext null

      if (rawBytes.isEmpty()) return@withContext null

      val targetFile = File(photosDir, "item_${UUID.randomUUID()}.jpg")

      val decodedAndCompressed =
        runCatching {
            val orientation =
              ByteArrayInputStream(rawBytes).use { exifStream ->
                ExifInterface(exifStream)
                  .getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                  )
              }

            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)

            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
              return@runCatching false
            }

            val sampleSize =
              calculateInSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, MAX_DIMENSION_PX)
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decodedBitmap =
              BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
                ?: return@runCatching false

            val rotatedBitmap = applyExifOrientation(decodedBitmap, orientation)
            if (rotatedBitmap !== decodedBitmap) {
              decodedBitmap.recycle()
            }

            FileOutputStream(targetFile).use { out ->
              rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            rotatedBitmap.recycle()
            true
          }
          .getOrDefault(false)

      if (!decodedAndCompressed) {
        runCatching { targetFile.writeBytes(rawBytes) }.getOrElse { return@withContext null }
      }

      targetFile.absolutePath
    }

  suspend fun deletePhotoFile(photoPath: String?) =
    withContext(Dispatchers.IO) {
      if (photoPath.isNullOrBlank()) return@withContext
      val file = File(photoPath)
      val photosCanonical = runCatching { photosDir.canonicalPath }.getOrNull() ?: return@withContext
      val fileCanonical = runCatching { file.canonicalPath }.getOrNull() ?: return@withContext
      if (fileCanonical.startsWith(photosCanonical) && file.exists()) {
        file.delete()
      }
    }

  suspend fun clearTempCameraUri(uri: Uri?) =
    withContext(Dispatchers.IO) {
      if (uri == null) return@withContext
      val lastSegment = uri.lastPathSegment ?: return@withContext
      val candidate = File(cameraTempDir, File(lastSegment).name)
      if (candidate.exists()) {
        candidate.delete()
      }
    }

  private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
    var inSampleSize = 1
    while ((width / inSampleSize) > maxDim * 2 || (height / inSampleSize) > maxDim * 2) {
      inSampleSize *= 2
    }
    return inSampleSize
  }

  private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
      ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
      ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
      ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
      ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
      ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
      else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }

  companion object {
    private const val PHOTOS_DIR_NAME = "item_photos"
    private const val CAMERA_TEMP_DIR_NAME = "camera_captures"
    private const val MAX_DIMENSION_PX = 1600
    private const val JPEG_QUALITY = 85
  }
}
