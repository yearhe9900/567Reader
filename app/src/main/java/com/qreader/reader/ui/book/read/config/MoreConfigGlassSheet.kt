package com.qreader.reader.ui.book.read.config

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import com.kyant.backdrop.Backdrop
import com.qreader.reader.R
import com.qreader.reader.help.config.ReadBookConfig
import com.qreader.reader.ui.compose.glass.GlassConfig
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
 * 列表选择（双页等）由 [GlassListDialogHost] 以居中玻璃卡片展示。
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
    val isLightPage = remember { ColorUtils.isColorLight(ReadBookConfig.bgMeanColor) }
    val contentColor = GlassConfig.contentColor(isLightPage)

    // Preference 内的 LiquidToggle 需要采样源与明暗，组合期间挂到 Host
    DisposableEffect(backdrop, isLightPage) {
        GlassToggleHost.attach(backdrop, isLightPage)
        onDispose { GlassToggleHost.detach() }
    }

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
        // 容器必须 MATCH_PARENT，否则 Preference 列表只包内容宽，条目会挤在左侧
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
                        .replace(container.id, MoreConfigDialog.ReadPreferenceFragment(), tag)
                        .commitNowAllowingStateLoss()
                }
            },
        )
    }

    // ── 列表选择玻璃弹框（双页 / 进度条行为等）──
    if (GlassListDialogHost.isOpen) {
        LiquidGlassDialog(
            backdrop = backdrop,
            onDismiss = { GlassListDialogHost.dismiss() },
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .wrapContentHeight()
                .padding(vertical = 24.dp),
            cardRadius = GlassConfig.sheetCornerRadius,
            alignment = Alignment.Center,
            // 列表弹框需要压住设置面板，保留默认蒙板
            showScrim = true,
        ) { colors ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
            ) {
                BasicText(
                    text = GlassListDialogHost.title.toString(),
                    style = TextStyle(
                        color = colors.contentColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                val entries = GlassListDialogHost.entries
                val values = GlassListDialogHost.entryValues
                val selected = GlassListDialogHost.selectedValue
                entries.forEachIndexed { index, entry ->
                    val value = values.getOrNull(index)?.toString() ?: index.toString()
                    val checked = value == selected
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                GlassListDialogHost.confirm(value)
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        // 玻璃单选圈：选中强调色环 + 圆点
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    if (checked) colors.accentColor.copy(alpha = 0.18f)
                                    else colors.contentColor.copy(alpha = 0.08f)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (checked) {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(colors.accentColor)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        BasicText(
                            text = entry.toString(),
                            style = TextStyle(color = colors.contentColor, fontSize = 15.sp),
                            modifier = Modifier.weight(1f),
                        )
                        if (checked) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = colors.accentColor,
                                modifier = Modifier.size(18.dp),
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
                ?.let { activity.supportFragmentManager.beginTransaction().remove(it).commitAllowingStateLoss() }
        }
    }
}