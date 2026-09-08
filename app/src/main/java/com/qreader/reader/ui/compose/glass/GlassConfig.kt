package com.qreader.reader.ui.compose.glass

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
}
