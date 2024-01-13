package com.sd.lib.compose.swich

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.positionChanged
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
import com.sd.lib.compose.gesture.fConsume
import com.sd.lib.compose.gesture.fPointer

@Composable
fun FSwitch(
    modifier: Modifier = Modifier,
    checked: Boolean,
    state: FSwitchState = rememberFSwitchState(),
    draggable: Boolean = false,
    enabled: Boolean = true,
    background: @Composable (FSwitchState) -> Unit = { FSwitchBackground(progress = it.progress) },
    thumb: @Composable (FSwitchState) -> Unit = { FSwitchThumb() },
    onCheckedChange: (Boolean) -> Unit,
) {
    Switch(
        modifier = modifier,
        checked = checked,
        state = state,
        isHorizontal = true,
        draggable = draggable,
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
    isHorizontal: Boolean,
    draggable: Boolean,
    enabled: Boolean,
    background: @Composable (FSwitchState) -> Unit,
    thumb: @Composable (FSwitchState) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
) {
    val boxSizeState = remember { mutableStateOf(IntSize.Zero) }
    val thumbSizeState = remember { mutableStateOf(IntSize.Zero) }

    state.apply {
        this.isEnabled = enabled
        this.onCheckedChange = onCheckedChange
        this.draggable = draggable
        this.density = LocalDensity.current
        if (isHorizontal) {
            this.setSize(boxSizeState.value.width, thumbSizeState.value.width)
        } else {
            this.setSize(boxSizeState.value.height, thumbSizeState.value.height)
        }
        this.HandleComposable(checked)
    }

    Box(modifier = modifier
        // handle click
        .let { m ->
            if (enabled) {
                m.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Switch,
                    onClick = {
                        state.handleClick()
                    },
                )
            } else {
                m
            }
        }
        // handle drag
        .let { m ->
            if (enabled && draggable) {
                m.handleDrag(
                    state = state,
                    isHorizontal = isHorizontal,
                )
            } else {
                m
            }
        }
        .let { m ->
            if (isHorizontal) {
                m.defaultMinSize(minWidth = 50.dp, minHeight = 25.dp)
            } else {
                m.defaultMinSize(minWidth = 25.dp, minHeight = 50.dp)
            }
        }
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
            isHorizontal = isHorizontal,
            boxSizeState = boxSizeState,
            thumbSizeState = thumbSizeState,
            thumb = thumb,
        )
    }
}

private fun Modifier.handleDrag(
    state: FSwitchState,
    isHorizontal: Boolean,
): Modifier = composed {

    var isDrag by remember { mutableStateOf(false) }

    fPointer(
        onStart = {
            this.calculatePan = true
            isDrag = false
        },
        onCalculate = {
            if (!isDrag) {
                val positionChanged = currentEvent.changes.any { it.positionChanged() }
                if (!positionChanged) {
                    cancelPointer()
                    return@fPointer
                }
            }

            val change = if (isHorizontal) this.pan.x else this.pan.y
            if (state.handleDrag(change)) {
                isDrag = true
            }

            if (isDrag) {
                currentEvent.fConsume { it.positionChanged() }
            }
        },
        onMove = {
            if (isDrag) {
                velocityAdd(it)
            }
        },
        onUp = { input ->
            if (pointerCount == 1) {
                if (isDrag) {
                    velocityGet(input.id)?.let { velocity ->
                        state.handleFling(if (isHorizontal) velocity.x else velocity.y)
                    }
                }
            }
        },
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
    modifier: Modifier = Modifier,
    state: FSwitchState,
    isHorizontal: Boolean,
    boxSizeState: State<IntSize>,
    thumbSizeState: MutableState<IntSize>,
    thumb: @Composable (FSwitchState) -> Unit,
) {
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .let {
                if (isHorizontal) {
                    val height = with(density) { boxSizeState.value.height.toDp() }
                    it.height(height)
                } else {
                    val width = with(density) { boxSizeState.value.width.toDp() }
                    it.width(width)
                }
            }
            .graphicsLayer {
                this.alpha = if (state.hasInitialized) 1f else 0f
            }
            .onSizeChanged {
                thumbSizeState.value = it
            }
            .offset {
                if (isHorizontal) {
                    IntOffset(state.thumbOffset, 0)
                } else {
                    IntOffset(0, state.thumbOffset)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        thumb(state)
    }
}