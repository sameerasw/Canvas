package com.sameerasw.canvas.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeOut(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    ) {
        ColorPickerContent(
            selectedColor = selectedColor,
            onColorSelected = onColorSelected,
            modifier = modifier
        )
    }
}

@Composable
private fun ColorPickerContent(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    // Primary colors
    val primaryColors = listOf(
        Color.Black,
        Color.White,
        Color.Red,
        Color(0xFFFF6B00), // Orange
        Color.Yellow,
        Color.Green,
        Color.Blue,
        Color(0xFF9C27B0), // Purple
        Color.Gray,
        Color(0xFF795548) // Brown
    )

    // Pastel versions (lighter)
    val pastelColors = listOf(
        Color(0xFF505050), // Dark gray (pastel black)
        Color(0xFFF5F5F5), // Pastel white
        Color(0xFFFFB3B3), // Pastel red
        Color(0xFFFFD4A3), // Pastel orange
        Color(0xFFFFFFB3), // Pastel yellow
        Color(0xFFB3FFB3), // Pastel green
        Color(0xFFB3D9FF), // Pastel blue
        Color(0xFFE1B3FF), // Pastel purple
        Color(0xFFD3D3D3), // Pastel gray
        Color(0xFFD7CCC8) // Pastel brown
    )

    // Dark versions (darker)
    val darkColors = listOf(
        Color(0xFF000000), // Pure black
        Color(0xFFCCCCCC), // Dark white
        Color(0xFF990000), // Dark red
        Color(0xFFCC4400), // Dark orange
        Color(0xFFCCCC00), // Dark yellow
        Color(0xFF008000), // Dark green
        Color(0xFF000099), // Dark blue
        Color(0xFF660099), // Dark purple
        Color(0xFF404040), // Dark gray
        Color(0xFF3E2723) // Dark brown
    )

    // Combine all colors in order: primary, pastel, dark
    val allColors = primaryColors + pastelColors + darkColors

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        allColors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (selectedColor == color) 3.dp else 1.dp,
                        color = if (selectedColor == color) MaterialTheme.colorScheme.primary else Color.Gray,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(color) }
            )
        }
    }
}
