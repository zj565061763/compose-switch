package com.sd.lib.compose.swich

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun FSwitch(
    modifier: Modifier = Modifier,
    checked: Boolean,
    state: FSwitchState = rememberFSwitchState(),
    enabled: Boolean = true,
    background: @Composable (FSwitchState) -> Unit = { FSwitchBackground(progress = it.progress) },
    thumb: @Composable (FSwitchState) -> Unit = { FSwitchThumb() },
    onCheckedChange: (Boolean) -> Unit,
) {
    Switch(
        modifier = modifier,
        checked = checked,
        state = state,
        enabled = enabled,
        background = background,
        thumb = thumb,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
private fun Switch(
    modifier: Modifier,
    checked: Boolean,
    state: FSwitchState,
    enabled: Boolean,
    background: @Composable (FSwitchState) -> Unit,
    thumb: @Composable (FSwitchState) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
) {
    val boxSizeState = remember { mutableStateOf(IntSize.Zero) }
    val thumbSizeState = remember { mutableStateOf(IntSize.Zero) }

    state.apply {
        this.enabled = enabled
        this.onCheckedChange = onCheckedChange
        this.density = LocalDensity.current
        this.setSize(boxSizeState.value.width, thumbSizeState.value.width)
        this.HandleComposable(checked)
    }

    Box(modifier = modifier
        .run {
            if (enabled) {
                this.handleGesture(state = state)
            } else {
                this
            }
        }
        .defaultMinSize(minWidth = 50.dp, minHeight = 25.dp)
        .onSizeChanged {
            boxSizeState.value = it
        }
        .semantics {
            this.role = Role.Switch
            this.toggleableState = ToggleableState(checked)
        }
    ) {
        // Background
        BackgroundBox(
            modifier = Modifier.matchParentSize(),
            state = state,
            background = background,
        )

        // Thumb
        ThumbBox(
            state = state,
            boxSizeState = boxSizeState,
            thumbSizeState = thumbSizeState,
            thumb = thumb,
        )
    }
}

private fun Modifier.handleGesture(
    state: FSwitchState,
): Modifier = this.composed {

    val velocityTracker = remember { VelocityTracker() }

    pointerInput(state) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)

            var hasDrag = false
            velocityTracker.resetTracking()

            // finishOrCancel，true表示正常结束，false表示取消
            val finishOrCancel = horizontalDrag(down.id) { input ->
                val delta = input.positionChange().x
                if (state.handleDrag(delta)) {
                    if (!hasDrag) {
                        hasDrag = true
                    }
                }
                if (hasDrag) {
                    velocityTracker.addPointerInputChange(input)
                    input.consume()
                }
            }

            if (hasDrag) {
                if (finishOrCancel) {
                    val velocity = velocityTracker.calculateVelocity().x
                    state.handleFling(velocity)
                } else {
                    state.handleDragCancel()
                }
            }
        }
    }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {
                state.handleClick()
            }
        )
}

@Composable
private fun BackgroundBox(
    modifier: Modifier = Modifier,
    state: FSwitchState,
    background: @Composable (FSwitchState) -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        background(state)
    }
}

@Composable
private fun ThumbBox(
    state: FSwitchState,
    boxSizeState: State<IntSize>,
    thumbSizeState: MutableState<IntSize>,
    thumb: @Composable (FSwitchState) -> Unit,
) {
    val density = LocalDensity.current
    val height = with(density) { boxSizeState.value.height.toDp() }

    Box(
        modifier = Modifier
            .height(height)
            .graphicsLayer {
                this.alpha = if (state.hasInitialized) 1f else 0f
            }
            .onSizeChanged {
                thumbSizeState.value = it
            }
            .offset {
                IntOffset(state.thumbOffset, 0)
            },
        contentAlignment = Alignment.Center,
    ) {
        thumb(state)
    }
}