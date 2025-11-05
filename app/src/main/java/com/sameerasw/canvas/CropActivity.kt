package com.sameerasw.canvas

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.canvas.ui.drawing.BitmapStorageHelper
import com.sameerasw.canvas.ui.drawing.StrokeDrawer.drawScribbleStroke
import com.sameerasw.canvas.ui.drawing.TextDrawer.drawStringWithFont
import com.sameerasw.canvas.ui.theme.CanvasTheme
import com.sameerasw.canvas.utils.HapticUtil
import com.sameerasw.canvas.model.DrawStroke
import com.sameerasw.canvas.data.TextItem
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import androidx.core.graphics.createBitmap
import kotlin.math.hypot

class CropActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageUriString = intent?.getStringExtra("image_uri")
        val isShare = intent?.getBooleanExtra("is_share", true) ?: true
        val strokesJson = intent?.getStringExtra("strokes_json") ?: "[]"
        val textsJson = intent?.getStringExtra("texts_json") ?: "[]"

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
                    var strokes by remember { mutableStateOf<List<DrawStroke>>(emptyList()) }
                    var texts by remember { mutableStateOf<List<TextItem>>(emptyList()) }
                    val scope = rememberCoroutineScope()
                    val haptics = LocalHapticFeedback.current

                    // Deserialize strokes and texts on first composition
                    LaunchedEffect(Unit) {
                        try {
                            val gson = com.google.gson.Gson()
                            val strokeType = object : com.google.gson.reflect.TypeToken<List<DrawStroke>>() {}.type
                            val textType = object : com.google.gson.reflect.TypeToken<List<TextItem>>() {}.type
                            strokes = gson.fromJson(strokesJson, strokeType) ?: emptyList()
                            texts = gson.fromJson(textsJson, textType) ?: emptyList()
                        } catch (_: Exception) {
                            strokes = emptyList()
                            texts = emptyList()
                        }
                    }

                    // Aspect selection: 0=9:16 (vertical), 1=1:1 (square), 2=16:9 (horizontal)
                    var selectedAspectIndex by remember { mutableStateOf(1) } // default 1:1
                    val aspectRatios = listOf(9f/16f, 1f, 16f/9f)

                    // UI state for transform gestures
                    var viewWidth by remember { mutableStateOf(1f) }
                    var viewHeight by remember { mutableStateOf(1f) }
                    var scale by remember { mutableStateOf(1f) }
                    var offsetX by remember { mutableStateOf(0f) }
                    var offsetY by remember { mutableStateOf(0f) }

                    // Load bitmap from uri
                    LaunchedEffect(imageUri) {
                        try {
                            withContext(Dispatchers.IO) {
                                val stream: InputStream? = context.contentResolver.openInputStream(imageUri)
                                val loadedBmp = stream.use {
                                    BitmapFactory.decodeStream(it)
                                }
                                bitmap = loadedBmp
                            }
                        } catch (_: Exception) {
                            bitmap = null
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(bitmap) {
                                while (true) {
                                    var emittedStartTick = false
                                    detectTransformGestures { centroid, pan, zoom, _ ->
                                        val panLen = hypot(pan.x, pan.y)
                                        val zoomDelta = kotlin.math.abs(zoom - 1f)
                                        if (!emittedStartTick && (panLen > 2f || zoomDelta > 0.01f)) {
                                            HapticUtil.performLightTick(haptics)
                                            emittedStartTick = true
                                        }

                                        // Zoom around centroid with proper math
                                        val oldScale = scale
                                        scale = (scale * zoom).coerceIn(0.2f, 5f)

                                        // Convert centroid from screen space to world space
                                        val worldCx = (centroid.x - offsetX) / oldScale
                                        val worldCy = (centroid.y - offsetY) / oldScale

                                        // Apply new scale
                                        offsetX = centroid.x - worldCx * scale
                                        offsetY = centroid.y - worldCy * scale

                                        // Apply pan
                                        offsetX += pan.x
                                        offsetY += pan.y

                                        // Emit haptic
                                        val strength = (zoomDelta * 2f).coerceIn(0f, 1f)
                                        if (zoomDelta > 0.002f) {
                                            HapticUtil.performVariableTick(haptics, strength)
                                        }
                                    }
                                }
                            }
                            .onGloballyPositioned { layoutCoordinates ->
                                val size = layoutCoordinates.size
                                viewWidth = size.width.toFloat()
                                viewHeight = size.height.toFloat()
                            }
                    ) {
                        val bmp = bitmap
                        if (bmp != null) {
                            // White background
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRect(Color.White, size = size)

                                // Draw bitmap with transform
                                val dispW = bmp.width * scale
                                val dispH = bmp.height * scale

                                // Draw the bitmap centered and transformed
                                drawImage(
                                    image = bmp.asImageBitmap(),
                                    dstOffset = androidx.compose.ui.unit.IntOffset(offsetX.toInt(), offsetY.toInt()),
                                    dstSize = androidx.compose.ui.unit.IntSize(dispW.toInt(), dispH.toInt())
                                )

                                // Draw strokes
                                strokes.forEach { stroke ->
                                    if (stroke.points.size >= 2) {
                                        val screenPoints = stroke.points.map { worldPoint ->
                                            Offset(
                                                worldPoint.x * scale + offsetX,
                                                worldPoint.y * scale + offsetY
                                            )
                                        }
                                        drawScribbleStroke(screenPoints, stroke.color, stroke.width * scale)
                                    }
                                }

                                // Draw texts
                                texts.forEach { textItem ->
                                    val screenX = textItem.x * scale + offsetX
                                    val screenY = textItem.y * scale + offsetY
                                    drawStringWithFont(
                                        context = context,
                                        text = textItem.text,
                                        x = screenX,
                                        y = screenY,
                                        fontSize = textItem.size * scale,
                                        colorInt = textItem.color.toArgb()
                                    )
                                }
                            }

                            // Compute crop overlay size using selected aspect ratio
                            val targetAspect = aspectRatios[selectedAspectIndex]
                            val maxOverlayWidth = viewWidth * 0.9f
                            val maxOverlayHeight = viewHeight * 0.5f
                            var overlayW = maxOverlayWidth
                            var overlayH = overlayW / targetAspect
                            if (overlayH > maxOverlayHeight) {
                                overlayH = maxOverlayHeight
                                overlayW = overlayH * targetAspect
                            }
                            val overlayLeft = (viewWidth - overlayW) / 2f
                            val overlayTop = (viewHeight - overlayH) / 2f

                            val overlayColor = Color.Black.copy(alpha = 0.55f)
                            val strokeColor = MaterialTheme.colorScheme.onBackground

                            // Draw crop overlay
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
                                        // Aspect picker
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
                                                OutlinedButton(onClick = { HapticUtil.performClick(haptics); finish() }) {
                                                    Text("Back")
                                                }

                                                Button(onClick = {
                                                    HapticUtil.performClick(haptics)
                                                    scope.launch {
                                                        val outBmp = performCropAndCreateBitmap(overlayLeft, overlayTop, overlayW, overlayH, offsetX, offsetY, scale, strokes, texts, context)
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
                                                        val outBmp = performCropAndCreateBitmap(overlayLeft, overlayTop, overlayW, overlayH, offsetX, offsetY, scale, strokes, texts, context)
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
                                                OutlinedButton(onClick = { HapticUtil.performClick(haptics); finish() }) {
                                                    Text("Back")
                                                }
                                                Button(onClick = {
                                                    HapticUtil.performClick(haptics)
                                                    scope.launch {
                                                        val outBmp = performCropAndCreateBitmap(overlayLeft, overlayTop, overlayW, overlayH, offsetX, offsetY, scale, strokes, texts, context)
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

// Compute and return the cropped bitmap, rendering strokes and texts in the crop region
private suspend fun performCropAndCreateBitmap(
    overlayLeft: Float,
    overlayTop: Float,
    overlayW: Float,
    overlayH: Float,
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    strokes: List<DrawStroke>,
    texts: List<TextItem>,
    context: android.content.Context
): android.graphics.Bitmap? {
    return withContext(Dispatchers.Default) {
        try {
            val outW = overlayW.toInt().coerceAtLeast(1)
            val outH = overlayH.toInt().coerceAtLeast(1)

            val outBmp = createBitmap(outW, outH)
            val canvas = android.graphics.Canvas(outBmp)
            canvas.drawColor(android.graphics.Color.WHITE)

            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
            }

            // Convert screen coordinates to world coordinates
            val worldLeft = (overlayLeft - offsetX) / scale
            val worldTop = (overlayTop - offsetY) / scale
            val worldWidth = overlayW / scale
            val worldHeight = overlayH / scale

            // Draw strokes that fall within the crop region
            strokes.forEach { s ->
                if (s.points.size < 2) return@forEach

                paint.color = s.color.toArgb()
                paint.strokeWidth = s.width

                val path = android.graphics.Path()
                val pts = s.points

                // Transform points from world to output bitmap space
                val (firstX, firstY) = convertWorldToOutput(pts.first(), worldLeft, worldTop, worldWidth, worldHeight, outW, outH)
                path.moveTo(firstX, firstY)

                for (i in 1 until pts.size) {
                    val prev = pts[i - 1]
                    val curr = pts[i]
                    val (prevX, prevY) = convertWorldToOutput(prev, worldLeft, worldTop, worldWidth, worldHeight, outW, outH)
                    val (currX, currY) = convertWorldToOutput(curr, worldLeft, worldTop, worldWidth, worldHeight, outW, outH)
                    val midX = (prevX + currX) / 2f
                    val midY = (prevY + currY) / 2f
                    path.quadTo(prevX, prevY, midX, midY)
                }

                val (lastX, lastY) = convertWorldToOutput(pts.last(), worldLeft, worldTop, worldWidth, worldHeight, outW, outH)
                path.lineTo(lastX, lastY)
                canvas.drawPath(path, paint)
            }

            // Draw texts that fall within the crop region
            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
            }

            texts.forEach { t ->
                val (textX, textY) = convertWorldToOutput(Offset(t.x, t.y), worldLeft, worldTop, worldWidth, worldHeight, outW, outH)
                textPaint.color = t.color.toArgb()
                textPaint.textSize = t.size
                try {
                    val tf: android.graphics.Typeface? = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.font)
                    if (tf != null) textPaint.typeface = tf
                } catch (_: Exception) { }

                val fm = textPaint.fontMetrics
                val baseline = textY - fm.ascent
                canvas.drawText(t.text, textX, baseline, textPaint)
            }

            outBmp
        } catch (_: Exception) {
            null
        }
    }
}

// Helper function to convert world coordinates to output bitmap coordinates
private fun convertWorldToOutput(
    worldPoint: Offset,
    worldLeft: Float,
    worldTop: Float,
    worldWidth: Float,
    worldHeight: Float,
    outW: Int,
    outH: Int
): Pair<Float, Float> {
    val relX = (worldPoint.x - worldLeft) / worldWidth
    val relY = (worldPoint.y - worldTop) / worldHeight
    val outX = relX * outW
    val outY = relY * outH
    return Pair(outX, outY)
}


