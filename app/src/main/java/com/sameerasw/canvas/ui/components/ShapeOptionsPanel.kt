package com.sameerasw.canvas.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ShapeOptionsPanel(
    visible: Boolean,
    shapeWidth: Float,
    shapeFilled: Boolean,
    onShapeWidthChange: (Float) -> Unit,
    onShapeFilledChange: (Boolean) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(260)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(220))
    ) {
        Surface(
            modifier = Modifier,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .width(280.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fill", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = shapeFilled,
                        onCheckedChange = onShapeFilledChange
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val circleSize = (shapeWidth * 1.5f).dp.coerceAtLeast(10.dp).coerceAtMost(64.dp)
                Box(
                    modifier = Modifier.width(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(circleSize)
                    ) {}
                }

                Slider(
                    value = shapeWidth,
                    onValueChange = onShapeWidthChange,
                    valueRange = 1f..48f,
                    modifier = Modifier.width(240.dp)
                )
            }
        }
    }
}
