package com.qreader.reader.ui.compose.glass

import android.content.Context
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.core.view.WindowCompat
import com.qreader.reader.lib.theme.backgroundColor
import com.qreader.reader.utils.ColorUtils

/**
 * 全局玻璃态统一配置（单一真源）。
 *
 * 集中管理所有玻璃组件的：
 *  - 模糊半径与透镜折射参数（[blur] / [lensX] / [lensY]）；
 *  - 容器半透明底色（[lightContainerColor] / [darkContainerColor]）；
 *  - 玻璃上承载的文字 / 图标色（[lightContentColor] / [darkContentColor]）；
 *  - 明暗主题判定（[isLightTheme]）。
 *
 * 任何玻璃组件（bar 类 / 弹框类）都从这里取色与判定，不要在各处重复硬编码或重复计算，
 * 这样「改一处即可全局生效」。弹框类玻璃的「蒙板 / 卡片 / 小组件」等专有视觉令牌仍放在
 * 独立的 [GlassDialogTokens]，但其共用的容器色与内容色一律引用本对象的常量，避免二次定义。
 *
 * 玻璃效果数值含义：
 *  - blur：drawBackdrop 的模糊半径（dp）；
 *  - lensX / lensY：透镜折射尺寸（dp）。
 */
object GlassConfig {

    // ── bar 类玻璃效果（导航栏 / 标题栏 / 玻璃按钮 / 下拉菜单）──
    val blur = 12.dp
    val lensX = 24.dp
    val lensY = 48.dp
    val chromaticAberration = true

    // 容器半透明底色。亮色用暖白 #FFFDF8（ARGB 0xFFFFFDF8）@0.9f——0.4f 时 vibrancy/blur 结果占 60%，整体偏灰；
    // 深色保持 0.4f（深灰底 + 低透明度 = 恰好出暗玻璃感）。
    val lightContainerColor = Color(0xFFFFFDF8).copy(alpha = 0.9f)
    val darkContainerColor = Color(0xFF121212).copy(alpha = 0.4f)

    // 玻璃上承载的文字 / 图标色：亮色黑、暗色白（与背景反色）。
    val lightContentColor = Color.Black
    val darkContentColor = Color.White

    // ── 玻璃按钮 / 菜单项通用参数（标题栏图标按钮、下拉菜单项、搜索按钮等）──

    /** 玻璃按钮/菜单项的表面透明度（0.6 = 60% 不透明度，让 blur/lens 效果透出）。 */
    val glassButtonSurfaceAlpha = 0.6f

    /** 玻璃按钮/菜单项的圆角半径（图标按钮等小尺寸玻璃组件）。 */
    val glassButtonCornerRadius: Dp = 12.dp

    /** 玻璃标题栏高度（不含 statusBarsPadding，仅纯内容高度）。 */
    val titleBarHeight: Dp = 56.dp

    // ── 伪玻璃按钮参数（捕获层内部不能用 drawBackdrop 时的近似方案）──

    /** 伪玻璃按钮渐变高光颜色（白→透明，叠加在容器色上产生玻璃光泽）。 */
    val pseudoGlassHighlight = listOf(
        Color.White.copy(alpha = 0.16f),
        Color.White.copy(alpha = 0.04f),
    )

    /** 伪玻璃按钮边框颜色。 */
    val pseudoGlassBorderColor = Color.White.copy(alpha = 0.14f)

    /** 伪玻璃按钮边框宽度。 */
    val pseudoGlassBorderWidth: Dp = 1.dp

    /**
     * 伪玻璃修饰符：容器色背景 + 白色渐变高光 + 半透明边框。
     *
     * 适用于 [com.kyant.backdrop.backdrops.layerBackdrop] 捕获层内部的小组件（按钮/标签/菜单项），
     * 在不能用 [com.kyant.backdrop.drawBackdrop]（会循环捕获崩溃）时提供玻璃质感近似。
     * 搜索页「清除」按钮、编辑页封面操作按钮均使用此修饰符。
     */
    fun Modifier.pseudoGlass(
        isLightTheme: Boolean,
        shape: Shape = RoundedCornerShape(8.dp),
    ): Modifier = this
        .clip(shape)
        .background(containerColor(isLightTheme), shape)
        .background(Brush.verticalGradient(pseudoGlassHighlight), shape)
        .border(pseudoGlassBorderWidth, pseudoGlassBorderColor, shape)

    /**
     * 判定当前是否亮色主题。
     *
     * 必须以「背景色」为准，而非主色：legado 主题的主色通常是用户选的强调色（多为深色），
     * 按主色判断会把「浅背景 + 深色主色」误判为暗色，导致玻璃永远走深灰、文字永远白。
     * 所有玻璃组件统一调用本方法，避免各页面各自实现判定逻辑、将来再次分叉。
     */
    fun isLightTheme(context: Context): Boolean =
        ColorUtils.isColorLight(context.backgroundColor)

    /** 根据明暗主题返回玻璃容器背景色。 */
    fun containerColor(isLightTheme: Boolean): Color =
        if (isLightTheme) lightContainerColor else darkContainerColor

    /** 根据明暗主题返回玻璃上承载的文字 / 图标色。 */
    fun contentColor(isLightTheme: Boolean): Color =
        if (isLightTheme) lightContentColor else darkContentColor

    /**
     * 让状态栏图标颜色跟随玻璃主题（单一真源），供各 Compose 玻璃页面在组合体内调用。
     *
     * 玻璃顶栏在亮色下背景接近白 → 需要深色图标（isAppearanceLightStatusBars=true）；
     * 暗色下背景接近黑 → 浅色（白）图标（false）。即与 [isLightTheme] 同值，集中在此避免
     * 每个页面重复写 SideEffect + WindowCompat 样板。
     *
     * 背景：BaseActivity.setupSystemBar 在透明状态栏时把 window.statusBarColor 设为 TRANSPARENT，
     * 于是 isColorLight(TRANSPARENT)=false → setLightStatusBar(false) → 白图标；白图标落在接近白的
     * 玻璃顶栏上不可见（电池/信号被「遮住」）。此处按玻璃实际背景纠正，并随主题切换重组同步。
     */
    @Composable
    fun SyncStatusBarToGlassTheme(
        context: Context = LocalContext.current,
        isLightTheme: Boolean = isLightTheme(context),
    ) {
        SideEffect {
            (context as? Activity)?.window?.let { win ->
                WindowCompat.getInsetsController(win, win.decorView)
                    .isAppearanceLightStatusBars = isLightTheme
            }
        }
    }
}
