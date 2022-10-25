package com.sd.lib.compose.swich

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sd.lib.compose.gesture.fPointerChange
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun FSwitch(
    checked: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    background: @Composable (progress: Float) -> Unit = { FSwitchBackground(progress = it) },
    thumb: @Composable (progress: Float) -> Unit = { FSwitchThumb() },
    onCheckedChange: (Boolean) -> Unit,
) {
    val state = remember { FSwitchState(checked) }.also {
        it.onCheckedChange = onCheckedChange
        it.handleComposable(checked)
    }

    val enabledUpdated by rememberUpdatedState(enabled)
    val coroutineScope = rememberCoroutineScope()
    var hasMove by remember { mutableStateOf(false) }

    Box(modifier = modifier
        .width(50.dp)
        .height(25.dp)
        .onPlaced {
            state.boxSize = it.size.width.toFloat()
        }
        .run {
            if (state.isReady && enabledUpdated) {
                fPointerChange(
                    onStart = {
                        enableVelocity = true
                        hasMove = false
                    },
                    onMove = { input ->
                        if (!input.isConsumed && input.id == currentEvent?.changes?.first()?.id) {
                            val change = input.positionChange()
                            input.consume()
                            hasMove = true
                            state.handleDrag(change.x)
                        }
                    },
                    onUp = { input ->
                        if (!input.isConsumed && pointerCount == 1) {
                            if (hasMove) {
                                val velocity = getPointerVelocity(input.id).x
                                coroutineScope.launch { state.handleFling(velocity) }
                            } else {
                                if (maxPointerCount == 1 && (input.uptimeMillis - input.previousUptimeMillis) < 200) {
                                    coroutineScope.launch { state.handleClick() }
                                }
                            }
                        }
                    },
                )
            } else {
                this
            }
        }
    ) {
        background(state.progress)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .onPlaced {
                    state.thumbSize = it.size.width.toFloat()
                }
                .offset {
                    IntOffset(state.currentOffset.roundToInt(), 0)
                },
            contentAlignment = Alignment.Center,
        ) {
            thumb(state.progress)
        }
    }
}

private class FSwitchState(
    checked: Boolean
) {
    var isChecked: Boolean by mutableStateOf(checked)
    var onCheckedChange: ((Boolean) -> Unit)? = null

    var boxSize: Float by mutableStateOf(0f)
    var thumbSize: Float by mutableStateOf(0f)
    val isReady: Boolean by derivedStateOf { boxSize > 0 && thumbSize > 0 }

    val uncheckedOffset: Float by mutableStateOf(0f)
    val checkedOffset: Float by derivedStateOf {
        val delta = (boxSize - thumbSize).coerceAtLeast(0f)
        uncheckedOffset + delta
    }

    var currentOffset: Float by mutableStateOf(boundsOffset(isChecked))
        private set

    val progress: Float by derivedStateOf {
        if (checkedOffset > uncheckedOffset) {
            when {
                currentOffset <= uncheckedOffset -> 0f
                currentOffset >= checkedOffset -> 1f
                else -> {
                    val total = checkedOffset - uncheckedOffset
                    val current = currentOffset - uncheckedOffset
                    (current / total).coerceIn(0f, 1f)
                }
            }
        } else {
            0f
        }
    }

    private val animatable = Animatable(boundsOffset(isChecked))

    @Composable
    fun handleComposable(checked: Boolean) {
        LaunchedEffect(checked) {
            isChecked = checked
        }

        LaunchedEffect(
            isReady,
            isChecked,
            uncheckedOffset,
            checkedOffset,
        ) {
            updateOffsetByState()
        }
    }

    fun handleDrag(delta: Float) {
        val offset = currentOffset + delta
        currentOffset = offset.coerceIn(uncheckedOffset, checkedOffset)
    }

    suspend fun handleFling(velocity: Float) {
        val offset = if (velocity.absoluteValue > 200f) {
            if (velocity > 0) checkedOffset else uncheckedOffset
        } else {
            boundsValue(currentOffset, uncheckedOffset, checkedOffset)
        }
        animateToOffset(offset, velocity)
    }

    suspend fun handleClick() {
        if (animatable.isRunning) return
        val offset = boundsOffset(!isChecked)
        animateToOffset(offset)
    }

    private suspend fun animateToOffset(offset: Float, initialVelocity: Float? = null) {
        if (currentOffset == offset) {
            notifyCallback()
            return
        }

        animatable.snapTo(currentOffset)
        animatable.animateTo(
            targetValue = offset,
            initialVelocity = initialVelocity ?: animatable.velocity,
        ) { currentOffset = value.coerceIn(uncheckedOffset, checkedOffset) }
        notifyCallback()
    }

    private fun updateOffsetByState() {
        if (isReady && !animatable.isRunning) {
            currentOffset = boundsOffset(isChecked)
        }
    }

    private fun boundsOffset(isChecked: Boolean): Float {
        return if (isChecked) checkedOffset else uncheckedOffset
    }

    private fun notifyCallback() {
        when (currentOffset) {
            uncheckedOffset -> {
                if (isChecked) {
                    isChecked = false
                    onCheckedChange?.invoke(false)
                }
            }
            checkedOffset -> {
                if (!isChecked) {
                    isChecked = true
                    onCheckedChange?.invoke(true)
                }
            }
        }
    }
}

private fun boundsValue(value: Float, minBounds: Float, maxBounds: Float): Float {
    require(value in minBounds..maxBounds)
    val center = (maxBounds - minBounds) / 2
    return if (value > center) maxBounds else minBounds
}