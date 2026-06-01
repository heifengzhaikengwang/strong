package com.paperscanner.app.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.opencv.core.Point
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    private const val SCAN_FOLDER = "PaperScanner"
    private const val IMAGE_PREFIX = "SCAN_"
    private const val IMAGE_EXTENSION = ".jpg"
    private const val QUALITY = 95

    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "$IMAGE_PREFIX$timeStamp"
        val storageDir = getScanDirectory(context)
        return File.createTempFile(imageFileName, IMAGE_EXTENSION, storageDir)
    }

    fun getScanDirectory(context: Context): File {
        val directory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), SCAN_FOLDER)
        } else {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), SCAN_FOLDER)
        }

        if (!directory.exists()) {
            directory.mkdirs()
        }

        return directory
    }

    fun saveBitmap(context: Context, bitmap: Bitmap, fileName: String? = null): Uri? {
        val timeStamp = fileName ?: SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileNameWithExt = "$IMAGE_PREFIX$timeStamp$IMAGE_EXTENSION"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageApi29AndAbove(context, bitmap, fileNameWithExt)
        } else {
            saveImageLegacy(context, bitmap, fileNameWithExt)
        }
    }

    private fun saveImageApi29AndAbove(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$SCAN_FOLDER")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, outputStream)
            }
        }

        return uri
    }

    private fun saveImageLegacy(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val directory = getScanDirectory(context)
        val file = File(directory, fileName)

        return try {
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, outputStream)
            }
            Uri.fromFile(file)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getAllScannedImages(context: Context): List<File> {
        val directory = getScanDirectory(context)
        return directory.listFiles { file ->
            file.isFile && file.name.startsWith(IMAGE_PREFIX) && file.name.endsWith(IMAGE_EXTENSION)
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun deleteFile(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun pointFListToPoint(list: List<PointF>): List<Point> {
        return list.map { Point(it.x.toDouble(), it.y.toDouble()) }
    }

    fun pointListToPointF(list: List<Point>): List<PointF> {
        return list.map { PointF(it.x.toFloat(), it.y.toFloat()) }
    }
}
