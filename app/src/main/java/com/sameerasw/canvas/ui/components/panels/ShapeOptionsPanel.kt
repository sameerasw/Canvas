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
import com.sameerasw.canvas.model.ShapeType

@Composable
fun ShapeOptionsPanel(
    visible: Boolean,
    selectedShape: ShapeType,
    shapeWidth: Float,
    shapeFilled: Boolean,
    onShapeSelected: (ShapeType) -> Unit,
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
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .width(280.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Row(
                    modifier = Modifier.padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { onShapeSelected(ShapeType.RECTANGLE) },
                        modifier = Modifier.then(
                            if (selectedShape == ShapeType.RECTANGLE)
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            else Modifier
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_rectangle_24),
                            contentDescription = "Rectangle",
                            tint = if (selectedShape == ShapeType.RECTANGLE) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onShapeSelected(ShapeType.CIRCLE) },
                        modifier = Modifier.then(
                            if (selectedShape == ShapeType.CIRCLE)
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            else Modifier
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_circle_24),
                            contentDescription = "Circle",
                            tint = if (selectedShape == ShapeType.CIRCLE) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onShapeSelected(ShapeType.TRIANGLE) },
                        modifier = Modifier.then(
                            if (selectedShape == ShapeType.TRIANGLE)
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            else Modifier
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_triangle_24),
                            contentDescription = "Triangle",
                            tint = if (selectedShape == ShapeType.TRIANGLE) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onShapeSelected(ShapeType.LINE) },
                        modifier = Modifier.then(
                            if (selectedShape == ShapeType.LINE)
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            else Modifier
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_line_24),
                            contentDescription = "Line",
                            tint = if (selectedShape == ShapeType.LINE) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
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
                        shape = CircleShape,
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

