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
import com.sameerasw.canvas.model.PenStyle

@Composable
fun PenStyleSelector(
    selectedStyle: PenStyle,
    onStyleSelected: (PenStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(8.dp),
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
                painter = painterResource(id = R.drawable.rounded_stylus_fountain_pen_24),
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
                painter = painterResource(id = R.drawable.rounded_stylus_fountain_pen_24),
                contentDescription = "Rough",
                tint = if (selectedStyle == PenStyle.ROUGH) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
