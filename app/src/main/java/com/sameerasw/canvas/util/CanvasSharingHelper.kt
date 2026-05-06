package com.sameerasw.canvas.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.sameerasw.canvas.CanvasModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object CanvasSharingHelper {
    private const val TAG = "CanvasSharingHelper"
    private const val METADATA_FILENAME = "metadata.json"
    private const val BACKGROUND_FILENAME = "background.png"

    /**
     * Exports the canvas state and background image into a single .canvas (ZIP) file.
     */
    suspend fun exportCanvas(context: Context, model: CanvasModel, outputUri: Uri) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Exporting canvas to $outputUri")
        context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
            ZipOutputStream(outputStream).use { zos ->
                // 1. Save JSON metadata
                val exportModel = model.copy(
                    backgroundImageUri = if (model.backgroundImageUri != null) BACKGROUND_FILENAME else null
                )
                val gson = GsonProvider.create()
                val json = gson.toJson(exportModel)
                Log.d(TAG, "Exported metadata JSON: $json")
                
                zos.putNextEntry(ZipEntry(METADATA_FILENAME))
                zos.write(json.toByteArray())
                zos.closeEntry()

                // 2. Save background image if exists
                model.backgroundImageUri?.let { uriString ->
                    try {
                        val uri = Uri.parse(uriString)
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            zos.putNextEntry(ZipEntry(BACKGROUND_FILENAME))
                            inputStream.copyTo(zos)
                            zos.closeEntry()
                            Log.d(TAG, "Background image bundled in export")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error bundling background image", e)
                    }
                }
            }
        }
    }

    /**
     * Imports a .canvas (ZIP) file, extracts metadata and image, and returns a CanvasModel.
     */
    suspend fun importCanvas(context: Context, inputUri: Uri): CanvasModel? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Importing canvas from $inputUri")
        var model: CanvasModel? = null
        var hasBackground = false
        val tempDir = File(context.cacheDir, "canvas_import_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        Log.d(TAG, "Processing ZIP entry: ${entry.name}")
                        when (entry.name) {
                            METADATA_FILENAME -> {
                                val json = zis.readBytes().toString(Charsets.UTF_8)
                                Log.d(TAG, "Imported metadata JSON: $json")
                                model = GsonProvider.create().fromJson(json, CanvasModel::class.java)
                            }
                            BACKGROUND_FILENAME -> {
                                val bgFile = File(tempDir, BACKGROUND_FILENAME)
                                bgFile.outputStream().use { fos ->
                                    zis.copyTo(fos)
                                }
                                hasBackground = true
                                Log.d(TAG, "Background image extracted during import")
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            if (model != null) {
                if (hasBackground) {
                    val internalBgDir = File(context.filesDir, "backgrounds")
                    if (!internalBgDir.exists()) internalBgDir.mkdirs()
                    val internalBgFile = File(internalBgDir, "bg_${System.currentTimeMillis()}.png")
                    val sourceFile = File(tempDir, BACKGROUND_FILENAME)
                    sourceFile.copyTo(internalBgFile, overwrite = true)
                    
                    model = model!!.copy(backgroundImageUri = Uri.fromFile(internalBgFile).toString())
                    Log.d(TAG, "Updated model with internal background URI: ${model?.backgroundImageUri}")
                }
                return@withContext model
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importing canvas", e)
        } finally {
            tempDir.deleteRecursively()
        }
        Log.d(TAG, "Import finished, model is null? ${model == null}")
        model
    }
}
