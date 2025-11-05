package com.sameerasw.canvas

import android.graphics.BitmapFactory
import android.graphics.Rect as AndroidRect
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
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
import androidx.compose.ui.res.painterResource
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

        @OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

                    // Aspect selection: 0=9:16 (vertical), 1=1:1 (square), 2=16:9 (horizontal)
                    var selectedAspectIndex by remember { mutableStateOf(1) } // default 1:1
                    val aspectRatios = listOf(9f/16f, 1f, 16f/9f)

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


                            // compute crop overlay size using selected aspect ratio
                            val targetAspect = aspectRatios[selectedAspectIndex]
                            val maxOverlayWidth = boxWidth * 0.9f
                            val maxOverlayHeight = boxHeight * 0.5f
                            // Determine overlay width/height constrained to max dims and aspect
                            var overlayW = maxOverlayWidth
                            var overlayH = overlayW / targetAspect
                            if (overlayH > maxOverlayHeight) {
                                overlayH = maxOverlayHeight
                                overlayW = overlayH * targetAspect
                            }
                            val overlayLeft = (boxWidth - overlayW) / 2f
                            val overlayTop = (boxHeight - overlayH) / 2f

                            // prepare colors (capture them outside Canvas so we don't call @Composable inside draw lambda)
                            val overlayColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            val strokeColor = MaterialTheme.colorScheme.onBackground

                            // draw image and dim/outline overlay
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cornerRadius = 12.dp.toPx()

                                val fullRect = ComposeRect(0f, 0f, size.width, size.height)
                                val holeRoundRect = RoundRect(
                                    ComposeRect(
                                        overlayLeft,
                                        overlayTop,
                                        overlayLeft + overlayW,
                                        overlayTop + overlayH
                                    ),
                                    CornerRadius(cornerRadius, cornerRadius)
                                )

                                val path = Path().apply {
                                    fillType = PathFillType.EvenOdd
                                    addRect(fullRect)
                                    addRoundRect(holeRoundRect)
                                }

                                drawPath(path = path, color = overlayColor)
                                drawRoundRect(
                                    color = strokeColor,
                                    topLeft = Offset(overlayLeft, overlayTop),
                                    size = androidx.compose.ui.geometry.Size(overlayW, overlayH),
                                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                                    style = Stroke(width = 3f)
                                )
                            }

                            // Bottom card with Cancel/Save/Share
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .padding(bottom = 24.dp)
                                        .padding(horizontal = 16.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 0.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Aspect picker using connected ButtonGroup with OutlinedToggleButtons (icons only)
                                        Row(
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp)
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                        ) {
                                            val aspectIcons = listOf(
                                                R.drawable.rounded_crop_9_16_24,
                                                R.drawable.rounded_crop_square_24,
                                                R.drawable.rounded_rectangle_24
                                            )
                                            aspectIcons.forEachIndexed { index, iconRes ->
                                                ToggleButton(
                                                    checked = selectedAspectIndex == index,
                                                    onCheckedChange = { selectedAspectIndex = index },
                                                    modifier = Modifier.weight(1f),
                                                    shapes = when (index) {
                                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                                        aspectIcons.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                                    },
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = iconRes),
                                                        contentDescription = null
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.size(12.dp))

                                        Text(
                                            text = "Crop and resize",
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.size(12.dp))

                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            if (isShare) {
                                                // Cancel outlined, Save and Share primary
                                                OutlinedButton(onClick = { HapticUtil.performClick(haptics); finish() }) {
                                                    Text("Cancel")
                                                }

                                                Button(onClick = {
                                                    HapticUtil.performClick(haptics)
                                                    scope.launch {
                                                        val outBmp = performCropAndCreateBitmap(bmp, overlayLeft, overlayTop, overlayW, overlayH, imageLeft, imageTop, baseScale, transformScale)
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
                                                }) {
                                                    Icon(painter = painterResource(id = R.drawable.rounded_download_24), contentDescription = "Save")
                                                    Spacer(modifier = Modifier.size(8.dp))
                                                    Text("Save")
                                                }

                                                Button(onClick = {
                                                    HapticUtil.performClick(haptics)
                                                    scope.launch {
                                                        val outBmp = performCropAndCreateBitmap(bmp, overlayLeft, overlayTop, overlayW, overlayH, imageLeft, imageTop, baseScale, transformScale)
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
                                                }) {
                                                    Icon(painter = painterResource(id = R.drawable.rounded_ios_share_24), contentDescription = "Share")
                                                    Spacer(modifier = Modifier.size(8.dp))
                                                    Text("Share")
                                                }
                                            } else {
                                                // Non-share flow: Cancel outlined + Save primary (finish after save)
                                                OutlinedButton(onClick = { HapticUtil.performClick(haptics); finish() }) {
                                                    Text("Back")
                                                }
                                                Button(onClick = {
                                                    HapticUtil.performClick(haptics)
                                                    scope.launch {
                                                        val outBmp = performCropAndCreateBitmap(bmp, overlayLeft, overlayTop, overlayW, overlayH, imageLeft, imageTop, baseScale, transformScale)
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
                                                }) {
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
    }
}

// compute and return the cropped bitmap, or null on failure
private suspend fun performCropAndCreateBitmap(
    bmp: android.graphics.Bitmap,
    overlayLeft: Float,
    overlayTop: Float,
    overlayW: Float,
    overlayH: Float,
    imageLeft: Float,
    imageTop: Float,
    baseScale: Float,
    transformScale: Float
): android.graphics.Bitmap? {
    return withContext(Dispatchers.Default) {
        try {
            val scalePxPerBitmap = baseScale * transformScale

            // compute requested crop rect in view coords -> bitmap coords
            val reqLeftBitmapF = (overlayLeft - imageLeft) / scalePxPerBitmap
            val reqTopBitmapF = (overlayTop - imageTop) / scalePxPerBitmap
            val reqWBitmapF = overlayW / scalePxPerBitmap
            val reqHBitmapF = overlayH / scalePxPerBitmap

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


