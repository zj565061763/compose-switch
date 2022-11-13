package com.sd.lib.compose.swich

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var thumbSize by remember { mutableStateOf(IntSize.Zero) }

    val state = remember { FSwitchState(checked) }.also {
        it.boxSize = boxSize.width.toFloat()
        it.thumbSize = thumbSize.width.toFloat()
        it.onCheckedChange = onCheckedChange
        it.handleComposable(checked)
    }

    val enabledUpdated by rememberUpdatedState(enabled)
    val coroutineScope = rememberCoroutineScope()
    var hasMove by remember { mutableStateOf(false) }

    Box(modifier = modifier
        .width(50.dp)
        .height(25.dp)
        .onSizeChanged { boxSize = it }
        .run {
            if (state.isReady && enabledUpdated) {
                fPointerChange(
                    onStart = {
                        enableVelocity = true
                        hasMove = false
                    },
                    onMove = { input ->
                        if (!input.isConsumed && pointerCount == 1) {
                            val change = input.positionChange()
                            input.consume()
                            hasMove = true
                            state.handleDrag(change.x)
                        }
                    },
                    onUp = { input ->
                        if (pointerCount == 1) {
                            if (hasMove) {
                                val velocity = getPointerVelocity(input.id).x
                                coroutineScope.launch { state.handleFling(velocity) }
                            } else {
                                if (!input.isConsumed && maxPointerCount == 1) {
                                    val clickTime = input.uptimeMillis - input.previousUptimeMillis
                                    if (clickTime < 200) {
                                        coroutineScope.launch { state.handleClick() }
                                    }
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
                .fillMaxHeight()
                .onSizeChanged { thumbSize = it }
                .offset { IntOffset(state.currentOffset.roundToInt(), 0) },
            contentAlignment = Alignment.Center,
        ) {
            thumb(state.progress)
        }
    }
}

private class FSwitchState(checked: Boolean) {
    var boxSize: Float by mutableStateOf(0f)
    var thumbSize: Float by mutableStateOf(0f)
    var onCheckedChange: ((Boolean) -> Unit)? = null

    val isReady: Boolean by derivedStateOf { boxSize > 0 && thumbSize > 0 }

    private val _uncheckedOffset by mutableStateOf(0f)
    private val _checkedOffset by derivedStateOf {
        val delta = (boxSize - thumbSize).coerceAtLeast(0f)
        _uncheckedOffset + delta
    }

    private var _isChecked by mutableStateOf(checked)
    private val _animOffset = Animatable(boundsOffset(checked))

    var currentOffset: Float by mutableStateOf(boundsOffset(checked))
        private set

    val progress: Float by derivedStateOf {
        val checkedOffset = _checkedOffset
        val uncheckedOffset = _uncheckedOffset
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


    @Composable
    fun handleComposable(checked: Boolean) {
        LaunchedEffect(checked) {
            _isChecked = checked
        }

        LaunchedEffect(
            isReady,
            _isChecked,
            _uncheckedOffset,
            _checkedOffset,
        ) {
            updateOffsetByState()
        }
    }

    fun handleDrag(delta: Float) {
        val offset = currentOffset + delta
        currentOffset = offset.coerceIn(_uncheckedOffset, _checkedOffset)
    }

    suspend fun handleFling(velocity: Float) {
        val offset = if (velocity.absoluteValue > 200f) {
            if (velocity > 0) _checkedOffset else _uncheckedOffset
        } else {
            boundsValue(currentOffset, _uncheckedOffset, _checkedOffset)
        }
        animateToOffset(offset, velocity)
    }

    suspend fun handleClick() {
        if (_animOffset.isRunning) return
        val offset = boundsOffset(!_isChecked)
        animateToOffset(offset)
    }

    private suspend fun animateToOffset(offset: Float, initialVelocity: Float? = null) {
        if (currentOffset == offset) {
            notifyCallback()
            return
        }

        _animOffset.snapTo(currentOffset)
        _animOffset.animateTo(
            targetValue = offset,
            initialVelocity = initialVelocity ?: _animOffset.velocity,
        ) { currentOffset = value.coerceIn(_uncheckedOffset, _checkedOffset) }
        notifyCallback()
    }

    private fun updateOffsetByState() {
        if (isReady && !_animOffset.isRunning) {
            currentOffset = boundsOffset(_isChecked)
        }
    }

    private fun boundsOffset(isChecked: Boolean): Float {
        return if (isChecked) _checkedOffset else _uncheckedOffset
    }

    private fun notifyCallback() {
        when (currentOffset) {
            _uncheckedOffset -> {
                if (_isChecked) {
                    _isChecked = false
                    onCheckedChange?.invoke(false)
                }
            }
            _checkedOffset -> {
                if (!_isChecked) {
                    _isChecked = true
                    onCheckedChange?.invoke(true)
                }
            }
        }
    }
}

private fun boundsValue(value: Float, minBounds: Float, maxBounds: Float): Float {
    require(value in minBounds..maxBounds)
    val center = (maxBounds + minBounds) / 2
    return if (value > center) maxBounds else minBounds
}

internal fun logMsg(block: () -> String) {
    Log.i("FSwitch", block())
}