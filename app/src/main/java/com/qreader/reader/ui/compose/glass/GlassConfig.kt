package com.qreader.reader.ui.compose.glass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 全局玻璃态统一配置。
 *
 * 集中管理所有玻璃组件的模糊半径与透镜折射参数，消除各组件内散落的硬编码数值。
 * 修改此处即可统一调整全部玻璃的观感。
 *
 * - bar 类玻璃（导航栏 / 标题栏 / 玻璃按钮 / 下拉菜单）使用 [blur] / [lensX] / [lensY]；
 * - 弹框类玻璃（主题弹框 / 删除框等）走独立的 [GlassDialogTokens]。
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

    // 容器半透明底色
    // 亮色用 0.8f 不透明度白——0.4f 时 vibrancy/blur 结果占 60%，整体偏灰；
    // 深色保持 0.4f（深灰底 + 低透明度 = 恰好出暗玻璃感）。
    val lightContainerColor = Color.White.copy(alpha = 0.9f)
    val darkContainerColor = Color(0xFF121212).copy(alpha = 0.4f)

    /** 根据明暗主题返回玻璃容器背景色。 */
    fun containerColor(isLightTheme: Boolean): Color =
        if (isLightTheme) lightContainerColor else darkContainerColor
}
