package com.sd.lib.compose.swch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun FSwitchBackground(
    modifier: Modifier = Modifier,
    progress: Float,
    colorUnchecked: Color = Color(0xFFE3E3E3),
    colorChecked: Color = Color(0xFF4AD863),
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
    shape: Shape = RoundedCornerShape(50)
) {
    Box(
        modifier = modifier
            .aspectRatio(1f, true)
            .padding(2.dp)
            .background(
                color = color,
                shape = shape,
            )
    )
}