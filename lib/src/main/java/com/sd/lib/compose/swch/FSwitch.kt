package com.sd.lib.compose.swch

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
    val enabledUpdated by rememberUpdatedState(enabled)
    val onCheckedChangeUpdated by rememberUpdatedState(onCheckedChange)

    val scope = rememberCoroutineScope()

    var isChecked by remember { mutableStateOf(checked) }
    LaunchedEffect(checked) { isChecked = checked }

    var totalWidth by remember { mutableStateOf(0f) }
    var thumbWidth by remember { mutableStateOf(0f) }
    val isReady by remember { derivedStateOf { totalWidth > 0 && thumbWidth > 0 } }

    val uncheckedOffset by remember { mutableStateOf(0f) }
    val checkedOffset by remember {
        derivedStateOf {
            val delta = (totalWidth - thumbWidth).coerceAtLeast(0f)
            uncheckedOffset + delta
        }
    }

    fun boundsOffset(isChecked: Boolean): Float {
        return if (isChecked) checkedOffset else uncheckedOffset
    }

    var currentOffset by remember { mutableStateOf(boundsOffset(isChecked)) }
    val animatable = remember { Animatable(boundsOffset(isChecked)) }

    var hasMove by remember { mutableStateOf(false) }

    val progress by remember {
        derivedStateOf {
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
    }

    fun notifyCallback() {
        when (currentOffset) {
            uncheckedOffset -> {
                if (isChecked) {
                    isChecked = false
                    onCheckedChangeUpdated(false)
                }
            }
            checkedOffset -> {
                if (!isChecked) {
                    isChecked = true
                    onCheckedChangeUpdated(true)
                }
            }
        }
    }

    fun animateToOffset(offset: Float, initialVelocity: Float? = null) {
        if (currentOffset == offset) {
            notifyCallback()
            return
        }

        scope.launch {
            animatable.snapTo(currentOffset)
            animatable.animateTo(
                targetValue = offset,
                initialVelocity = initialVelocity ?: animatable.velocity,
            ) { currentOffset = value.coerceIn(uncheckedOffset, checkedOffset) }
            notifyCallback()
        }
    }

    fun handleClick() {
        if (!animatable.isRunning) {
            val offset = boundsOffset(!isChecked)
            animateToOffset(offset)
        }
    }

    LaunchedEffect(isReady, isChecked, uncheckedOffset, checkedOffset) {
        if (isReady && !animatable.isRunning) {
            currentOffset = boundsOffset(isChecked)
        }
    }

    Box(modifier = modifier
        .width(50.dp)
        .height(25.dp)
        .onPlaced {
            totalWidth = it.size.width.toFloat()
        }
        .run {
            if (isReady && enabledUpdated) {
                fPointerChange(
                    onStart = {
                        enableVelocity = true
                        hasMove = false
                    },
                    onMove = {
                        if (it.id == currentEvent?.changes?.first()?.id) {
                            val change = it.positionChange()
                            it.consume()
                            hasMove = true
                            val offset = currentOffset + change.x
                            currentOffset = offset.coerceIn(uncheckedOffset, checkedOffset)
                        }
                    },
                    onUp = {
                        if (pointerCount == 1) {
                            if (hasMove) {
                                val velocity = getPointerVelocity(it.id).x
                                val offset = if (velocity.absoluteValue > 200f) {
                                    if (velocity > 0) checkedOffset else uncheckedOffset
                                } else {
                                    boundsValue(currentOffset, uncheckedOffset, checkedOffset)
                                }
                                animateToOffset(offset, velocity)
                            } else {
                                if (maxPointerCount == 1
                                    && !it.isConsumed
                                    && (it.uptimeMillis - it.previousUptimeMillis) < 180
                                ) {
                                    it.consume()
                                    handleClick()
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
        background(progress)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .onPlaced {
                    thumbWidth = it.size.width.toFloat()
                }
                .offset {
                    IntOffset(currentOffset.roundToInt(), 0)
                },
            contentAlignment = Alignment.Center,
        ) {
            thumb(progress)
        }
    }
}

private fun boundsValue(value: Float, minBounds: Float, maxBounds: Float): Float {
    require(value in minBounds..maxBounds)
    val center = (maxBounds - minBounds) / 2
    return if (value > center) maxBounds else minBounds
}