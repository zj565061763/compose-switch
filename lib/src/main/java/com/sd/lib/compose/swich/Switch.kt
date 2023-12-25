package com.sd.lib.compose.swich

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sd.lib.compose.gesture.fConsume
import com.sd.lib.compose.gesture.fHasConsumed
import com.sd.lib.compose.gesture.fPointer
import kotlin.math.roundToInt

@SuppressLint("ModifierParameter")
@Composable
fun FSwitch(
    modifier: Modifier = Modifier,
    checked: Boolean,
    state: FSwitchState = rememberFSwitchState(),
    interactiveMode: Boolean = false,
    enabled: Boolean = true,
    background: @Composable (progress: Float) -> Unit = { FSwitchBackground(progress = it) },
    thumb: @Composable (progress: Float) -> Unit = { FSwitchThumb() },
    onCheckedChange: (Boolean) -> Unit,
) {
    Switch(
        modifier = modifier,
        checked = checked,
        state = state,
        isHorizontal = true,
        interactiveMode = interactiveMode,
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
    interactiveMode: Boolean,
    enabled: Boolean,
    background: @Composable (progress: Float) -> Unit,
    thumb: @Composable (progress: Float) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var thumbSize by remember { mutableStateOf(IntSize.Zero) }

    val density = LocalDensity.current

    state.let {
        it.isEnabled = enabled
        it.onCheckedChange = onCheckedChange
        it.interactiveMode = interactiveMode
        if (boxSize.hasSize() && thumbSize.hasSize()) {
            if (isHorizontal) {
                it.boxSize = boxSize.width.toFloat()
                it.thumbSize = thumbSize.width.toFloat()
            } else {
                it.boxSize = boxSize.height.toFloat()
                it.thumbSize = thumbSize.height.toFloat()
            }
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
            boxSize = it
        }
        .let {
            if (enabled && state.isReady) {
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
        Box(
            modifier = Modifier.matchParentSize(),
            contentAlignment = Alignment.Center,
        ) {
            background(state.progress)
        }

        // Thumb
        Box(
            modifier = Modifier
                .let {
                    if (isHorizontal) {
                        it.height(with(density) { boxSize.height.toDp() })
                    } else {
                        it.width(with(density) { boxSize.width.toDp() })
                    }
                }
                .graphicsLayer {
                    this.alpha = if (state.hasInitialized) 1f else 0f
                }
                .onSizeChanged {
                    thumbSize = it
                }
                .offset {
                    if (isHorizontal) {
                        IntOffset(state.thumbOffset.roundToInt(), 0)
                    } else {
                        IntOffset(0, state.thumbOffset.roundToInt())
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            thumb(state.progress)
        }
    }
}

private fun IntSize.hasSize(): Boolean {
    return this.width > 0 && this.height > 0
}