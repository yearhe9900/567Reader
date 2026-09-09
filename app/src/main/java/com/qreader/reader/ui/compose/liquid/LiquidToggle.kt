package com.qreader.reader.ui.compose.liquid

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.qreader.reader.ui.compose.glass.GlassConfig
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

@Composable
fun LiquidToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isLightTheme: Boolean = !isSystemInDarkTheme(),
) {
    val accentColor = GlassConfig.toggleAccentColor(isLightTheme)
    val trackColor = GlassConfig.toggleTrackColor(isLightTheme)

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    // 滑块水平行程：轨道宽 - 滑块宽 - 两侧 2dp 边距（与 graphicsLayer 内 padding 一致）
    val dragWidth = with(density) {
        (GlassConfig.toggleWidth - GlassConfig.toggleThumbWidth - 4.dp).toPx().coerceAtLeast(1f)
    }
    val animationScope = rememberCoroutineScope()

    // 用 rememberUpdatedState 持有最新的 selected/onSelect 引用，
    // 避免 remember(animationScope) 的 DampedDragAnimation 捕获过期闭包。
    val latestSelected = rememberUpdatedState(selected)
    val latestOnSelect = rememberUpdatedState(onSelect)
    val touchSlop = LocalViewConfiguration.current.touchSlop

    var fraction by remember { mutableFloatStateOf(if (selected()) 1f else 0f) }
    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = GlassConfig.togglePressedScale,
            onDragStarted = {},
            onDragStopped = {},
            onDrag = { _, _ -> },
            minHoldDuration = GlassConfig.toggleMinHoldDurationMs
        )
    }
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { fraction }
            .collectLatest { fraction ->
                dampedDragAnimation.updateValue(fraction)
            }
    }
    LaunchedEffect(selected) {
        snapshotFlow { selected() }
            .collectLatest { isSelected ->
                val target = if (isSelected) 1f else 0f
                if (target != fraction) {
                    fraction = target
                    dampedDragAnimation.animateToValue(target)
                }
            }
    }

    val trackBackdrop = rememberLayerBackdrop()

    Box(
        // 外围留白：按压缩放与 Shadow/Highlight 会画到轨道外，
        // 布局尺寸必须包含这部分，否则 ComposeView/父 FrameLayout 会裁切特效
        modifier.padding(GlassConfig.toggleEffectPadding),
        contentAlignment = Alignment.Center
    ) {
        // 轨道盒：固定 toggle 尺寸；滑块必须相对轨道左缘定位（CenterStart），
        // 若直接把滑块放在外层 Center 对齐的 Box 里，false 态会落在灰条中间。
        Box(
            Modifier.size(GlassConfig.toggleWidth, GlassConfig.toggleHeight),
            contentAlignment = Alignment.CenterStart,
        ) {
            // 整条轨道可点可拖：命中区 56×24，不依赖小滑块；
            // 位移 ≤ touchSlop 视为点击取反；拖拽结束吸附。不挂 DampedDragAnimation.modifier，
            // 避免父级消费事件时 inspectDragGestures 走 cancel 并误切换。
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(animationScope, dragWidth, isLtr) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            dampedDragAnimation.press()
                            var totalDx = 0f
                            var current = dampedDragAnimation.value
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes
                                    .fastFirstOrNull { it.id == down.id }
                                    ?: break
                                if (change.changedToUpIgnoreConsumed()) {
                                    val next = if (abs(totalDx) <= touchSlop) {
                                        if (latestSelected.value()) 0f else 1f
                                    } else {
                                        if (current >= 0.5f) 1f else 0f
                                    }
                                    fraction = next
                                    latestOnSelect.value(next == 1f)
                                    break
                                }
                                if (change.isConsumed) break
                                val dx = change.positionChange().x
                                totalDx += dx
                                if (abs(totalDx) > touchSlop) {
                                    change.consume()
                                    val delta = dx / dragWidth
                                    current = (if (isLtr) current + delta else current - delta)
                                        .fastCoerceIn(0f, 1f)
                                    fraction = current
                                }
                            }
                            dampedDragAnimation.release()
                        }
                    }
                    .layerBackdrop(trackBackdrop)
                    .clip(Capsule())
                    .drawBehind {
                        val f = dampedDragAnimation.value
                        drawRect(lerp(trackColor, accentColor, f))
                    }
            )

            Box(
                Modifier
                    .graphicsLayer {
                        val f = dampedDragAnimation.value
                        val padding = 2f.dp.toPx()
                        translationX =
                            if (isLtr) lerp(padding, padding + dragWidth, f)
                            else lerp(-padding, -(padding + dragWidth), f)
                    }
                    .semantics {
                        role = Role.Switch
                    }
                    .drawBackdrop(
                        backdrop = rememberCombinedBackdrop(
                            backdrop,
                            rememberBackdrop(trackBackdrop) { drawBackdrop ->
                                val progress = dampedDragAnimation.pressProgress
                                val scaleX = lerp(2f / 3f, 0.75f, progress)
                                val scaleY = lerp(0f, 0.75f, progress)
                                scale(scaleX, scaleY) {
                                    drawBackdrop()
                                }
                            }
                        ),
                        shape = { Capsule() },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            blur(8f.dp.toPx() * (1f - progress))
                            lens(
                                5f.dp.toPx() * progress,
                                10f.dp.toPx() * progress,
                                chromaticAberration = true
                            )
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Ambient.copy(
                                width = Highlight.Ambient.width / 1.5f,
                                blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                                alpha = progress
                            )
                        },
                        shadow = {
                            Shadow(
                                radius = 4f.dp,
                                color = Color.Black.copy(alpha = 0.05f)
                            )
                        },
                        innerShadow = {
                            val progress = dampedDragAnimation.pressProgress
                            InnerShadow(
                                radius = 4f.dp * progress,
                                alpha = progress
                            )
                        },
                        layerBlock = {
                            scaleX = dampedDragAnimation.scaleX
                            scaleY = dampedDragAnimation.scaleY
                            val velocity = dampedDragAnimation.velocity / 50f
                            scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                            scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                        },
                        // 官方 demo 的背景是丰富壁纸，surface 不透明白色即可借高光/阴影呈现玻璃感。
                        // 本页背景为柔和渐变，若 rest 态 alpha=1 会完全盖住 blur/lens 采样结果，
                        // 导致点击/静止时看不到玻璃折射。降低静止透明度，让 backdrop blur 始终可见。
                        onDrawSurface = {
                            val progress = dampedDragAnimation.pressProgress
                            drawRect(Color.White.copy(alpha = 0.6f - progress * 0.25f))
                        }
                    )
                    .size(GlassConfig.toggleThumbWidth, GlassConfig.toggleThumbHeight)
            )
        }
    }
}
