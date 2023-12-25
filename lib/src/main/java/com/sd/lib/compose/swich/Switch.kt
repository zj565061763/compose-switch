package com.sd.lib.compose.swich

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sd.lib.compose.gesture.fConsume
import com.sd.lib.compose.gesture.fHasConsumed
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
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
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
        interactionSource = interactionSource,
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
    interactionSource: MutableInteractionSource,
    onCheckedChange: (Boolean) -> Unit,
) {
    val boxSizeState = remember { mutableStateOf(IntSize.Zero) }
    val thumbSizeState = remember { mutableStateOf(IntSize.Zero) }

    state.let {
        it.isEnabled = enabled
        it.onCheckedChange = onCheckedChange
        it.draggable = draggable
        if (isHorizontal) {
            it.setSize(boxSizeState.value.width, thumbSizeState.value.width)
        } else {
            it.setSize(boxSizeState.value.height, thumbSizeState.value.height)
        }
        it.HandleComposable(checked)
    }

    var hasDrag by remember { mutableStateOf(false) }
    var hasMove by remember { mutableStateOf(false) }

    Box(modifier = modifier
        .let {
            if (isHorizontal) {
                it.defaultMinSize(minWidth = 50.dp, minHeight = 25.dp)
            } else {
                it.defaultMinSize(minWidth = 25.dp, minHeight = 50.dp)
            }
        }
        .onSizeChanged {
            boxSizeState.value = it
        }
        .toggleable(
            value = checked,
            onValueChange = onCheckedChange,
            enabled = enabled,
            role = Role.Switch,
            interactionSource = interactionSource,
            indication = null
        )
        .let {
            if (enabled) {
                it.fPointer(
                    onStart = {
                        this.enableVelocity = true
                        this.calculatePan = true
                        hasDrag = false
                        hasMove = false
                    },
                    onCalculate = {
                        if (!currentEvent.fHasConsumed()) {
                            hasMove = true
                            val change = if (isHorizontal) this.pan.x else this.pan.y
                            if (state.handleDrag(change)) {
                                currentEvent.fConsume()
                                hasDrag = true
                            }
                        }
                    },
                    onUp = { input ->
                        if (pointerCount == 1) {
                            if (hasDrag) {
                                getPointerVelocity(input.id)?.let { velocity ->
                                    state.handleFling(if (isHorizontal) velocity.x else velocity.y)
                                }
                            } else {
                                if (!input.isConsumed && maxPointerCount == 1 && !hasMove) {
                                    val clickTime = input.uptimeMillis - input.previousUptimeMillis
                                    if (clickTime < 200) {
                                        state.handleClick()
                                    }
                                }
                            }
                        }
                    },
                )
            } else {
                it
            }
        }) {

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