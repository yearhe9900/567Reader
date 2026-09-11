package com.qreader.reader.ui.qrcode

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.qreader.reader.R
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.GlassTopBar
import com.qreader.reader.ui.compose.glass.GlassTopBarIcon
import com.qreader.reader.ui.compose.glass.GlassTopBarReservedHeight

/**
 * 二维码扫描页（QrCodeActivity）的 Compose 外壳。
 *
 * 结构：玻璃顶栏 + 相机预览（[QrCodeFragment] 由 Activity 事务挂进来）。
 *
 * ── 与其它页面的一个差异 ──
 * 本页顶栏不做「悬浮盖在内容上」——它是**独立占位**的：顶栏占据顶部空间，
 * 相机预览从顶栏下方开始。原因是相机预览属于 `BarcodeCameraScanFragment`，
 * 内部有自己的取景框与对焦逻辑，若被顶栏盖住会出现「取景区被遮挡但仍可扫码」的怪异手感。
 * 因此这里用 Column 让顶栏与预览上下分栏，而不是像 BookInfoScreen 那样叠加。
 */
@Composable
fun QrCodeScreen(
    title: String,
    fragmentHost: FrameLayout,
    onBack: () -> Unit,
    onPickFromGallery: () -> Unit,
) {
    val context = LocalContext.current
    val backdrop = rememberLayerBackdrop()
    val isLightTheme = GlassConfig.isLightTheme(context)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // ── 顶栏占位：保证预览从顶栏下方开始 ──
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(GlassTopBarReservedHeight)
                    .statusBarsPadding(),
            )

            // ── 捕获层：相机预览（供顶栏玻璃采样；本页顶栏下方即内容，符合捕获层约定）──
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
        }

        // ── 玻璃顶栏：在捕获层之外 ──
        GlassTopBar(
            title = title,
            backdrop = backdrop,
            onBack = onBack,
            isLightTheme = isLightTheme,
            actions = {
                GlassTopBarIcon(
                    iconRes = R.drawable.ic_image,
                    contentDescription = context.getString(R.string.gallery),
                    contentColor = GlassConfig.contentColor(isLightTheme),
                    onClick = onPickFromGallery,
                )
            },
        )
    }
}
