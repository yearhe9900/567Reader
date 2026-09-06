package com.qreader.reader.ui.compose.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Clock

class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
    /**
     * 最短按压保持时长（ms）。
     * 放大（scaleX/scaleY）走的是慢弹簧（刚度 250，约 400ms 才稳定），
     * 快速点击（~100ms）松手后 release 立刻反向，放大动画来不及跑出来 → 点击看不到滑块放大。
     * 设 >0 时，release 会等够这个时长再反向，让点击也能完整跑出放大+玻璃态；拖动不受影响。
     * 默认 0（保持官方 demo 行为）。
     */
    val minHoldDuration: Long = 0L,
) {

    private val valueAnimationSpec =
        spring(1f, 1000f, visibilityThreshold)
    private val velocityAnimationSpec =
        spring(0.5f, 300f, visibilityThreshold * 10f)
    private val pressProgressAnimationSpec =
        spring(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec =
        spring(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec =
        spring(0.7f, 250f, 0.001f)

    private val valueAnimation =
        Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation =
        Animatable(0f, 5f)
    private val pressProgressAnimation =
        Animatable(0f, 0.001f)
    private val scaleXAnimation =
        Animatable(initialScale, 0.001f)
    private val scaleYAnimation =
        Animatable(initialScale, 0.001f)

    private val mutatorMutex = MutatorMutex()

    private val velocityTracker = VelocityTracker()

    /** press() 触发时刻，用于 release 计算已按压时长，决定是否需要补齐 minHoldDuration */
    private var pressStartTime = 0L

    val value: Float get() = valueAnimation.value
    val progress: Float get() = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float get() = velocityAnimation.value

    /**
     * 滑块位移动画或惯性动画是否正在运行。
     *
     * 动画期间 [value] 逐帧变化，会导致导航栏的三层玻璃逐帧重绘。
     * 宿主据此在动画期间临时关闭 blur/lens/vibrancy 以保帧率。
     * 直接读取 Animatable.isRunning（本身由 snapshot state 支持），无需额外维护标志位。
     */
    val isAnimating: Boolean
        get() = valueAnimation.isRunning || velocityAnimation.isRunning

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        inspectDragGestures(
            onDragStart = { down ->
                onDragStarted(down.position)
                press()
            },
            onDragEnd = {
                onDragStopped()
                release()
            },
            onDragCancel = {
                onDragStopped()
                release()
            }
        ) { change ->
            val dragAmount = change.positionChange()
            onDrag(size, dragAmount)
        }
    }

    private suspend fun awaitFrame() {
        yield()
    }

    fun press() {
        velocityTracker.resetTracking()
        pressStartTime = Clock.System.now().toEpochMilliseconds()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
        }
    }

    fun release() {
        animationScope.launch {
            awaitFrame()
            // 点击场景中手指停留极短，放大动画来不及跑出；补齐最短按压时长再反向，
            // 让点击也能看到完整的滑块放大 + 玻璃态。拖动（停留久）不受此影响。
            if (minHoldDuration > 0L) {
                val heldMs = Clock.System.now().toEpochMilliseconds() - pressStartTime
                val remaining = minHoldDuration - heldMs
                if (remaining > 0L) delay(remaining)
            }
            if (value != targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                snapshotFlow { valueAnimation.value }
                    .filter { abs(it - valueAnimation.targetValue) < threshold }
                    .first()
            }
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
        }
    }

    fun updateValue(value: Float) {
        val targetValue = value.coerceIn(valueRange)
        animationScope.launch {
            launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) { updateVelocity() } }
        }
    }

    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val targetValue = value.coerceIn(valueRange)
                launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
                release()
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(
            Clock.System.now().toEpochMilliseconds(),
            Offset(value, 0f)
        )
        val targetVelocity = velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        animationScope.launch { velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec) }
    }
}
