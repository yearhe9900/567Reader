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

    // ── 深色固定配色（非 E-Ink 且暗色主题路径）──
    val contentColor = Color.White
    val accentColor = Color(0xFF0091FF)
    val containerColor = Color(0xFF121212).copy(alpha = 0.4f)
    val dimColor = Color(0xFF121212).copy(alpha = 0.56f)
    val scrimColor = Color.Black.copy(alpha = 0.5f)

    // ── 亮色配色（非 E-Ink 且亮色主题路径）──
    // 暖白卡黑字：与 bar 类玻璃一致用 #FFFDF8（ARGB 0xFFFFFDF8）@0.9f；蒙板用白雾 + 极淡暗压，
    // 让白色卡片在浅背景上依然能突出，又不像暗色蒙板那样压黑。
    val lightContentColor = Color.Black
    val lightContainerColor = Color(0xFFFFFDF8).copy(alpha = 0.9f)
    val lightDimColor = Color(0xFF202020).copy(alpha = 0.10f)
    val lightScrimColor = Color.White.copy(alpha = 0.55f)

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
 * 玻璃弹框配色集（按是否墨水屏 / 明暗主题解析）。
 *
 * - E-Ink：黑白高对比，蒙板与 scrim 透明（不压暗，靠纯色卡片区分层级）；
 * - 非 E-Ink + 亮色主题：白卡黑字（[GlassDialogTokens] 亮色常量）；
 * - 非 E-Ink + 暗色主题：深灰卡白字固定风格（[GlassDialogTokens] 默认值）。
 *
 * 明暗以「背景色」为准（与 MainScreen / SearchScreen 一致），而非主色——
 * legado 主题主色通常是深色强调色，按主色判断会把浅背景误判为暗色。
 */
data class GlassDialogColors(
    val contentColor: Color,
    val accentColor: Color,
    val containerColor: Color,
    val dimColor: Color,
    val scrimColor: Color,
)

fun glassDialogColors(isEInkMode: Boolean, isLightTheme: Boolean): GlassDialogColors = when {
    isEInkMode -> GlassDialogColors(
        contentColor = Color.Black,
        accentColor = GlassDialogTokens.accentColor,
        containerColor = Color.White,
        dimColor = Color.Transparent,
        scrimColor = Color.Transparent,
    )

    isLightTheme -> GlassDialogColors(
        contentColor = GlassDialogTokens.lightContentColor,
        accentColor = GlassDialogTokens.accentColor,
        containerColor = GlassDialogTokens.lightContainerColor,
        dimColor = GlassDialogTokens.lightDimColor,
        scrimColor = GlassDialogTokens.lightScrimColor,
    )

    else -> GlassDialogColors(
        contentColor = GlassDialogTokens.contentColor,
        accentColor = GlassDialogTokens.accentColor,
        containerColor = GlassDialogTokens.containerColor,
        dimColor = GlassDialogTokens.dimColor,
        scrimColor = GlassDialogTokens.scrimColor,
    )
}
