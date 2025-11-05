package com.sameerasw.canvas.ui.drawing

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object BitmapStorageHelper {
    suspend fun saveBitmapToDownloads(
        context: Context,
        bitmap: Bitmap,
        filename: String,
        format: Bitmap.CompressFormat
    ): Uri? = withContext(Dispatchers.IO) {
        val mime = if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri: Uri? = resolver.insert(collection, values)
            if (uri == null) return@withContext null

            try {
                resolver.openOutputStream(uri).use { out ->
                    if (out == null) throw IOException("Unable to open output stream")
                    bitmap.compress(format, if (format == Bitmap.CompressFormat.PNG) 100 else 90, out)
                    out.flush()
                }
            } catch (_: Exception) {
                resolver.delete(uri, null, null)
                return@withContext null
            }

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)

            return@withContext uri
        } else {
            try {
                val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, filename)
                file.outputStream().use { out ->
                    bitmap.compress(format, if (format == Bitmap.CompressFormat.PNG) 100 else 90, out)
                    out.flush()
                }

                val authority = context.packageName + ".fileprovider"
                return@withContext FileProvider.getUriForFile(context, authority, file)
            } catch (_: Exception) {
                return@withContext null
            }
        }
    }

    suspend fun saveBitmapToCacheAndGetUri(
        context: Context,
        bitmap: Bitmap,
        filename: String,
        format: Bitmap.CompressFormat
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, filename)
            file.outputStream().use { out ->
                bitmap.compress(format, if (format == Bitmap.CompressFormat.PNG) 100 else 90, out)
                out.flush()
            }
            val authority = context.packageName + ".fileprovider"
            return@withContext FileProvider.getUriForFile(context, authority, file)
        } catch (_: Exception) {
            return@withContext null
        }
    }
}

