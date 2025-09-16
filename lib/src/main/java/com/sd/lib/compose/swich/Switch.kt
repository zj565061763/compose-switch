package com.sd.lib.compose.swich

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun FSwitch(
  modifier: Modifier = Modifier,
  state: FSwitchState,
  background: @Composable () -> Unit = { SwitchDefaultBackground(progress = state.progress) },
  thumb: @Composable () -> Unit = { SwitchDefaultThumb() },
  enabled: Boolean = true,
  draggable: Boolean = true,
) {
  val density = LocalDensity.current
  val inspectionMode = LocalInspectionMode.current

  BoxWithConstraints(
    modifier = modifier
      .defaultMinSize(minWidth = 48.dp, minHeight = 24.dp)
      .let {
        if (enabled) {
          it.clickable(
            interactionSource = null,
            indication = null,
            role = Role.Switch,
            onClick = { state.handleClick() },
          )
        } else it
      }
      .let {
        if (enabled && draggable) {
          it.handleDragGesture(state)
        } else it
      },
  ) {
    state.setSize(
      boxSize = with(density) { minWidth.toPx() },
      thumbSize = with(density) { minHeight.toPx() },
    )

    if (inspectionMode) {
      state.updateStateWhenInspectionMode()
    }

    // Background
    Box(
      modifier = Modifier.matchParentSize(),
      contentAlignment = Alignment.Center,
    ) {
      background()
    }

    // Thumb
    Box(
      modifier = Modifier
        .height(minHeight)
        .graphicsLayer { this.alpha = if (state.hasInitialized) 1f else 0f }
        .offset { IntOffset(state.thumbOffset, 0) },
      contentAlignment = Alignment.Center,
    ) {
      thumb()
    }
  }
}

private fun Modifier.handleDragGesture(state: FSwitchState): Modifier = composed {
  val coroutineScope = rememberCoroutineScope()
  val velocityTracker = remember { VelocityTracker() }
  pointerInput(state) {
    awaitEachGesture {
      val down = awaitFirstDown()

      val touchSlopChange = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
      if (touchSlopChange == null) return@awaitEachGesture

      (touchSlopChange.position - down.position).also { initialDistance ->
        state.handleDrag(initialDistance.x)
      }

      val finish = drag(touchSlopChange.id) { change ->
        val dragAmount = change.positionChange()
        if (state.handleDrag(dragAmount.x)) {
          change.consume()
          velocityTracker.addPointerInputChange(change)
        }
      }

      if (finish) {
        val velocity = velocityTracker.calculateVelocity().x
        coroutineScope.launch { state.handleFling(velocity) }
      } else {
        state.handleDragCancel()
      }
    }
  }
}