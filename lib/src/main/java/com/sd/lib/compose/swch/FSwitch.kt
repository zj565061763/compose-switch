package com.sd.lib.compose.swch

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
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
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    background: @Composable (progress: Float) -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50))
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(it)
                    .background(MaterialTheme.colors.primary, RoundedCornerShape(50))
            )
        }
    },
    thumb: @Composable (progress: Float) -> Unit = {
        Box(
            modifier = Modifier
                .aspectRatio(1f, true)
                .padding(2.dp)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(50),
                )
        )
    },
) {
    var isChecked by remember(checked) { mutableStateOf(checked) }
    val scope = rememberCoroutineScope()

    val onCheckedChangeUpdated by rememberUpdatedState(onCheckedChange)
    val enabledUpdated by rememberUpdatedState(enabled)

    var totalWidth by remember { mutableStateOf(0f) }
    var thumbWidth by remember { mutableStateOf(0f) }
    val isReady by remember { derivedStateOf { totalWidth > 0 && thumbWidth > 0 } }

    val uncheckedOffset by remember { mutableStateOf(0f) }
    val checkedOffset by remember { derivedStateOf { (totalWidth - thumbWidth).coerceAtLeast(0f) } }
    val boundsOffset by remember { derivedStateOf { if (isChecked) checkedOffset else uncheckedOffset } }

    var currentOffset by remember { mutableStateOf(boundsOffset) }
    val animatable = remember { Animatable(boundsOffset) }

    var isTouch by remember { mutableStateOf(false) }

    val progress by remember {
        derivedStateOf {
            if (checkedOffset > 0f) {
                (currentOffset / checkedOffset).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    fun animateToOffset(offset: Float, initialVelocity: Float? = null) {
        if (currentOffset == offset) return
        scope.launch {
            animatable.snapTo(currentOffset)
            animatable.animateTo(
                targetValue = offset,
                initialVelocity = initialVelocity ?: animatable.velocity,
            ) { currentOffset = value }
        }
    }

    LaunchedEffect(isReady, isChecked) {
        if (isReady && !animatable.isRunning && !isTouch) {
            currentOffset = boundsOffset
        }
    }

    LaunchedEffect(animatable) {
        var lastState = animatable.isRunning
        snapshotFlow { animatable.isRunning }.collect { isRunning ->
            if (lastState && !isRunning && !isTouch) {
                when (animatable.value) {
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
            lastState = isRunning
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
                clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    if (!animatable.isRunning) {
                        val offset = if (isChecked) uncheckedOffset else checkedOffset
                        animateToOffset(offset)
                    }
                }.fPointerChange(
                    onStart = {
                        enableVelocity = true
                        isTouch = true
                    },
                    onMove = {
                        if (it.id == currentEvent?.changes?.first()?.id) {
                            val change = it.positionChange()
                            val offset = (currentOffset + change.x).coerceIn(uncheckedOffset, checkedOffset)
                            currentOffset = offset
                        }
                    },
                    onUp = {
                        if (pointerCount == 1) {
                            val velocity = getPointerVelocity(it.id).x
                            val offset = if (velocity.absoluteValue > 200f) {
                                if (velocity > 0) checkedOffset else uncheckedOffset
                            } else {
                                boundsValue(currentOffset, uncheckedOffset, checkedOffset)
                            }
                            animateToOffset(offset, velocity)
                        }
                    },
                    onFinish = {
                        isTouch = false
                    }
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