package com.qreader.reader.ui.book.read.config

import android.annotation.SuppressLint
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import com.kyant.backdrop.Backdrop
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog

/**
 * 阅读页「设置」玻璃底部面板。
 *
 * 复用 [MoreConfigDialog.ReadPreferenceFragment] 承载 Preference 列表，
 * 外层用 [LiquidGlassDialog] 真玻璃采样阅读页正文；
 * 尺寸/圆角/留白统一走 [GlassConfig] 的 sheet* 配置，禁止散落硬编码。
 */
@SuppressLint("CommitTransaction")
@Composable
fun MoreConfigGlassSheet(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity ?: return
    val containerId = remember { View.generateViewId() }
    val tag = remember { "more_config_glass_sheet" }

    LiquidGlassDialog(
        backdrop = backdrop,
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlassConfig.sheetHorizontalPadding)
            .height(GlassConfig.sheetHeight)
            .navigationBarsPadding(),
        cardRadius = GlassConfig.sheetCornerRadius,
        alignment = Alignment.BottomCenter,
        // 正文保持清晰可读，不加全屏压暗/模糊蒙板
        showScrim = false,
    ) {
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply { id = containerId }
            },
            update = { container ->
                val fm = activity.supportFragmentManager
                if (fm.findFragmentById(container.id) == null) {
                    fm.beginTransaction()
                        .replace(container.id, MoreConfigDialog.ReadPreferenceFragment(), tag)
                        .commitNowAllowingStateLoss()
                }
            },
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            activity.supportFragmentManager
                .findFragmentByTag(tag)
                ?.let { activity.supportFragmentManager.beginTransaction().remove(it).commitAllowingStateLoss() }
        }
    }
}
