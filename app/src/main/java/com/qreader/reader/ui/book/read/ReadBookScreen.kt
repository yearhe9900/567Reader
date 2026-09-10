package com.qreader.reader.ui.book.read

import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.qreader.reader.ui.book.read.config.AutoReadGlassSheet
import com.qreader.reader.ui.book.read.config.ClickActionGlassSheet
import com.qreader.reader.ui.book.read.config.MoreConfigGlassSheet
import com.qreader.reader.ui.book.read.config.ReadStyleGlassSheet
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
    onSearch: () -> Unit = {},
    onAutoPage: () -> Unit = {},
    onReplaceRule: () -> Unit = {},
    onToggleNightTheme: () -> Unit = {},
    onToggleBrightnessAuto: () -> Unit = {},
    onToggleBrightnessPos: () -> Unit = {},
    onBrightnessChange: (Float) -> Unit = {},
    onBrightnessChangeFinished: (Float) -> Unit = {},
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
            onSearch = onSearch,
            onAutoPage = onAutoPage,
            onReplaceRule = onReplaceRule,
            onToggleNightTheme = onToggleNightTheme,
            onToggleBrightnessAuto = onToggleBrightnessAuto,
            onToggleBrightnessPos = onToggleBrightnessPos,
            onBrightnessChange = onBrightnessChange,
            onBrightnessChangeFinished = onBrightnessChangeFinished,
            onCatalog = onCatalog,
            onReadAloud = onReadAloud,
            onFont = onFont,
            onSetting = onSetting,
            onDismiss = onDismissMenu,
        )

        // ── 设置玻璃底部面板（采样 ReadView，必须在捕获层之外）──
        if (state.showMoreConfigDialog) {
            MoreConfigGlassSheet(
                backdrop = readBackdrop,
                onDismiss = {
                    state.showMoreConfigDialog = false
                    if (state.bottomDialogCount > 0) {
                        state.bottomDialogCount--
                    }
                },
            )
        }

        // ── 界面玻璃底部面板 ──
        if (state.showReadStyleDialog) {
            ReadStyleGlassSheet(
                backdrop = readBackdrop,
                onDismiss = {
                    state.showReadStyleDialog = false
                    if (state.bottomDialogCount > 0) {
                        state.bottomDialogCount--
                    }
                },
            )
        }

        // ── 自动翻页玻璃面板 ──
        if (state.showAutoReadDialog) {
            AutoReadGlassSheet(
                backdrop = readBackdrop,
                onDismiss = {
                    state.showAutoReadDialog = false
                    if (state.bottomDialogCount > 0) {
                        state.bottomDialogCount--
                    }
                },
            )
        }

        // ── 点击区域设置 ──
        if (state.showClickActionDialog) {
            ClickActionGlassSheet(
                backdrop = readBackdrop,
                onDismiss = {
                    state.showClickActionDialog = false
                    if (state.bottomDialogCount > 0) {
                        state.bottomDialogCount--
                    }
                },
            )
        }
    }
}
