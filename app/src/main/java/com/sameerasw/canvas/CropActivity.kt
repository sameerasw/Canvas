package com.sameerasw.canvas

import android.graphics.BitmapFactory
import android.graphics.Rect as AndroidRect
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.sameerasw.canvas.ui.drawing.BitmapStorageHelper
import com.sameerasw.canvas.ui.theme.CanvasTheme
import com.sameerasw.canvas.utils.HapticUtil
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import androidx.core.graphics.createBitmap

class CropActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageUriString = intent?.getStringExtra("image_uri")
        val isShare = intent?.getBooleanExtra("is_share", true) ?: true
        if (imageUriString == null) {
            finish()
            return
        }

        val imageUri = imageUriString.toUri()

        setContent {
            CanvasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                    val scope = rememberCoroutineScope()
                    val haptics = LocalHapticFeedback.current

                    // UI state for transform gestures
                    var boxWidth by remember { mutableStateOf(1f) }
                    var boxHeight by remember { mutableStateOf(1f) }
                    var baseScale by remember { mutableStateOf(1f) }
                    var transformScale by remember { mutableStateOf(1f) } // multiplier on top of baseScale
                    var offset by remember { mutableStateOf(Offset.Zero) } // translation in view pixels

                    // Load bitmap from uri
                    LaunchedEffect(imageUri) {
                        bitmap = try {
                            withContext(Dispatchers.IO) {
                                val stream: InputStream? = context.contentResolver.openInputStream(imageUri)
                                stream.use {
                                    BitmapFactory.decodeStream(it)
                                }
                            }
                        } catch (_: Exception) {
                            null
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(bitmap) {
                                // Loop so each gesture resets emittedStartTick when detectTransformGestures returns
                                while (true) {
                                    var emittedStartTick = false
                                    detectTransformGestures { centroid, pan, zoom, _ ->
                                        val panLen = kotlin.math.hypot(pan.x, pan.y)
                                        val zoomDelta = kotlin.math.abs(zoom - 1f)
                                        if (!emittedStartTick && (panLen > 2f || zoomDelta > 0.01f)) {
                                            HapticUtil.performLightTick(haptics)
                                            emittedStartTick = true
                                        }

                                        // adjust scale and translation
                                        val newScale = (transformScale * zoom).coerceIn(0.2f, 6f)
                                        transformScale = newScale
                                        offset += pan

                                        // emit a subtle variable tick proportional to zoom magnitude
                                        val strength = (zoomDelta * 2f).coerceIn(0f, 1f)
                                        if (zoomDelta > 0.002f) {
                                            HapticUtil.performVariableTick(haptics, strength)
                                        }
                                    }
                                }
                            }
                            .onGloballyPositioned { layoutCoordinates ->
                                val size = layoutCoordinates.size
                                boxWidth = size.width.toFloat()
                                boxHeight = size.height.toFloat()
                                // baseScale will be computed once bitmap is loaded
                                val bmp = bitmap
                                if (bmp != null) {
                                    baseScale = minOf(boxWidth / bmp.width.toFloat(), boxHeight / bmp.height.toFloat())
                                }
                            }
                    ) {
                        val bmp = bitmap
                        if (bmp != null) {
                            // compute displayed image size and position
                            val dispW = bmp.width * baseScale * transformScale
                            val dispH = bmp.height * baseScale * transformScale
                            val imageLeft = (boxWidth - dispW) / 2f + offset.x
                            val imageTop = (boxHeight - dispH) / 2f + offset.y

                            // Render the bitmap using Image positioned in pixel offsets
                            val density = LocalDensity.current
                            val dispWDp = with(density) { dispW.toDp() }
                            val dispHDp = with(density) { dispH.toDp() }
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(dispWDp, dispHDp)
                                    .graphicsLayer {
                                        translationX = imageLeft
                                        translationY = imageTop
                                    }
                            )


                            // Fixed centered square overlay size (80% of the minimum axis)
                            val squareSize = minOf(boxWidth, boxHeight) * 0.8f
                            val squareLeft = (boxWidth - squareSize) / 2f
                            val squareTop = (boxHeight - squareSize) / 2f

                            // capture theme colors here (composable scope) so the Canvas draw lambda doesn't call
                            // @Composable APIs (MaterialTheme) from a non-composable scope
                            val overlayColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            val strokeColor = MaterialTheme.colorScheme.onBackground

                            // dim outside the square and draw rounded outline using even-odd path
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                 val cornerRadius = 12.dp.toPx()

                                 val fullRect = ComposeRect(0f, 0f, size.width, size.height)
                                 val holeRoundRect = RoundRect(
                                     ComposeRect(
                                         squareLeft,
                                         squareTop,
                                         squareLeft + squareSize,
                                         squareTop + squareSize
                                     ),
                                     CornerRadius(cornerRadius, cornerRadius)
                                 )

                                 val path = Path().apply {
                                     fillType = PathFillType.EvenOdd
                                     addRect(fullRect)
                                     addRoundRect(holeRoundRect)
                                 }

                                 drawPath(path = path, color = overlayColor)

                                 // Draw rounded stroke around the crop area using theme onBackground color
                                 drawRoundRect(
                                     color = strokeColor,
                                     topLeft = Offset(squareLeft, squareTop),
                                     size = androidx.compose.ui.geometry.Size(squareSize, squareSize),
                                     cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                                     style = Stroke(width = 3f)
                                 )
                             }

                            // Bottom buttons: Cancel and Confirm centered
                            Column(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceContainer),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                                ) {
                                    Text(
                                        text = "Crop to continue",
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 24.dp),
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                                ) {
                                    if (isShare) {
                                        // When sharing, show Cancel | Save | Share
                                        Button(
                                            onClick = { HapticUtil.performClick(haptics); finish() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary,
                                                contentColor = MaterialTheme.colorScheme.onSecondary
                                            )
                                        ) {
                                            Text("Cancel")
                                        }
                                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(12.dp))

                                        Button(
                                            onClick = {
                                                HapticUtil.performClick(haptics)
                                                // perform crop and save to Downloads but keep activity open
                                                scope.launch {
                                                    val outBmp = performCropAndCreateBitmap(bmp, squareLeft, squareTop, squareSize, imageLeft, imageTop, baseScale, transformScale)
                                                    if (outBmp == null) {
                                                        Toast.makeText(context, "Nothing to export", Toast.LENGTH_SHORT).show()
                                                        return@launch
                                                    }
                                                    try {
                                                        val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                                                        val filename = "canvas_crop_$ts.png"
                                                        val uri = BitmapStorageHelper.saveBitmapToDownloads(context, outBmp, filename, android.graphics.Bitmap.CompressFormat.PNG)
                                                        if (uri != null) {
                                                            Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } catch (_: Exception) {
                                                        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        ) {
                                            Text("Save")
                                        }
                                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(12.dp))

                                        Button(
                                            onClick = {
                                                HapticUtil.performClick(haptics)
                                                // perform crop and share, then finish
                                                scope.launch {
                                                    val outBmp = performCropAndCreateBitmap(bmp, squareLeft, squareTop, squareSize, imageLeft, imageTop, baseScale, transformScale)
                                                    if (outBmp == null) {
                                                        Toast.makeText(context, "Nothing to export", Toast.LENGTH_SHORT).show()
                                                        return@launch
                                                    }
                                                    try {
                                                        val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                                                        val filename = "canvas_crop_$ts.png"
                                                        val uri = BitmapStorageHelper.saveBitmapToCacheAndGetUri(context, outBmp, filename, android.graphics.Bitmap.CompressFormat.PNG)
                                                        if (uri != null) {
                                                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                                type = "image/png"
                                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            }
                                                            try {
                                                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share image"))
                                                            } catch (_: Exception) {
                                                                Toast.makeText(context, "No app available to share image", Toast.LENGTH_SHORT).show()
                                                            }
                                                        } else {
                                                            Toast.makeText(context, "Failed to export image", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } catch (_: Exception) {
                                                        Toast.makeText(context, "Failed to process image", Toast.LENGTH_SHORT).show()
                                                    } finally {
                                                        finish()
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        ) {
                                            Text("Share")
                                        }
                                    } else {
                                        // Non-share flow: existing Cancel + Save (finish after saving)
                                        Button(
                                            onClick = { HapticUtil.performClick(haptics); finish() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary,
                                                contentColor = MaterialTheme.colorScheme.onSecondary
                                            )
                                        ) {
                                            Text("Cancel")
                                        }
                                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(16.dp))
                                        Button(
                                            onClick = {
                                                HapticUtil.performClick(haptics)
                                                scope.launch {
                                                    val outBmp = performCropAndCreateBitmap(bmp, squareLeft, squareTop, squareSize, imageLeft, imageTop, baseScale, transformScale)
                                                    if (outBmp == null) {
                                                        Toast.makeText(context, "Nothing to export", Toast.LENGTH_SHORT).show()
                                                        return@launch
                                                    }
                                                    try {
                                                        val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                                                        val filename = "canvas_crop_$ts.png"
                                                        val uri = BitmapStorageHelper.saveBitmapToDownloads(context, outBmp, filename, android.graphics.Bitmap.CompressFormat.PNG)
                                                        if (uri != null) {
                                                            Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } catch (_: Exception) {
                                                        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                                                    } finally {
                                                        finish()
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        ) {
                                            Text("Save")
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
 }

// compute and return the cropped bitmap, or null on failure
private suspend fun performCropAndCreateBitmap(
    bmp: android.graphics.Bitmap,
    squareLeft: Float,
    squareTop: Float,
    squareSize: Float,
    imageLeft: Float,
    imageTop: Float,
    baseScale: Float,
    transformScale: Float
): android.graphics.Bitmap? {
    return withContext(Dispatchers.Default) {
        try {
            val scalePxPerBitmap = baseScale * transformScale
            val reqLeftBitmapF = (squareLeft - imageLeft) / scalePxPerBitmap
            val reqTopBitmapF = (squareTop - imageTop) / scalePxPerBitmap
            val reqWBitmapF = squareSize / scalePxPerBitmap
            val reqHBitmapF = squareSize / scalePxPerBitmap

            val reqLeftBitmap = reqLeftBitmapF.toInt()
            val reqTopBitmap = reqTopBitmapF.toInt()
            val reqWBitmap = reqWBitmapF.toInt()
            val reqHBitmap = reqHBitmapF.toInt()

            val srcLeft = reqLeftBitmap.coerceAtLeast(0)
            val srcTop = reqTopBitmap.coerceAtLeast(0)
            val srcRight = (reqLeftBitmap + reqWBitmap).coerceAtMost(bmp.width)
            val srcBottom = (reqTopBitmap + reqHBitmap).coerceAtMost(bmp.height)

            val srcW = srcRight - srcLeft
            val srcH = srcBottom - srcTop
            if (srcW <= 0 || srcH <= 0) return@withContext null

            val outW = reqWBitmap.coerceAtLeast(1)
            val outH = reqHBitmap.coerceAtLeast(1)
            val outBmp = createBitmap(outW, outH)
            val canvas = android.graphics.Canvas(outBmp)
            canvas.drawColor(android.graphics.Color.WHITE)

            val dstLeft = (srcLeft - reqLeftBitmap)
            val dstTop = (srcTop - reqTopBitmap)
            val srcRect = AndroidRect(srcLeft, srcTop, srcRight, srcBottom)
            val dstRect = AndroidRect(dstLeft, dstTop, dstLeft + srcW, dstTop + srcH)
            canvas.drawBitmap(bmp, srcRect, dstRect, null)

            outBmp
        } catch (_: Exception) {
            null
        }
    }
}
