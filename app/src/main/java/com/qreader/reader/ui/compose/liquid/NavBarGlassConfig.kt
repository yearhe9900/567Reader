package com.qreader.reader.ui.compose.liquid

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.qreader.reader.constant.PreferKey
import com.qreader.reader.utils.getPrefBoolean
import com.qreader.reader.utils.getPrefString
import com.qreader.reader.utils.putPrefBoolean
import com.qreader.reader.utils.putPrefString

/**
 * 底部导航栏玻璃态配置的持久化模型。
 *
 * 与 AndroidLiquidGlass 官方 GlassPlayground 对齐（去掉对 Capsule 无效的 Corner radius）：
 *  - Blur radius
 *  - Refraction height
 *  - Refraction amount
 *  - Chromatic aberration
 * 容器背景透明度固定为 0.4f（官方 LiquidBottomTabs 默认值）。
 * 所有浮点字段通过 SharedPreferences 以字符串形式存取（项目未提供 float pref helper）。
 *
 * @param blurRadiusDp        背景模糊半径（dp），默认 8dp
 * @param refractionHeightDp  折射高度（lens height），默认 24dp
 * @param refractionAmountDp  折射强度（lens amount），默认 24dp
 * @param chromaticAberration 是否开启色差效果，默认开启
 */
data class NavBarGlassConfig(
    val blurRadiusDp: Float = 8f,
    val refractionHeightDp: Float = 24f,
    val refractionAmountDp: Float = 24f,
    val chromaticAberration: Boolean = true,
) {
    /** 转换为 [LiquidBottomTabs] 的玻璃样式参数。 */
    fun toLiquidGlassStyle() = LiquidGlassStyle(
        blurRadiusDp = blurRadiusDp,
        refractionHeightDp = refractionHeightDp,
        refractionAmountDp = refractionAmountDp,
        chromaticAberration = chromaticAberration,
    )

    /** 根据明暗主题计算玻璃容器背景色（透明度固定 0.4f，与官方 LiquidBottomTabs 一致）。 */
    fun containerColor(isLightTheme: Boolean): Color {
        val base = if (isLightTheme) Color(0xFFFAFAFA) else Color(0xFF121212)
        return base.copy(alpha = 0.4f)
    }

    companion object {
        /** 从 SharedPreferences 读取配置，缺失字段回退到默认值。 */
        fun load(context: Context): NavBarGlassConfig = NavBarGlassConfig(
            blurRadiusDp = context.getPrefString(PreferKey.navBarBlurRadius)
                ?.toFloatOrNull() ?: 8f,
            refractionHeightDp = context.getPrefString(PreferKey.navBarRefractionHeight)
                ?.toFloatOrNull() ?: 24f,
            refractionAmountDp = context.getPrefString(PreferKey.navBarRefractionAmount)
                ?.toFloatOrNull() ?: 24f,
            chromaticAberration = context.getPrefBoolean(
                PreferKey.navBarChromaticAberration, true
            ),
        )

        /** 将配置写入 SharedPreferences。 */
        fun save(context: Context, config: NavBarGlassConfig) {
            context.putPrefString(PreferKey.navBarBlurRadius, config.blurRadiusDp.toString())
            context.putPrefString(
                PreferKey.navBarRefractionHeight,
                config.refractionHeightDp.toString()
            )
            context.putPrefString(
                PreferKey.navBarRefractionAmount,
                config.refractionAmountDp.toString()
            )
            context.putPrefBoolean(
                PreferKey.navBarChromaticAberration,
                config.chromaticAberration
            )
        }
    }
}
