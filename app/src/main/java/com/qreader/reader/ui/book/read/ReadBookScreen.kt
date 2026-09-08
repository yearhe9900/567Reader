package com.qreader.reader.ui.book.read

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.qreader.reader.ui.book.read.page.ReadView

/**
 * 阅读页 Compose 根布局。
 *
 * Phase 1：ReadView 用 AndroidView 承载，菜单 overlay 先留空。
 * Phase 2+：逐步替换为 Compose 玻璃 overlay。
 */
@Composable
fun ReadBookScreen(
    state: ReadPageOverlayState,
    readViewFactory: (android.content.Context) -> ReadView,
    onReadViewCreated: (ReadView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val readBackdrop = rememberLayerBackdrop()

    Box(modifier = modifier.fillMaxSize()) {
        // ── 捕获层：ReadView（正文渲染）──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(readBackdrop)
        ) {
            AndroidView(
                factory = { ctx ->
                    readViewFactory(ctx).also { onReadViewCreated(it) }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // ── ReadMenu 覆盖层（Phase 2 实现）──
        // ReadMenuOverlay(state = state, backdrop = readBackdrop)

        // ── SearchMenu 覆盖层（Phase 3 实现）──
        // SearchMenuOverlay(state = state, backdrop = readBackdrop)
    }
}
