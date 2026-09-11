package com.qreader.reader.ui.book.read.config

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.Capsule
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.help.config.ReadBookConfig
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.GlassDialogTokens
import com.qreader.reader.ui.compose.glass.GlassListDialogHost
import com.qreader.reader.ui.compose.glass.GlassToggleHost
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog
import com.qreader.reader.utils.ColorUtils

/**
 * 阅读页「设置」玻璃底部面板。
 *
 * 复用 [MoreConfigDialog.ReadPreferenceFragment] 承载 Preference 列表，
 * 外层用 [LiquidGlassDialog] 真玻璃采样阅读页正文；
 * 尺寸/圆角/留白统一走 [GlassConfig] 的 sheet* 配置，禁止散落硬编码。
 *
 * 列表选择（双页等）同样采样阅读页正文、且 **showScrim=false**：
 * 不要再叠一层可见渐变或全屏蒙板——渐变会盖住正文发黑，蒙板会把整屏糊成一团。
 */
@SuppressLint("CommitTransaction")
@Composable
fun MoreConfigGlassSheet(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
) {
    val activity = LocalContext.current as? FragmentActivity ?: return
    val containerId = remember { View.generateViewId() }
    val tag = remember { "more_config_glass_sheet" }
    val isLightPage = remember { ColorUtils.isColorLight(ReadBookConfig.bgMeanColor) }

    DisposableEffect(backdrop, isLightPage) {
        GlassToggleHost.attach(backdrop, isLightPage)
        onDispose { GlassToggleHost.detach() }
    }

    // 设置玻璃底部面板（采样阅读页正文）
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
                .fillMaxHeight()
                // 内容区顶部留白：Preference 列表第一行原本紧贴玻璃上边缘，
                // 视觉上像是被切掉一截，这里给 5dp 呼吸空间（对齐「界面」弹层的处理）
                .padding(top = GlassConfig.sheetContentTopPadding),
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

    // ── 列表选择玻璃弹框（双页等）──
    // 与设置面板同一 backdrop（正文），无全屏蒙板，正文/设置列表保持可见
    if (GlassListDialogHost.isOpen) {
        val entries = GlassListDialogHost.entries
        val values = GlassListDialogHost.entryValues
        val selectedIndexInit =
            values.indexOfFirst { it.toString() == GlassListDialogHost.selectedValue }
                .coerceAtLeast(0)
        var selectedIndex by remember(GlassListDialogHost.isOpen) {
            mutableStateOf(selectedIndexInit)
        }

        LiquidGlassDialog(
            backdrop = backdrop,
            onDismiss = { GlassListDialogHost.dismiss() },
            modifier = Modifier.fillMaxWidth(0.78f),
            cardRadius = GlassConfig.dialogCardRadius,
            contentPadding = PaddingValues(0.dp),
            alignment = Alignment.Center,
            showScrim = false,
        ) { colors ->
            val contentColor = colors.contentColor
            val accentColor = colors.accentColor
            val containerColor = colors.containerColor

            BasicText(
                text = GlassListDialogHost.title.toString(),
                modifier = Modifier.padding(28.dp, 24.dp, 28.dp, 12.dp),
                style = TextStyle(contentColor, 24.sp, FontWeight.Medium),
            )

            Column(Modifier.verticalScroll(rememberScrollState())) {
                entries.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                selectedIndex = index
                                val value = values.getOrNull(index)?.toString()
                                    ?: index.toString()
                                GlassListDialogHost.confirm(value)
                            }
                            .padding(
                                start = 24.dp,
                                end = 24.dp,
                                top = 10.dp,
                                bottom = if (index == entries.lastIndex) 24.dp else 10.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { Capsule() },
                                    effects = {
                                        if (!AppConfig.isEInkMode) {
                                            colorControls(
                                                brightness = GlassDialogTokens.cardBrightness,
                                                saturation = GlassDialogTokens.cardSaturation,
                                            )
                                            blur(GlassDialogTokens.widgetBlur.toPx())
                                            lens(
                                                GlassDialogTokens.widgetLensX.toPx(),
                                                GlassDialogTokens.widgetLensY.toPx(),
                                                depthEffect = true,
                                            )
                                        }
                                    },
                                    highlight = { Highlight.Plain },
                                    onDrawSurface = {
                                        drawRect(
                                            if (index == selectedIndex) accentColor
                                            else containerColor.copy(alpha = 0.3f),
                                        )
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (index == selectedIndex) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(Capsule())
                                        .background(Color.White)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier.height(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = entry.toString(),
                                style = TextStyle(contentColor.copy(alpha = 0.9f), 16.sp),
                            )
                        }
                    }
                }
            }
        }
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