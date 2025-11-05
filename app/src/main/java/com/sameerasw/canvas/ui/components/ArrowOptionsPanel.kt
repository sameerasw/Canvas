package com.sameerasw.canvas.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ArrowOptionsPanel(
    visible: Boolean,
    arrowWidth: Float,
    onArrowWidthChange: (Float) -> Unit
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
                val circleSize = (arrowWidth * 1.5f).dp.coerceAtLeast(10.dp).coerceAtMost(64.dp)
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
                    value = arrowWidth,
                    onValueChange = onArrowWidthChange,
                    valueRange = 1f..48f,
                    modifier = Modifier.width(240.dp)
                )
            }
        }
    }
}
