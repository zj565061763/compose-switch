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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    val coroutineScope = rememberCoroutineScope()
    val state = remember { FSwitchState(checked, coroutineScope) }.also {
        it.onCheckedChange = onCheckedChange
        it.setBoxSize(boxSize.width.toFloat())
        it.setThumbSize(thumbSize.width.toFloat())
        it.HandleComposable(checked)
    }


    var hasMove by remember { mutableStateOf(false) }

    Box(modifier = modifier
        .width(50.dp)
        .height(25.dp)
        .onSizeChanged { boxSize = it }
        .run {
            if (state.isReady && enabled) {
                fPointerChange(
                    onStart = {
                        enableVelocity = true
                        hasMove = false
                    },
                    onMove = { input ->
                        if (!input.isConsumed && pointerCount == 1) {
                            val change = input.positionChange()
                            if (state.handleDrag(change.x)) {
                                input.consume()
                                hasMove = true
                            }
                        }
                    },
                    onUp = { input ->
                        if (pointerCount == 1) {
                            if (hasMove) {
                                val velocity = getPointerVelocity(input.id).x
                                state.handleFling(velocity)
                            } else {
                                if (!input.isConsumed && maxPointerCount == 1) {
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

private class FSwitchState(
    checked: Boolean,
    scope: CoroutineScope,
) {
    private val _scope = scope
    var onCheckedChange: ((Boolean) -> Unit)? = null

    private var _boxSize: Float by mutableStateOf(0f)
    private var _thumbSize: Float by mutableStateOf(0f)
    val isReady: Boolean by derivedStateOf { _boxSize > 0 && _thumbSize > 0 }

    private val _uncheckedOffset = 0f
    private var _checkedOffset by mutableStateOf(0f)

    private var _isChecked = checked
        set(value) {
            if (field != value) {
                field = value
                _animJob?.cancel()
            }
        }

    private val _animOffset = Animatable(boundsOffset(checked))
    private var _animJob: Job? = null

    var currentOffset: Float by mutableStateOf(boundsOffset(checked))
        private set

    val progress: Float by derivedStateOf {
        val currentOffset = currentOffset
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

    private var _internalOffset = currentOffset
        set(value) {
            val newValue = value.coerceIn(_uncheckedOffset, _checkedOffset)
            if (field != newValue) {
                field = newValue
                currentOffset = newValue
            }
        }

    fun setBoxSize(size: Float) {
        _boxSize = size
        updateCheckedOffset()
    }

    fun setThumbSize(size: Float) {
        _thumbSize = size
        updateCheckedOffset()
    }

    private fun updateCheckedOffset() {
        val delta = (_boxSize - _thumbSize).coerceAtLeast(0f)
        _checkedOffset = _uncheckedOffset + delta
    }

    @Composable
    fun HandleComposable(checked: Boolean) {
        _isChecked = checked
        LaunchedEffect(
            isReady,
            _isChecked,
            _uncheckedOffset,
            _checkedOffset,
        ) {
            updateOffsetByState()
        }
    }

    fun handleDrag(delta: Float): Boolean {
        if (_animJob?.isActive == true) return false
        val oldOffset = _internalOffset
        _internalOffset += delta
        return _internalOffset != oldOffset
    }

    fun handleFling(velocity: Float) {
        if (_animJob?.isActive == true) return
        val offset = if (velocity.absoluteValue > 1000f) {
            if (velocity > 0) _checkedOffset else _uncheckedOffset
        } else {
            boundsValue(_internalOffset, _uncheckedOffset, _checkedOffset)
        }
        animateToOffset(offset, velocity)
    }

    fun handleClick() {
        if (_animJob?.isActive == true) return
        val offset = boundsOffset(!_isChecked)
        animateToOffset(offset)
    }

    private fun animateToOffset(offset: Float, initialVelocity: Float? = null) {
        _scope.launch {
            _animOffset.snapTo(_internalOffset)
            _animOffset.animateTo(
                targetValue = offset,
                initialVelocity = initialVelocity ?: _animOffset.velocity,
            ) { _internalOffset = value }

            notifyCallback()
            delay(500)
            updateOffsetByState()
        }.also {
            _animJob = it
        }
    }

    private fun updateOffsetByState() {
        if (isReady && !_animOffset.isRunning) {
            _internalOffset = boundsOffset(_isChecked)
            logMsg { "updateOffsetByState" }
        }
    }

    private fun boundsOffset(isChecked: Boolean): Float {
        return if (isChecked) _checkedOffset else _uncheckedOffset
    }

    private fun notifyCallback() {
        when (_internalOffset) {
            _uncheckedOffset -> onCheckedChange?.invoke(false)
            _checkedOffset -> onCheckedChange?.invoke(true)
        }
    }
}

private fun boundsValue(value: Float, min: Float, max: Float): Float {
    require(value in min..max)
    val center = (min + max) / 2
    return if (value > center) max else min
}

internal inline fun logMsg(block: () -> String) {
    Log.i("FSwitch", block())
}