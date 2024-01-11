package com.sd.lib.compose.swich

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun rememberFSwitchState(): FSwitchState {
    val coroutineScope = rememberCoroutineScope()
    return remember(coroutineScope) {
        FSwitchState(coroutineScope)
    }
}

class FSwitchState(scope: CoroutineScope) {
    private val _scope = scope

    /** 是否可用 */
    var isEnabled: Boolean by mutableStateOf(true)
        internal set

    /** 当前进度[0-1] */
    var progress: Float by mutableFloatStateOf(0f)
        private set

    /** Thumb的偏移量 */
    internal var thumbOffset: Int by mutableIntStateOf(0)
        private set

    internal var hasInitialized: Boolean by mutableStateOf(false)
        private set

    internal lateinit var onCheckedChange: (Boolean) -> Unit
    internal var draggable = false

    private var _isChecked = false
    private val _uncheckedOffset: Float by mutableFloatStateOf(0f)
    private var _checkedOffset: Float by mutableFloatStateOf(0f)

    private val _animOffset = Animatable(0f)
    private var _animJob: Job? = null

    private var _internalOffset: Float = 0f
        set(value) {
            val newValue = value.coerceIn(_uncheckedOffset, _checkedOffset)
            if (field != newValue) {
                field = newValue
                thumbOffset = newValue.toInt()
                progress = calculateProgress()
            }
        }

    private fun calculateProgress(): Float {
        val checkedOffset = _checkedOffset
        val uncheckedOffset = _uncheckedOffset
        if (checkedOffset <= uncheckedOffset) return 0f

        val offset = thumbOffset
        return when {
            offset <= uncheckedOffset -> 0f
            offset >= checkedOffset -> 1f
            else -> {
                val current = offset - uncheckedOffset
                val total = checkedOffset - uncheckedOffset
                (current / total).coerceIn(0f, 1f)
            }
        }
    }

    internal fun setSize(boxSize: Int, thumbSize: Int) {
        val delta = boxSize - thumbSize
        _checkedOffset = _uncheckedOffset + delta.coerceAtLeast(0)
    }

    @Composable
    internal fun HandleComposable(checked: Boolean) {
        _isChecked = checked
        LaunchedEffect(checked, _uncheckedOffset, _checkedOffset) {
            if (_uncheckedOffset == _checkedOffset) {
                progress = 0f
                return@LaunchedEffect
            }

            if (!hasInitialized) {
                updateOffsetByStateStatic()
                hasInitialized = true
            } else {
                val offset = boundsOffset(_isChecked)
                animateToOffset(offset)
            }
        }
    }

    internal fun handleDrag(delta: Float): Boolean {
        if (!draggable) return false
        if (_animJob?.isActive == true) return false
        if (_checkedOffset == _uncheckedOffset) return false

        val oldOffset = _internalOffset
        _internalOffset += delta
        return _internalOffset != oldOffset
    }

    internal fun handleFling(velocity: Float) {
        if (!draggable) return
        if (_animJob?.isActive == true) return
        if (_checkedOffset == _uncheckedOffset) return

        val offset = if (velocity.absoluteValue > 1000f) {
            if (velocity > 0) _checkedOffset else _uncheckedOffset
        } else {
            boundsValue(_internalOffset, _uncheckedOffset, _checkedOffset)
        }

        animateToOffset(
            offset = offset,
            initialVelocity = velocity,
        ) {
            if (_checkedOffset != _uncheckedOffset) {
                val checked = _internalOffset == _checkedOffset
                if (checked != _isChecked) {
                    notifyCallback(checked)
                    delay(500)
                }
            }
        }
    }

    internal fun handleClick() {
        if (_animJob?.isActive == true) return
        notifyCallback(!_isChecked)
    }

    private fun animateToOffset(
        offset: Float,
        initialVelocity: Float? = null,
        onFinish: (suspend () -> Unit)? = null,
    ) {
        _animJob?.cancel()
        _scope.launch {
            _animOffset.snapTo(_internalOffset)
            _animOffset.animateTo(
                targetValue = offset,
                animationSpec = tween(durationMillis = 150),
                initialVelocity = initialVelocity ?: _animOffset.velocity,
            ) {
                _internalOffset = value
            }
            onFinish?.invoke()
            updateOffsetByStateStatic()
        }.also {
            _animJob = it
        }
    }

    private fun updateOffsetByStateStatic() {
        _internalOffset = boundsOffset(_isChecked)
    }

    private fun boundsOffset(isChecked: Boolean): Float {
        return if (isChecked) _checkedOffset else _uncheckedOffset
    }

    private fun notifyCallback(isChecked: Boolean) {
        onCheckedChange(isChecked)
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