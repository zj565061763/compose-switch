package com.sd.lib.compose.swich

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

class FSwitchState(scope: CoroutineScope) {
    private val _scope = scope

    lateinit var onCheckedChange: (Boolean) -> Unit
    var interactiveMode = false

    var boxSize: Float by mutableFloatStateOf(0f)
    var thumbSize: Float by mutableFloatStateOf(0f)
    val isReady: Boolean by derivedStateOf { boxSize > 0 && thumbSize > 0 }

    private val _uncheckedOffset: Float by mutableFloatStateOf(0f)
    private val _checkedOffset: Float by derivedStateOf {
        val delta = (boxSize - thumbSize).coerceAtLeast(0f)
        _uncheckedOffset + delta
    }

    private var _isChecked: Boolean by mutableStateOf(false)
    var hasInitialized: Boolean by mutableStateOf(false)
        private set

    private val _animOffset = Animatable(0f)
    private var _animJob: Job? = null

    var progress: Float by mutableFloatStateOf(0f)
        private set

    var currentOffset: Float by mutableFloatStateOf(0f)
        private set

    private var _internalOffset: Float = currentOffset
        set(value) {
            val newValue = value.coerceIn(_uncheckedOffset, _checkedOffset)
            if (field != newValue) {
                field = newValue
                currentOffset = newValue
                updateProgress()
            }
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
            if (!isReady) return@LaunchedEffect

            if (!hasInitialized) {
                updateOffsetByStateStatic()
                hasInitialized = true
                return@LaunchedEffect
            }

            val offset = boundsOffset(_isChecked)
            animateToOffset(offset)
        }
    }

    fun handleDrag(delta: Float): Boolean {
        if (!interactiveMode) return false
        if (_animJob?.isActive == true) return false
        if (_checkedOffset == _uncheckedOffset) return false

        val oldOffset = _internalOffset
        _internalOffset += delta
        return _internalOffset != oldOffset
    }

    fun handleFling(velocity: Float) {
        if (_animJob?.isActive == true) return
        if (_checkedOffset == _uncheckedOffset) return

        val offset = if (velocity.absoluteValue > 1000f) {
            if (velocity > 0) _checkedOffset else _uncheckedOffset
        } else {
            boundsValue(_internalOffset, _uncheckedOffset, _checkedOffset)
        }
        animateToOffsetInteractive(offset, velocity)
    }

    fun handleClick() {
        if (_animJob?.isActive == true) return
        if (_checkedOffset == _uncheckedOffset) {
            onCheckedChange(!_isChecked)
            return
        }

        if (interactiveMode) {
            val offset = boundsOffset(!_isChecked)
            animateToOffsetInteractive(offset)
        } else {
            onCheckedChange(!_isChecked)
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
            if (_checkedOffset != _uncheckedOffset) {
                val checked = _internalOffset == _checkedOffset
                if (checked != _isChecked) {
                    onCheckedChange(checked)
                    delay(500)
                }
            }
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
}

private fun boundsValue(value: Float, min: Float, max: Float): Float {
    require(value in min..max)
    val center = (min + max) / 2
    return if (value > center) max else min
}

internal inline fun logMsg(block: () -> String) {
    Log.i("FSwitch", block())
}