package com.sd.lib.compose.swich

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun FSwitchBackground(
    progress: Float,
    modifier: Modifier = Modifier,
    colorUnchecked: Color = Color(0xFFE3E3E3),
    colorChecked: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(50)
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorUnchecked, shape)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .alpha(progress)
                .background(colorChecked, shape)
        )
    }
}

@Composable
fun FSwitchThumb(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    paddings: PaddingValues = PaddingValues(2.dp),
    shape: Shape = RoundedCornerShape(50)
) {
    Box(
        modifier = modifier
            .aspectRatio(1f, true)
            .padding(paddings)
            .background(
                color = color,
                shape = shape,
            )
    )
}