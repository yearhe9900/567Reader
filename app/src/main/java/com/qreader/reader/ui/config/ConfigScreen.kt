package com.qreader.reader.ui.config

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.GlassTopBar

/**
 * 设置页（ConfigActivity）的 Compose 外壳。
 *
 * 本页是「纯容器 + Fragment」结构：Activity 只负责挂一个 Fragment 容器，
 * 具体内容由 [OtherConfigFragment] / [ThemeConfigFragment] 等子 Fragment 提供。
 *
 * 因此 Compose 这里只做两件事：
 *  1. 画一块玻璃顶栏（替代原来的 `TitleBar` View）；
 *  2. 用 [AndroidView] 包一个 [FrameLayout] 作为 Fragment 的挂载点。
 *
 * 为什么不用 `AndroidFragment`（androidx.fragment.app.fragment.compose）：
 * 该组件属于 `androidx.fragment:fragment-compose`，本项目未引入；
 * 而 [AndroidView] 已在 BookInfoScreen 等多处使用（简介容器就是 FrameLayout），
 * 用它可以**零新增依赖**地完成同样的互操作。
 *
 * @param title        顶栏标题。由 Activity 的 `setTitle(Int)` 转发而来——
 *                     子 Fragment 内部用 `activity?.setTitle(R.string.xxx)` 驱动标题，
 *                     所以它必须是外部可变状态，不能写死在组合体内。
 * @param fragmentHost 由 Activity 创建的 [FrameLayout]，Fragment 事务往这里 replace。
 */
@Composable
fun ConfigScreen(
    title: String,
    fragmentHost: FrameLayout,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val backdrop = rememberLayerBackdrop()
    val isLightTheme = GlassConfig.isLightTheme(context)

    Box(Modifier.fillMaxSize()) {
        // ── 捕获层：Fragment 宿主（供顶栏玻璃采样）──
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            AndroidView(
                factory = { fragmentHost },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // ── 玻璃顶栏：必须在捕获层之外 ──
        GlassTopBar(
            title = title,
            backdrop = backdrop,
            onBack = onBack,
            isLightTheme = isLightTheme,
        )
    }
}
