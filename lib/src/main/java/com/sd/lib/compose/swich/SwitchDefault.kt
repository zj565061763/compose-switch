package com.sd.lib.compose.swich

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun SwitchDefaultBackground(
  modifier: Modifier = Modifier,
  progress: Float,
  colorUnchecked: Color = MaterialTheme.colorScheme.onSurface.copy(0.15f),
  colorChecked: Color = MaterialTheme.colorScheme.primary,
  shape: Shape = CircleShape,
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
fun SwitchDefaultThumb(
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.surface,
  paddings: PaddingValues = PaddingValues(2.dp),
  shape: Shape = CircleShape,
  content: @Composable BoxScope.() -> Unit = {},
) {
  Box(
    modifier = modifier
      .aspectRatio(1f, true)
      .padding(paddings)
      .background(color, shape),
    contentAlignment = Alignment.Center,
    content = content,
  )
}