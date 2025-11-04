package com.sameerasw.canvas

import android.graphics.BitmapFactory
import android.graphics.Rect as AndroidRect
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sameerasw.canvas.ui.drawing.BitmapStorageHelper
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

class CropActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageUriString = intent?.getStringExtra("image_uri")
        val isShare = intent?.getBooleanExtra("is_share", true) ?: true
        if (imageUriString == null) {
            finish()
            return
        }

        val imageUri = Uri.parse(imageUriString)

        setContent {
            val context = LocalContext.current
            var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
            val scope = rememberCoroutineScope()

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
                } catch (e: Exception) {
                    null
                }
            }

            Box(modifier = Modifier
                .fillMaxSize()
                .pointerInput(bitmap) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        // Simple, responsive transform: adjust scale and translation
                        val newScale = (transformScale * zoom).coerceIn(0.2f, 6f)
                        transformScale = newScale
                        offset += pan
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

                    // dim outside the square and draw outline
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // darken outside
                        drawRect(color = Color(0x88000000))
                        // draw outline
                        drawRect(color = Color.Transparent, topLeft = Offset(squareLeft, squareTop), size = androidx.compose.ui.geometry.Size(squareSize, squareSize))
                        drawRect(color = Color.White, topLeft = Offset(squareLeft, squareTop), size = androidx.compose.ui.geometry.Size(squareSize, squareSize), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
                    }

                    // Bottom buttons: Cancel and Confirm centered
                    Column(modifier = Modifier
                        .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom
                    ) {
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                            Button(onClick = { finish() }) {
                                Text("Cancel")
                            }
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(16.dp))
                            Button(onClick = {
                                // perform crop based on fixed square
                                scope.launch {
                                    // compute requested crop in bitmap pixel coordinates
                                    val reqLeftView = squareLeft
                                    val reqTopView = squareTop
                                    val reqWView = squareSize
                                    val reqHView = squareSize

                                    val scalePxPerBitmap = baseScale * transformScale
                                    val reqLeftBitmapF = (reqLeftView - imageLeft) / scalePxPerBitmap
                                    val reqTopBitmapF = (reqTopView - imageTop) / scalePxPerBitmap
                                    val reqWBitmapF = reqWView / scalePxPerBitmap
                                    val reqHBitmapF = reqHView / scalePxPerBitmap

                                    val reqLeftBitmap = reqLeftBitmapF.toInt()
                                    val reqTopBitmap = reqTopBitmapF.toInt()
                                    val reqWBitmap = reqWBitmapF.toInt()
                                    val reqHBitmap = reqHBitmapF.toInt()

                                    // Intersection with source bitmap
                                    val srcLeft = reqLeftBitmap.coerceAtLeast(0)
                                    val srcTop = reqTopBitmap.coerceAtLeast(0)
                                    val srcRight = (reqLeftBitmap + reqWBitmap).coerceAtMost(bmp.width)
                                    val srcBottom = (reqTopBitmap + reqHBitmap).coerceAtMost(bmp.height)

                                    val srcW = srcRight - srcLeft
                                    val srcH = srcBottom - srcTop

                                    if (srcW <= 0 || srcH <= 0) {
                                        // nothing overlapped
                                        Toast.makeText(context, "Nothing to export", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    // Create output bitmap of requested size and draw the intersected portion at the correct offset
                                    val outW = reqWBitmap.coerceAtLeast(1)
                                    val outH = reqHBitmap.coerceAtLeast(1)
                                    val outBmp = android.graphics.Bitmap.createBitmap(outW, outH, android.graphics.Bitmap.Config.ARGB_8888)
                                    val canvas = android.graphics.Canvas(outBmp)
                                    canvas.drawColor(android.graphics.Color.WHITE)

                                    val dstLeft = (srcLeft - reqLeftBitmap)
                                    val dstTop = (srcTop - reqTopBitmap)

                                    val srcRect = AndroidRect(srcLeft, srcTop, srcRight, srcBottom)
                                    val dstRect = AndroidRect(dstLeft, dstTop, dstLeft + srcW, dstTop + srcH)

                                    canvas.drawBitmap(bmp, srcRect, dstRect, null)

                                    // Save / share
                                    try {
                                        val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                                        val filename = "doodlist_crop_$ts.png"
                                        if (isShare) {
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
                                        } else {
                                            val uri = BitmapStorageHelper.saveBitmapToDownloads(context, outBmp, filename, android.graphics.Bitmap.CompressFormat.PNG)
                                            if (uri != null) {
                                                Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to process image", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        finish()
                                    }
                                }
                            }) {
                                Text("Confirm")
                            }
                        }
                    }
                }
            }
        }
    }
}
