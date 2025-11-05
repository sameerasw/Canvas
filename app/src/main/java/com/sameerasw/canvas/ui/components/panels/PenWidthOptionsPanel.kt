package com.sameerasw.canvas.ui.components.panels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.canvas.R
import com.sameerasw.canvas.model.PenStyle

@Composable
fun PenWidthOptionsPanel(
    visible: Boolean,
    selectedStyle: PenStyle,
    penWidth: Float,
    onStyleSelected: (PenStyle) -> Unit,
    onPenWidthChange: (Float) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(260)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(220))
    ) {
        Surface(
            modifier = Modifier,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .width(280.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { onStyleSelected(PenStyle.NORMAL) },
                        modifier = Modifier.then(
                            if (selectedStyle == PenStyle.NORMAL)
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            else Modifier
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_stylus_fountain_pen_24),
                            contentDescription = "Normal",
                            tint = if (selectedStyle == PenStyle.NORMAL) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onStyleSelected(PenStyle.SMOOTH) },
                        modifier = Modifier.then(
                            if (selectedStyle == PenStyle.SMOOTH)
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            else Modifier
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.stylus_pen_24px),
                            contentDescription = "Smooth",
                            tint = if (selectedStyle == PenStyle.SMOOTH) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onStyleSelected(PenStyle.ROUGH) },
                        modifier = Modifier.then(
                            if (selectedStyle == PenStyle.ROUGH)
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            else Modifier
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.stylus_pencil_24px),
                            contentDescription = "Rough",
                            tint = if (selectedStyle == PenStyle.ROUGH) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val circleSize = (penWidth * 1.5f).dp.coerceAtLeast(10.dp).coerceAtMost(64.dp)
                Box(
                    modifier = Modifier.width(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(circleSize)
                    ) {}
                }

                Slider(
                    value = penWidth,
                    onValueChange = onPenWidthChange,
                    valueRange = 1f..48f,
                    modifier = Modifier.width(240.dp)
                )
            }
        }
    }
}

