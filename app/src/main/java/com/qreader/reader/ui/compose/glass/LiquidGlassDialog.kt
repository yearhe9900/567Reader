package com.qreader.reader.ui.compose.glass

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.RoundedRectangle
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.lib.theme.backgroundColor
import com.qreader.reader.utils.ColorUtils

/**
 * 通用 Compose 液态玻璃弹框（kyant drawBackdrop 真·毛玻璃）。
 *
 * 统一「主题模式」弹框 / 删除书籍框 / 编辑分组框 三处重复的「双 drawBackdrop」玻璃结构：
 *  1) 全屏玻璃蒙板：模糊真实背景([backdrop]) + 染色 + dim 压暗；
 *  2) 居中玻璃卡片：模糊背景 + 圆角容器，content 为卡片内容（标题/正文/列表/按钮由调用方填充）。
 *
 * 配色与玻璃效果统一走 [GlassDialogTokens] / [glassDialogColors]，E-Ink 自动回退黑白高对比。
 * 背景采样源 [backdrop] 由调用方传入同一 Backdrop 实例（如 MainScreen 的 layerBackdrop），
 * 因此卡片采样的是其下方真实页面，而非弹框自身。
 *
 * 系统返回键 / 侧滑返回手势由本组件内部接管并回调 [onDismiss]（受 [dismissOnScrimClick] 控制）。
 * 这一点很关键：本弹框是 in-tree overlay，不是 Dialog，返回事件不会自动被它拦下；
 * 若不接管，事件会穿透到 Activity 的返回处理，造成「弹框还在、宿主页面却被换掉」的状态错乱。
 *
 * 注意：本组件根节点为 [Box]([Modifier.fillMaxSize])，但蒙板是否全屏取决于「父容器」是否撑满全屏。
 * 典型坑：不要把本弹框的 [androidx.compose.animation.AnimatedVisibility] 嵌套进某个
 * 使用 align/fillMaxWidth、自身只有内容高的 Box（例如底部导航栏 Box）里——那样 fillMaxSize 至多只填满该小 Box，
 * 蒙板只会盖住一小块区域。应让该 AnimatedVisibility 直接作为全屏外层容器的子节点（与 backdrop 捕获 Box 平级）。
 *
 * @param backdrop             玻璃模糊源（调用方传入的 Backdrop 实例）
 * @param onDismiss           关闭回调（点蒙板区域触发，受 [dismissOnScrimClick] 控制）
 * @param modifier            应用于卡片 Column 的修饰符（建议在此设置宽度，如 fillMaxWidth(0.9f)、
 *                            wrapContentHeight、verticalScroll 等；不要在此加 padding，请用 [contentPadding]）
 * @param cardRadius          卡片圆角（Dp），默认 28.dp
 * @param contentPadding      卡片内边距（绘制在玻璃表面之内），默认 0.dp（调用方可在内容里自行留白）
 * @param dismissOnScrimClick 点蒙板是否关闭，默认 true
 * @param content             卡片内容 lambda，接收解析后的 [GlassDialogColors] 供调用方取色
 */
@Composable
fun LiquidGlassDialog(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    cardRadius: Dp = 28.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    dismissOnScrimClick: Boolean = true,
    content: @Composable ColumnScope.(colors: GlassDialogColors) -> Unit
) {
    val isEInkMode = AppConfig.isEInkMode
    val isLightTheme = ColorUtils.isColorLight(LocalContext.current.backgroundColor)
    val colors = glassDialogColors(isEInkMode, isLightTheme)
    val containerColor = colors.containerColor
    val dimColor = colors.dimColor
    val scrimColor = colors.scrimColor

    // 接管系统返回键 / 侧滑返回手势：先关闭弹框，避免事件穿透到 Activity。
    //
    // 坑（已踩）：本弹框是 in-tree overlay 而非 Dialog，返回事件不会自动被它吃掉。
    // 「主题模式」弹框场景下，穿透后 MainActivity 的返回处理会把 tab 切回书架，
    // 设置页被 HorizontalPager 销毁 → 弹框消失，但宿主持有的 themeDialogOpen 仍是 true
    // → 标题栏/导航栏的 AnimatedVisibility(!themeDialogOpen) 永久隐藏。
    //
    // LocalOnBackPressedDispatcherOwner 可能为空（例如宿主是 DialogFragment 内的 ComposeView，
    // 该窗口的 decorView 上没有挂 owner），此时不注册，交由外层窗口自行处理，避免 BackHandler 抛异常。
    if (dismissOnScrimClick && LocalOnBackPressedDispatcherOwner.current != null) {
        BackHandler { onDismiss() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1) 全屏玻璃蒙板
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(0.5f.dp) },
                    effects = {
                        if (!isEInkMode) {
                            colorControls(
                                brightness = GlassDialogTokens.scrimBrightness,
                                saturation = GlassDialogTokens.scrimSaturation
                            )
                            blur(GlassDialogTokens.scrimBlur.toPx())
                            lens(
                                GlassDialogTokens.scrimLensX.toPx(),
                                GlassDialogTokens.scrimLensY.toPx(),
                                depthEffect = true
                            )
                        }
                    },
                    highlight = { Highlight.Plain },
                    onDrawSurface = { drawRect(scrimColor) }
                )
                .drawWithContent {
                    drawContent()
                    drawRect(dimColor)
                }
        )

        // 2) 居中玻璃卡片层（点外部区域 = 取消）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (dismissOnScrimClick) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onDismiss() }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* 点卡片内部不关闭 */ }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(cardRadius) },
                        effects = {
                            if (!isEInkMode) {
                                colorControls(
                                    brightness = GlassDialogTokens.cardBrightness,
                                    saturation = GlassDialogTokens.cardSaturation
                                )
                                blur(GlassDialogTokens.cardBlur.toPx())
                                lens(
                                    GlassDialogTokens.cardLensX.toPx(),
                                    GlassDialogTokens.cardLensY.toPx(),
                                    depthEffect = true
                                )
                            }
                        },
                        highlight = { Highlight.Plain },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .padding(contentPadding)
            ) {
                content(colors)
            }
        }
    }
}
