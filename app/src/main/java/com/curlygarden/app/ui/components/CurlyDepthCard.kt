package com.curlygarden.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

private val RimWidth = 1.5.dp
private val HaloWidth = 3.dp
private const val RimDarken = 0.2f
private const val HaloAlpha = 0.18f

@Composable
fun CurlyDepthCard(
    color: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val rim = BorderStroke(RimWidth, lerp(color, Color.Black, RimDarken))
    val cardModifier = modifier
        .fillMaxWidth()
        .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
        .drawBehind {
            val halo = HaloWidth.toPx()
            val outline = shape.createOutline(
                Size(size.width + halo * 2, size.height + halo * 2),
                layoutDirection,
                this
            )
            translate(-halo, -halo) {
                drawOutline(outline, color.copy(alpha = HaloAlpha))
            }
        }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = shape,
            color = color,
            border = rim,
            modifier = cardModifier,
            content = content
        )
    } else {
        Surface(
            shape = shape,
            color = color,
            border = rim,
            modifier = cardModifier,
            content = content
        )
    }
}
