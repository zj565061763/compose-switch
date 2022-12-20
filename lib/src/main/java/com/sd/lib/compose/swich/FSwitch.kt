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
    checked: Boolean,
    interactiveMode: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    background: @Composable (progress: Float) -> Unit = { FSwitchBackground(progress = it) },
    thumb: @Composable (progress: Float) -> Unit = { FSwitchThumb() },
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var thumbSize by remember { mutableStateOf(IntSize.Zero) }

    val coroutineScope = rememberCoroutineScope()
    val state = remember { FSwitchState(checked, coroutineScope) }.also {
        it.onCheckedChange = onCheckedChange
        it.setInteractiveMode(interactiveMode)
        it.setBoxSize(boxSize.width.toFloat())
        it.setThumbSize(thumbSize.width.toFloat())
        it.HandleComposable(checked)
    }


    var hasDrag by remember { mutableStateOf(false) }

    Box(modifier = modifier
        .width(50.dp)
        .height(25.dp)
        .onSizeChanged { boxSize = it }
        .run {
            if (state.isReady && enabled) {
                fPointerChange(
                    onStart = {
                        enableVelocity = true
                        hasDrag = false
                    },
                    onMove = { input ->
                        if (!input.isConsumed && pointerCount == 1) {
                            val change = input.positionChange()
                            if (state.handleDrag(change.x)) {
                                input.consume()
                                hasDrag = true
                            }
                        }
                    },
                    onUp = { input ->
                        if (pointerCount == 1) {
                            if (hasDrag) {
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

    private var _isFirst = true
    private var _interactiveMode = false
    private var _boxSize: Float by mutableStateOf(0f)
    private var _thumbSize: Float by mutableStateOf(0f)
    val isReady: Boolean by derivedStateOf { _boxSize > 0 && _thumbSize > 0 }

    private val _uncheckedOffset = 0f
    private var _checkedOffset = 0f

    private var _isChecked = checked
        set(value) {
            if (field != value) {
                field = value
                _animJob?.cancel()
            }
        }

    private val _animOffset = Animatable(boundsOffset(checked))
    private var _animJob: Job? = null

    var progress: Float by mutableStateOf(0f)
        private set

    var currentOffset: Float by mutableStateOf(boundsOffset(checked))
        private set

    private var _internalOffset = currentOffset
        set(value) {
            val newValue = value.coerceIn(_uncheckedOffset, _checkedOffset)
            if (field != newValue) {
                field = newValue
                currentOffset = newValue
                updateProgress()
            }
        }

    fun setInteractiveMode(interactive: Boolean) {
        _interactiveMode = interactive
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

    private fun updateProgress() {
        val checkedOffset = _checkedOffset
        val uncheckedOffset = _uncheckedOffset
        progress = if (checkedOffset > uncheckedOffset) {
            val currentOffset = currentOffset
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
        if (!_interactiveMode) return false
        if (_checkedOffset == _uncheckedOffset) return false
        if (_animJob?.isActive == true) return false

        val oldOffset = _internalOffset
        _internalOffset += delta
        return _internalOffset != oldOffset
    }

    fun handleFling(velocity: Float) {
        if (_checkedOffset == _uncheckedOffset) return
        if (_animJob?.isActive == true) return

        val offset = if (velocity.absoluteValue > 1000f) {
            if (velocity > 0) _checkedOffset else _uncheckedOffset
        } else {
            boundsValue(_internalOffset, _uncheckedOffset, _checkedOffset)
        }
        animateToOffsetInteractive(offset, velocity)
    }

    fun handleClick() {
        if (_checkedOffset == _uncheckedOffset) return
        if (_animJob?.isActive == true) return

        if (_interactiveMode) {
            val offset = boundsOffset(!_isChecked)
            animateToOffsetInteractive(offset)
        } else {
            onCheckedChange?.invoke(!_isChecked)
        }
    }

    private fun animateToOffsetInteractive(
        offset: Float,
        initialVelocity: Float? = null,
    ) {
        animateToOffset(
            offset = offset,
            initialVelocity = initialVelocity,
        ) {
            if (notifyCallbackByOffset()) {
                delay(500)
            }
            _internalOffset = boundsOffset(_isChecked)
        }
    }

    private fun animateToOffset(
        offset: Float,
        initialVelocity: Float? = null,
        onFinish: (suspend () -> Unit)? = null,
    ) {
        _scope.launch {
            _animOffset.snapTo(_internalOffset)
            _animOffset.animateTo(
                targetValue = offset,
                initialVelocity = initialVelocity ?: _animOffset.velocity,
            ) { _internalOffset = value }
            onFinish?.invoke()
        }.also {
            _animJob = it
        }
    }

    private fun updateOffsetByState() {
        if (!isReady) return

        if (_isFirst) {
            _isFirst = false
            _internalOffset = boundsOffset(_isChecked)
            return
        }

        val offset = boundsOffset(_isChecked)
        if (_animOffset.isRunning && _animOffset.targetValue != offset) {
            _animJob?.cancel()
        }
        animateToOffset(offset)
    }

    private fun boundsOffset(isChecked: Boolean): Float {
        return if (isChecked) _checkedOffset else _uncheckedOffset
    }

    private fun notifyCallbackByOffset(): Boolean {
        if (_checkedOffset == _uncheckedOffset) return false
        val checked = _internalOffset == _checkedOffset
        if (checked == _isChecked) return false
        val callback = onCheckedChange ?: return false
        callback(checked)
        return true
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