package com.qreader.reader.ui.compose.glass

import com.kyant.backdrop.Backdrop

/**
 * PreferenceFragment 与 Compose 玻璃 Toggle 之间的桥。
 *
 * 阅读页设置等仍是 Preference 列表（View 体系），但 Switch 要用 LiquidToggle；
 * LiquidToggle 需要 Backdrop 采样源，由 [com.qreader.reader.ui.book.read.config.MoreConfigGlassSheet]
 * 在组合时写入 [backdrop] / [isLightTheme]，onBind 时取出使用。
 * 仅服务「玻璃面板内的 Preference」场景，不要挪作全局单例配置。
 */
object GlassToggleHost {
    @Volatile
    var backdrop: Backdrop? = null

    @Volatile
    var isLightTheme: Boolean = false

    fun attach(backdrop: Backdrop, isLightTheme: Boolean) {
        this.backdrop = backdrop
        this.isLightTheme = isLightTheme
    }

    fun detach() {
        backdrop = null
    }
}
