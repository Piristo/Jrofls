package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QrPdfManager {
  const val PAGE_WIDTH_PT = 595
  const val PAGE_HEIGHT_PT = 842
  const val COLUMNS = 2
  const val ROWS = 4
  const val STICKERS_PER_PAGE = COLUMNS * ROWS
  private const val STICKERS_CACHE_DIR = "qr_stickers"

  fun calculatePageCount(stickerCount: Int): Int {
    if (stickerCount <= 0) return 0
    return (stickerCount + STICKERS_PER_PAGE - 1) / STICKERS_PER_PAGE
  }

  fun generateQrSheetPdf(
    locations: List<LocationEntity>,
    outputStream: OutputStream,
  ): Boolean {
    if (locations.isEmpty()) return false
    val pages = locations.chunked(STICKERS_PER_PAGE)

    if (tryGenerateWithAndroidPdfDocument(pages, outputStream)) {
      return true
    }
    return generateFallbackRasterPdf(pages, outputStream)
  }

  private fun tryGenerateWithAndroidPdfDocument(
    pages: List<List<LocationEntity>>,
    outputStream: OutputStream,
  ): Boolean {
    val pdfDocument = PdfDocument()
    return try {
      pages.forEachIndexed { pageIndex, pageLocations ->
        val pageInfo =
          PdfDocument.PageInfo.Builder(PAGE_WIDTH_PT, PAGE_HEIGHT_PT, pageIndex + 1).create()
        val page = pdfDocument.startPage(pageInfo)
        drawStickersPage(page.canvas, pageLocations)
        pdfDocument.finishPage(page)
      }
      pdfDocument.writeTo(outputStream)
      outputStream.flush()
      true
    } catch (_: Throwable) {
      false
    } finally {
      runCatching { pdfDocument.close() }
    }
  }

  private fun generateFallbackRasterPdf(
    pages: List<List<LocationEntity>>,
    outputStream: OutputStream,
  ): Boolean {
    return try {
      val dpiScale = 2f
      val widthPx = (PAGE_WIDTH_PT * dpiScale).toInt()
      val heightPx = (PAGE_HEIGHT_PT * dpiScale).toInt()

      val pageJpegs =
        pages.map { pageLocations ->
          val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
          try {
            val canvas = Canvas(bitmap)
            canvas.scale(dpiScale, dpiScale)
            drawStickersPage(canvas, pageLocations)
            ByteArrayOutputStream().use { jpegOut ->
              bitmap.compress(Bitmap.CompressFormat.JPEG, 90, jpegOut)
              jpegOut.toByteArray()
            }
          } finally {
            bitmap.recycle()
          }
        }

      writeMultiPageJpegPdf(
        pageJpegs = pageJpegs,
        imageWidthPx = widthPx,
        imageHeightPx = heightPx,
        outputStream = outputStream,
      )
      true
    } catch (_: Throwable) {
      false
    }
  }

  private fun drawStickersPage(canvas: Canvas, pageLocations: List<LocationEntity>) {
    val pageMargin = 24f
    val gap = 12f
    val cellWidth = (PAGE_WIDTH_PT - pageMargin * 2f - gap * (COLUMNS - 1)) / COLUMNS
    val cellHeight = (PAGE_HEIGHT_PT - pageMargin * 2f - gap * (ROWS - 1)) / ROWS

    val cutBorderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.0f
        color = Color.parseColor("#C4BCA9")
        pathEffect = DashPathEffect(floatArrayOf(6f, 5f), 0f)
      }

    val innerCardPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
      }

    val headerPillPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#2E6F40")
      }

    val eyeletPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FBF7EF")
      }

    val headerTextPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 7.2f
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.06f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }

    val bracketPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.8f
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#2E6F40")
      }

    val dividerPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.8f
        color = Color.parseColor("#E2D8C5")
      }

    val titlePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F241E")
        textSize = 11.0f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }

    canvas.drawColor(Color.WHITE)

    pageLocations.forEachIndexed { slotIndex, location ->
      val col = slotIndex % COLUMNS
      val row = slotIndex / COLUMNS
      val left = pageMargin + col * (cellWidth + gap)
      val top = pageMargin + row * (cellHeight + gap)
      val rect = RectF(left, top, left + cellWidth, top + cellHeight)

      canvas.drawRoundRect(rect, 10f, 10f, innerCardPaint)
      canvas.drawRoundRect(rect, 10f, 10f, cutBorderPaint)

      val pillWidth = 104f
      val pillHeight = 16f
      val pillLeft = left + (cellWidth - pillWidth) / 2f
      val pillTop = top + 9f
      canvas.drawRoundRect(
        RectF(pillLeft, pillTop, pillLeft + pillWidth, pillTop + pillHeight),
        8f,
        8f,
        headerPillPaint,
      )
      canvas.drawCircle(pillLeft + 8f, pillTop + pillHeight / 2f, 1.8f, eyeletPaint)
      canvas.drawCircle(pillLeft + pillWidth - 8f, pillTop + pillHeight / 2f, 1.8f, eyeletPaint)
      canvas.drawText("ГДЕ ЛЕЖИТ?", left + cellWidth / 2f, pillTop + 10.6f, headerTextPaint)

      val frameSize = 98f
      val qrSizePt = 90f
      val frameLeft = left + (cellWidth - frameSize) / 2f
      val frameTop = pillTop + pillHeight + 8f
      val frameRight = frameLeft + frameSize
      val frameBottom = frameTop + frameSize
      val arm = 10f

      canvas.drawLine(frameLeft, frameTop + arm, frameLeft, frameTop, bracketPaint)
      canvas.drawLine(frameLeft, frameTop, frameLeft + arm, frameTop, bracketPaint)
      canvas.drawLine(frameRight - arm, frameTop, frameRight, frameTop, bracketPaint)
      canvas.drawLine(frameRight, frameTop, frameRight, frameTop + arm, bracketPaint)
      canvas.drawLine(frameLeft, frameBottom - arm, frameLeft, frameBottom, bracketPaint)
      canvas.drawLine(frameLeft, frameBottom, frameLeft + arm, frameBottom, bracketPaint)
      canvas.drawLine(frameRight - arm, frameBottom, frameRight, frameBottom, bracketPaint)
      canvas.drawLine(frameRight, frameBottom - arm, frameRight, frameBottom, bracketPaint)

      val qrLeft = left + (cellWidth - qrSizePt) / 2f
      val qrTop = frameTop + (frameSize - qrSizePt) / 2f
      val payload = QrCodeManager.qrPayloadForLocation(location)
      val qrBitmap = QrCodeManager.generateQrBitmap(payload, 320)
      try {
        canvas.drawBitmap(
          qrBitmap,
          null,
          RectF(qrLeft, qrTop, qrLeft + qrSizePt, qrTop + qrSizePt),
          null,
        )
      } finally {
        qrBitmap.recycle()
      }

      val dividerY = frameBottom + 7f
      canvas.drawLine(left + 24f, dividerY, left + cellWidth - 24f, dividerY, dividerPaint)

      val textHorizontalPadding = 12f
      val textWidth = (cellWidth - textHorizontalPadding * 2f).toInt().coerceAtLeast(40)
      val titleLayout =
        StaticLayout.Builder.obtain(
            location.name,
            0,
            location.name.length,
            titlePaint,
            textWidth,
          )
          .setAlignment(Layout.Alignment.ALIGN_CENTER)
          .setLineSpacing(1.5f, 1.0f)
          .setMaxLines(2)
          .setEllipsize(TextUtils.TruncateAt.END)
          .build()

      canvas.save()
      canvas.translate(left + textHorizontalPadding, dividerY + 6f)
      titleLayout.draw(canvas)
      canvas.restore()
    }
  }

  private fun writeMultiPageJpegPdf(
    pageJpegs: List<ByteArray>,
    imageWidthPx: Int,
    imageHeightPx: Int,
    outputStream: OutputStream,
  ) {
    val buffer = ByteArrayOutputStream()
    val offsets = mutableListOf(0)

    fun writeAscii(text: String) {
      buffer.write(text.toByteArray(Charsets.US_ASCII))
    }

    fun startObj(objId: Int) {
      offsets.add(buffer.size())
      writeAscii("$objId 0 obj\n")
    }

    writeAscii("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n")

    val pageObjIds = pageJpegs.indices.map { 3 + it * 3 }
    startObj(1)
    writeAscii("<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

    startObj(2)
    val kidsRefs = pageObjIds.joinToString(" ") { "$it 0 R" }
    writeAscii("<< /Type /Pages /Kids [ $kidsRefs ] /Count ${pageJpegs.size} >>\nendobj\n")

    val contentCommand =
      "q\n$PAGE_WIDTH_PT 0 0 $PAGE_HEIGHT_PT 0 0 cm\n/Im0 Do\nQ\n".toByteArray(Charsets.US_ASCII)

    pageJpegs.forEachIndexed { index, jpegBytes ->
      val pageId = 3 + index * 3
      val contentId = pageId + 1
      val imageId = pageId + 2

      startObj(pageId)
      writeAscii(
        "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $PAGE_WIDTH_PT $PAGE_HEIGHT_PT] " +
          "/Resources << /XObject << /Im0 $imageId 0 R >> >> /Contents $contentId 0 R >>\nendobj\n"
      )

      startObj(contentId)
      writeAscii("<< /Length ${contentCommand.size} >>\nstream\n")
      buffer.write(contentCommand)
      writeAscii("\nendstream\nendobj\n")

      startObj(imageId)
      writeAscii(
        "<< /Type /XObject /Subtype /Image /Width $imageWidthPx /Height $imageHeightPx " +
          "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode " +
          "/Length ${jpegBytes.size} >>\nstream\n"
      )
      buffer.write(jpegBytes)
      writeAscii("\nendstream\nendobj\n")
    }

    val xrefOffset = buffer.size()
    val totalObjects = offsets.size
    writeAscii("xref\n0 $totalObjects\n")
    writeAscii("0000000000 65535 f \n")
    for (i in 1 until totalObjects) {
      writeAscii(String.format(Locale.US, "%010d 00000 n \n", offsets[i]))
    }
    writeAscii("trailer\n<< /Size $totalObjects /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")

    outputStream.write(buffer.toByteArray())
    outputStream.flush()
  }

  suspend fun writeQrSheetPdfToUri(
    context: Context,
    targetUri: Uri,
    locations: List<LocationEntity>,
  ): Boolean =
    withContext(Dispatchers.IO) {
      if (locations.isEmpty()) return@withContext false
      context.contentResolver.openOutputStream(targetUri)?.use { out ->
        generateQrSheetPdf(locations, out)
      } ?: false
    }

  suspend fun createShareableQrSheetPdfUri(
    context: Context,
    locations: List<LocationEntity>,
  ): Uri? =
    withContext(Dispatchers.IO) {
      if (locations.isEmpty()) return@withContext null
      val dir = File(context.cacheDir, STICKERS_CACHE_DIR).apply { if (!exists()) mkdirs() }
      val file = File(dir, "qr_stickers_sheet.pdf")
      val ok =
        FileOutputStream(file).use { out ->
          generateQrSheetPdf(locations, out)
        }
      if (!ok) return@withContext null
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
