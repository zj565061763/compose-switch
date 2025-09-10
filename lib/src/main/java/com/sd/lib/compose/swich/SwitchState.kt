package com.sd.lib.compose.swich

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay

@Composable
fun rememberSwitchState(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
): FSwitchState {
  return remember { FSwitchState() }.apply {
    this.Checked(checked)
    this.onCheckedChange = onCheckedChange
  }
}

class FSwitchState internal constructor() {
  /** 当前进度[0-1] */
  var progress: Float by mutableFloatStateOf(0f)
    private set

  internal var hasInitialized: Boolean by mutableStateOf(false)
    private set

  internal var thumbOffset: Int by mutableIntStateOf(0)
    private set

  internal val checked: Boolean get() = _checked

  private lateinit var _coroutineScope: CoroutineScope
  internal lateinit var onCheckedChange: (Boolean) -> Unit

  private var _checked: Boolean by mutableStateOf(false)
  private val _uncheckedOffset: Float = 0f
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

  @Composable
  internal fun Checked(checked: Boolean) {
    _coroutineScope = rememberCoroutineScope()
    _checked = checked
    LaunchedEffect(checked, _uncheckedOffset, _checkedOffset) {
      if (!hasInitialized) {
        if (_uncheckedOffset != _checkedOffset) {
          updateOffsetByState()
          hasInitialized = true
        }
      } else {
        val offset = boundsOffset(checked)
        animateToOffset(offset)
      }
    }
  }

  internal fun updateStateWhenInspectionMode() {
    updateOffsetByState()
    hasInitialized = true
  }

  internal fun setSize(boxSize: Float, thumbSize: Float) {
    _checkedOffset = _uncheckedOffset + (boxSize - thumbSize).coerceAtLeast(0f)
    if (_checkedOffset == _uncheckedOffset) reset()
  }

  internal fun handleDrag(delta: Float): Boolean {
    if (_animJob?.isActive == true) return false
    if (_checkedOffset == _uncheckedOffset) return false

    val oldOffset = _internalOffset
    _internalOffset += delta
    return _internalOffset != oldOffset
  }

  internal suspend fun handleFling(velocity: Float) {
    if (_animJob?.isActive == true) return
    if (_checkedOffset == _uncheckedOffset) return

    val decayOffset = exponentialDecay<Float>().calculateTargetValue(
      initialValue = _internalOffset,
      initialVelocity = velocity,
    )

    animateToOffset(
      offset = boundsValue(decayOffset, _uncheckedOffset, _checkedOffset),
      initialVelocity = velocity,
    ) {
      if (_checkedOffset != _uncheckedOffset) {
        val checked = _internalOffset == _checkedOffset
        if (checked != _checked) {
          onCheckedChange(checked)
          delay(500)
        }
      }
    }
  }

  internal fun handleDragCancel() {
    reset()
  }

  internal fun handleClick() {
    if (_animJob?.isActive == true) return
    onCheckedChange(!_checked)
  }

  private suspend fun animateToOffset(
    offset: Float,
    initialVelocity: Float? = null,
    onFinish: (suspend () -> Unit)? = null,
  ) {
    _animJob?.cancelAndJoin()
    _animJob = currentCoroutineContext()[Job]

    _animOffset.snapTo(_internalOffset)
    _animOffset.animateTo(
      targetValue = offset,
      animationSpec = tween(durationMillis = 150),
      initialVelocity = initialVelocity ?: _animOffset.velocity,
      block = { _internalOffset = value },
    )

    onFinish?.invoke()
    updateOffsetByState()
  }

  private fun reset() {
    _animJob?.cancel()
    updateOffsetByState()
  }

  private fun updateOffsetByState() {
    _internalOffset = boundsOffset(_checked)
  }

  private fun boundsOffset(checked: Boolean): Float {
    return if (checked) _checkedOffset else _uncheckedOffset
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
}

private fun boundsValue(value: Float, min: Float, max: Float): Float {
  if (value <= min) return min
  if (value >= max) return max
  val center = (min + max) / 2
  return if (value > center) max else min
}