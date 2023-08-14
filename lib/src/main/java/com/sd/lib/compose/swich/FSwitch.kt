package com.sd.lib.compose.swich

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
    onCheckedChange: (Boolean) -> Unit,
) {
    Switch(
        checked = checked,
        isHorizontal = true,
        interactiveMode = interactiveMode,
        enabled = enabled,
        modifier = modifier,
        background = background,
        thumb = thumb,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
private fun Switch(
    checked: Boolean,
    isHorizontal: Boolean,
    interactiveMode: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    background: @Composable (progress: Float) -> Unit,
    thumb: @Composable (progress: Float) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var thumbSize by remember { mutableStateOf(IntSize.Zero) }

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val state = remember { SwitchState(coroutineScope) }.also {
        it.onCheckedChange = onCheckedChange
        it.interactiveMode = interactiveMode
        if (boxSize.width > 0 && boxSize.height > 0 &&
            thumbSize.width > 0 && thumbSize.height > 0
        ) {
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

    Box(modifier = modifier.let {
        if (isHorizontal) {
            it.defaultMinSize(minWidth = 50.dp, minHeight = 25.dp)
        } else {
            it.defaultMinSize(minWidth = 25.dp, minHeight = 50.dp)
        }
    }.onSizeChanged {
        boxSize = it
    }.let {
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
                        IntOffset(state.currentOffset.roundToInt(), 0)
                    } else {
                        IntOffset(0, state.currentOffset.roundToInt())
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            thumb(state.progress)
        }
    }
}

private class SwitchState(scope: CoroutineScope) {
    private val _scope = scope

    lateinit var onCheckedChange: (Boolean) -> Unit
    var interactiveMode = false

    var boxSize: Float by mutableStateOf(0f)
    var thumbSize: Float by mutableStateOf(0f)
    val isReady: Boolean by derivedStateOf { boxSize > 0 && thumbSize > 0 }

    private val _uncheckedOffset: Float by mutableStateOf(0f)
    private val _checkedOffset: Float by derivedStateOf {
        val delta = (boxSize - thumbSize).coerceAtLeast(0f)
        _uncheckedOffset + delta
    }

    private var _isChecked: Boolean by mutableStateOf(false)
    var hasInitialized: Boolean by mutableStateOf(false)
        private set

    private val _animOffset = Animatable(0f)
    private var _animJob: Job? = null

    var progress: Float by mutableStateOf(0f)
        private set

    var currentOffset: Float by mutableStateOf(0f)
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