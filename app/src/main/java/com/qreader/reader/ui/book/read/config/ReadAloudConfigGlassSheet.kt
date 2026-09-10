package com.qreader.reader.ui.book.read.config

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.qreader.reader.ui.compose.glass.GlassToggleHost
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog
import com.qreader.reader.help.config.ReadBookConfig
import com.qreader.reader.utils.ColorUtils

/**
 * 朗读设置玻璃面板（托管 [ReadAloudConfigDialog.ReadAloudPreferenceFragment]）。
 * 与 MoreConfigGlassSheet 同构。
 */
@SuppressLint("CommitTransaction")
@Composable
fun ReadAloudConfigGlassSheet(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
) {
    val activity = LocalContext.current as? FragmentActivity ?: return
    val containerId = remember { View.generateViewId() }
    val tag = remember { "read_aloud_config_glass_sheet" }
    val isLightPage = remember { ColorUtils.isColorLight(ReadBookConfig.bgMeanColor) }

    DisposableEffect(backdrop, isLightPage) {
        GlassToggleHost.attach(backdrop, isLightPage)
        onDispose { GlassToggleHost.detach() }
    }

    LiquidGlassDialog(
        backdrop = backdrop,
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .height(GlassConfig.sheetHeight)
            .navigationBarsPadding()
            .padding(horizontal = GlassConfig.sheetHorizontalPadding),
        cardRadius = GlassConfig.sheetCornerRadius,
        alignment = Alignment.BottomCenter,
        showScrim = false,
    ) {
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    id = containerId
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            update = { container ->
                val fm = activity.supportFragmentManager
                if (fm.findFragmentById(container.id) == null) {
                    fm.beginTransaction()
                        .replace(
                            container.id,
                            ReadAloudConfigDialog.ReadAloudPreferenceFragment(),
                            tag,
                        )
                        .commitNowAllowingStateLoss()
                }
            },
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            activity.supportFragmentManager
                .findFragmentByTag(tag)
                ?.let {
                    activity.supportFragmentManager.beginTransaction().remove(it)
                        .commitAllowingStateLoss()
                }
        }
    }
}
