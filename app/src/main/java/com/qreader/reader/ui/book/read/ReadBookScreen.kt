package com.qreader.reader.ui.book.read

import android.widget.ImageView
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
 */
@Composable
fun ReadBookScreen(
    state: ReadPageOverlayState,
    readView: ReadView,
    cursorLeft: ImageView,
    cursorRight: ImageView,
    onBack: () -> Unit = {},
    onPrevChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    onSeekTo: (Int) -> Unit = {},
    onCatalog: () -> Unit = {},
    onReadAloud: () -> Unit = {},
    onFont: () -> Unit = {},
    onSetting: () -> Unit = {},
    onDismissMenu: () -> Unit = {},
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
                factory = { readView },
                modifier = Modifier.fillMaxSize(),
            )

            // 光标（Phase 6 改为 Compose）
            AndroidView(factory = { cursorLeft })
            AndroidView(factory = { cursorRight })
        }

        // ── ReadMenu 覆盖层 ──
        ReadMenuOverlay(
            state = state,
            backdrop = readBackdrop,
            onBack = onBack,
            onPrevChapter = onPrevChapter,
            onNextChapter = onNextChapter,
            onSeekTo = onSeekTo,
            onCatalog = onCatalog,
            onReadAloud = onReadAloud,
            onFont = onFont,
            onSetting = onSetting,
            onDismiss = onDismissMenu,
        )
    }
}
