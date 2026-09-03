package com.qreader.reader.ui.compose.glass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 玻璃态确认弹框统一视觉令牌。
 *
 * 集中管理所有玻璃弹框（「主题模式」弹框、发现页删除框、书籍信息页删除框等）
 * 共享的配色与玻璃效果参数，避免在各处重复硬编码同一套数值。
 *
 * 设计约定（与用户确认的玻璃弹框风格一致）：
 *  - 弹框固定使用**深色主题**风格（白字、深灰半透明卡片、深灰蒙板），不随系统浅色模式变化；
 *  - E-Ink（墨水屏）模式下回退为黑白高对比，由 [glassDialogColors] 按 `isEInkMode` 解析。
 *
 * 玻璃效果数值含义：
 *  - brightness/saturation：colorControls 的亮度/饱和度；
 *  - blur/lens：drawBackdrop 的模糊半径与透镜折射尺寸（单位为 Dp，调用处需 `.toPx()`）。
 */
object GlassDialogTokens {

    // ── 深色固定配色（非 E-Ink 路径）──
    val contentColor = Color.White
    val accentColor = Color(0xFF0091FF)
    val containerColor = Color(0xFF121212).copy(alpha = 0.4f)
    val dimColor = Color(0xFF121212).copy(alpha = 0.56f)
    val scrimColor = Color.Black.copy(alpha = 0.5f)

    // ── 蒙板（scrim）玻璃效果 ──
    const val scrimBrightness = 0f
    const val scrimSaturation = 1.4f
    val scrimBlur = 12.dp
    val scrimLensX = 28.dp
    val scrimLensY = 56.dp

    // ── 卡片（card）玻璃效果 ──
    const val cardBrightness = 0f
    const val cardSaturation = 1.5f
    val cardBlur = 8.dp
    val cardLensX = 24.dp
    val cardLensY = 48.dp

    // ── 小组件（单选圈 / 复选框）玻璃效果 ──
    val widgetBlur = 8.dp
    val widgetLensX = 16.dp
    val widgetLensY = 24.dp
}

/**
 * 玻璃弹框配色集（按是否墨水屏解析）。
 *
 * 非 E-Ink：深色固定风格（[GlassDialogTokens] 默认值）。
 * E-Ink：黑白高对比，蒙板与 scrim 透明（不压暗，靠纯色卡片区分层级）。
 */
data class GlassDialogColors(
    val contentColor: Color,
    val accentColor: Color,
    val containerColor: Color,
    val dimColor: Color,
    val scrimColor: Color,
)

fun glassDialogColors(isEInkMode: Boolean): GlassDialogColors = if (isEInkMode) {
    GlassDialogColors(
        contentColor = Color.Black,
        accentColor = GlassDialogTokens.accentColor,
        containerColor = Color.White,
        dimColor = Color.Transparent,
        scrimColor = Color.Transparent,
    )
} else {
    GlassDialogColors(
        contentColor = GlassDialogTokens.contentColor,
        accentColor = GlassDialogTokens.accentColor,
        containerColor = GlassDialogTokens.containerColor,
        dimColor = GlassDialogTokens.dimColor,
        scrimColor = GlassDialogTokens.scrimColor,
    )
}
