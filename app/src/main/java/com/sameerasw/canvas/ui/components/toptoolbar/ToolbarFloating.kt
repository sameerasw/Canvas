package com.sameerasw.canvas.ui.components.toptoolbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.canvas.R
import com.sameerasw.canvas.model.ToolType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToolbarFloating(
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    currentTool: ToolType,
    onHandTool: () -> Unit,
    onPenTool: () -> Unit,
    onEraserTool: () -> Unit,
    onTextTool: () -> Unit,
    onArrowTool: () -> Unit,
    onShapeTool: () -> Unit
) {
    // Animated values with bouncy spring animations
    val buttonScale by animateFloatAsState(
        targetValue = if (expanded) 0.95f else 1.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )

    val iconWidth by animateDpAsState(
        targetValue = if (expanded) 48.dp else 24.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "icon_width"
    )

    // Add bouncy animation to toolbar container
    val toolbarScale by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.75f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "toolbar_scale"
    )

    HorizontalFloatingToolbar(
        modifier = Modifier
            .graphicsLayer {
                scaleX = toolbarScale
                scaleY = toolbarScale
            }
            .then(
                if (expanded) Modifier.fillMaxWidth(0.8f)
                else Modifier
            ),
        expanded = expanded,
        content = {
            FilledIconButton(
                modifier = Modifier
                    .width(64.dp)
                    .height(48.dp)
                    .scale(buttonScale),
                onClick = onExpandToggle
            ) {
                // Animated icon morph with crossfade
                AnimatedContent(
                    targetState = if (expanded) "expanded" to R.drawable.icon else currentTool.name to getToolIcon(currentTool),
                    transitionSpec = {
                        (fadeIn(animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )) +
                         scaleIn(initialScale = 0.85f, animationSpec = spring(
                             dampingRatio = Spring.DampingRatioMediumBouncy,
                             stiffness = Spring.StiffnessMediumLow
                         ))) togetherWith
                        (fadeOut(animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessHigh
                        )) +
                         scaleOut(targetScale = 0.85f, animationSpec = tween(150)))
                    },
                    label = "icon_morph"
                ) { (_, iconRes) ->
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = "Toggle toolbar",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.width(iconWidth)
                    )
                }
            }
        },
        trailingContent = {
            if (expanded) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    // Hand tool
                    AnimatedToolButton(
                        isExpanded = expanded,
                        isSelected = currentTool == ToolType.HAND,
                        iconRes = R.drawable.rounded_back_hand_24,
                        contentDescription = "Hand tool",
                        onClick = onHandTool
                    )

                    // Pen tool
                    AnimatedToolButton(
                        isExpanded = expanded,
                        isSelected = currentTool == ToolType.PEN,
                        iconRes = R.drawable.rounded_stylus_fountain_pen_24,
                        contentDescription = "Pen tool",
                        onClick = onPenTool
                    )

                    // Arrow tool
                    AnimatedToolButton(
                        isExpanded = expanded,
                        isSelected = currentTool == ToolType.ARROW,
                        iconRes = R.drawable.round_arrow_upward_24,
                        contentDescription = "Arrow tool",
                        onClick = onArrowTool
                    )

                    // Shape tool
                    AnimatedToolButton(
                        isExpanded = expanded,
                        isSelected = currentTool == ToolType.SHAPE,
                        iconRes = R.drawable.shape_line_24px,
                        contentDescription = "Shape tool",
                        onClick = onShapeTool
                    )

                    // Eraser tool
                    AnimatedToolButton(
                        isExpanded = expanded,
                        isSelected = currentTool == ToolType.ERASER,
                        iconRes = R.drawable.rounded_ink_eraser_24,
                        contentDescription = "Eraser tool",
                        onClick = onEraserTool
                    )

                    // Text tool
                    AnimatedToolButton(
                        isExpanded = expanded,
                        isSelected = currentTool == ToolType.TEXT,
                        iconRes = R.drawable.rounded_text_fields_24,
                        contentDescription = "Text tool",
                        onClick = onTextTool
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {

                    // Hand tool
                    AnimatedToolButton(
                        isExpanded = expanded,
                        isSelected = currentTool == ToolType.HAND,
                        iconRes = R.drawable.rounded_back_hand_24,
                        contentDescription = "Hand tool",
                        onClick = onHandTool
                    )

                    // Pen tool
                    AnimatedToolButton(
                        isExpanded = expanded,
                        isSelected = currentTool == ToolType.PEN,
                        iconRes = R.drawable.rounded_stylus_fountain_pen_24,
                        contentDescription = "Pen tool",
                        onClick = onPenTool
                    )

                    // Arrow tool
                    AnimatedToolButton(
                        isExpanded = expanded,
                        isSelected = currentTool == ToolType.ARROW,
                        iconRes = R.drawable.rounded_arrow_forward_ios_24,
                        contentDescription = "Arrow tool",
                        onClick = onArrowTool
                    )

                    // Shape tool
                    AnimatedToolButton(
                        isExpanded = expanded,
                        isSelected = currentTool == ToolType.SHAPE,
                        iconRes = R.drawable.rounded_rectangle_24,
                        contentDescription = "Shape tool",
                        onClick = onShapeTool
                    )

                    // Eraser tool
                    AnimatedToolButton(
                        isExpanded = expanded,
                        isSelected = currentTool == ToolType.ERASER,
                        iconRes = R.drawable.rounded_ink_eraser_24,
                        contentDescription = "Eraser tool",
                        onClick = onEraserTool
                    )

                    // Text tool
                    AnimatedToolButton(
                        isExpanded = expanded,
                        isSelected = currentTool == ToolType.TEXT,
                        iconRes = R.drawable.rounded_text_fields_24,
                        contentDescription = "Text tool",
                        onClick = onTextTool
                    )
                }
            }
        }
    )
}

@Composable
private fun AnimatedToolButton(
    isExpanded: Boolean,
    isSelected: Boolean,
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    val buttonWidth by animateDpAsState(
        targetValue = if (isExpanded) 64.dp else 48.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "button_width"
    )

    val iconWidth by animateDpAsState(
        targetValue = if (isExpanded) 28.dp else 24.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "icon_width"
    )

    val borderAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "border_alpha"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "icon_scale"
    )

    IconButton(
        modifier = Modifier
            .width(buttonWidth)
            .then(
                if (borderAlpha > 0.01f)
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha),
                        shape = RoundedCornerShape(24.dp)
                    )
                else Modifier
            ),
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(iconWidth)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
    }
}

private fun getToolIcon(tool: ToolType): Int {
    return when (tool) {
        ToolType.HAND -> R.drawable.rounded_back_hand_24
        ToolType.PEN -> R.drawable.rounded_stylus_fountain_pen_24
        ToolType.ERASER -> R.drawable.rounded_ink_eraser_24
        ToolType.TEXT -> R.drawable.rounded_text_fields_24
        ToolType.ARROW -> R.drawable.rounded_arrow_forward_ios_24
        ToolType.SHAPE -> R.drawable.rounded_rectangle_24
    }
}

