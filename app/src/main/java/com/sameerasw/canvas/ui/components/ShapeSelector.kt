package com.sameerasw.canvas.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.canvas.R
import com.sameerasw.canvas.model.ShapeType

@Composable
fun ShapeSelector(
    selectedShape: ShapeType,
    onShapeSelected: (ShapeType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(8.dp),
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
}
