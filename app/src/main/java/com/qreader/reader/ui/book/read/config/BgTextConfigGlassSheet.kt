package com.qreader.reader.ui.book.read.config

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.kyant.backdrop.Backdrop
import com.qreader.reader.R
import com.qreader.reader.constant.EventBus
import com.qreader.reader.help.DefaultData
import com.qreader.reader.help.config.ReadBookConfig
import com.qreader.reader.lib.dialogs.alert
import com.qreader.reader.lib.dialogs.selector
import com.qreader.reader.ui.book.read.ReadBookActivity
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.GlassSliderRow
import com.qreader.reader.ui.compose.glass.GlassToggleRow
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog
import com.qreader.reader.utils.ColorUtils
import com.qreader.reader.utils.postEvent
import com.qreader.reader.utils.showDialogFragment

/**
 * 文字/背景样式玻璃面板（核心控件；导入/导出/系统选图仍打开原 Dialog）。
 */
@Composable
fun BgTextConfigGlassSheet(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
) {
    val activity = LocalContext.current as? ReadBookActivity ?: return
    val isLight = ColorUtils.isColorLight(ReadBookConfig.bgMeanColor)
    val contentColor = GlassConfig.contentColor(isLight)

    var name by remember { mutableStateOf(ReadBookConfig.durConfig.name.ifBlank { "文字" }) }
    var darkStatus by remember { mutableStateOf(ReadBookConfig.durConfig.curStatusIconDark()) }
    var underline by remember { mutableIntStateOf(ReadBookConfig.durConfig.underlineMode) }
    var bgAlpha by remember { mutableIntStateOf(ReadBookConfig.bgAlpha) }
    val bgAssets = remember { activity.assets.list("bg")?.toList().orEmpty() }

    LiquidGlassDialog(
        backdrop = backdrop,
        onDismiss = {
            ReadBookConfig.save()
            onDismiss()
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .navigationBarsPadding(),
        cardRadius = GlassConfig.sheetCornerRadius,
        alignment = Alignment.BottomCenter,
        showScrim = false,
    ) { colors ->
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = name,
                    style = TextStyle(colors.contentColor, 16.sp, fontWeight = FontWeight.Medium),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            activity.alert(R.string.style_name) {
                                val edit = android.widget.EditText(activity).apply {
                                    hint = "name"
                                    setText(ReadBookConfig.durConfig.name)
                                }
                                customView { edit }
                                okButton {
                                    val t = edit.text?.toString()
                                    if (!t.isNullOrBlank()) {
                                        name = t
                                        ReadBookConfig.durConfig.name = t
                                    }
                                }
                                cancelButton()
                            }
                        },
                )
                Spacer(Modifier.width(8.dp))
                ActionChip("恢复", colors.contentColor) {
                    val defaults = DefaultData.readConfigs
                    activity.selector("选择预设布局", defaults.map { it.name }) { _, i ->
                        if (i >= 0) {
                            ReadBookConfig.durConfig = defaults[i].copy()
                            name = ReadBookConfig.durConfig.name.ifBlank { "文字" }
                            darkStatus = ReadBookConfig.durConfig.curStatusIconDark()
                            underline = ReadBookConfig.durConfig.underlineMode
                            bgAlpha = ReadBookConfig.bgAlpha
                            postEvent(EventBus.UP_CONFIG, arrayListOf(1, 2, 5, 13))
                        }
                    }
                }
                ActionChip("导入", colors.contentColor) {
                    activity.showDialogFragment<BgTextConfigDialog>()
                    onDismiss()
                }
                ActionChip("导出", colors.contentColor) {
                    activity.showDialogFragment<BgTextConfigDialog>()
                    onDismiss()
                }
            }

            GlassToggleRow("深色状态栏图标", darkStatus, colors.contentColor, colors.accentColor) {
                darkStatus = it
                ReadBookConfig.durConfig.setCurStatusIconDark(it)
                activity.upSystemUiVisibility()
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText("下划线", style = TextStyle(colors.contentColor, 14.sp), modifier = Modifier.weight(1f))
                listOf("关闭", "实线", "虚线").forEachIndexed { i, label ->
                    val selected = underline == i
                    BasicText(
                        text = label,
                        style = TextStyle(
                            color = if (selected) colors.accentColor else colors.contentColor,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) colors.accentColor.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                underline = i
                                ReadBookConfig.durConfig.underlineMode = i
                                postEvent(EventBus.UP_CONFIG, arrayListOf(6, 9, 11))
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }

            ColorRow("文字颜色", colors.contentColor) {
                ColorPickerDialog.newBuilder()
                    .setColor(ReadBookConfig.durConfig.curTextColor())
                    .setShowAlphaSlider(false)
                    .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                    .setDialogId(BgTextConfigDialog.TEXT_COLOR)
                    .show(activity)
            }
            ColorRow("背景颜色", colors.contentColor) {
                val bg = if (ReadBookConfig.durConfig.curBgType() == 0)
                    ReadBookConfig.durConfig.curBgStr().toColorInt()
                else "#015A86".toColorInt()
                ColorPickerDialog.newBuilder()
                    .setColor(bg)
                    .setShowAlphaSlider(false)
                    .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                    .setDialogId(BgTextConfigDialog.BG_COLOR)
                    .show(activity)
            }
            ColorRow("强调色", colors.contentColor) {
                ColorPickerDialog.newBuilder()
                    .setColor(ReadBookConfig.durConfig.curTextAccentColor())
                    .setShowAlphaSlider(false)
                    .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                    .setDialogId(BgTextConfigDialog.TEXT_ACCENT_COLOR)
                    .show(activity)
            }

            GlassSliderRow("背景透明度", bgAlpha, 255, colors.contentColor, backdrop) {
                bgAlpha = it
                ReadBookConfig.bgAlpha = it
                postEvent(EventBus.UP_CONFIG, arrayListOf(3))
            }

            BasicText(
                text = "背景图片",
                style = TextStyle(colors.contentColor, 14.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    ActionChip(stringResource(R.string.select_image), colors.contentColor) {
                        activity.showDialogFragment<BgTextConfigDialog>()
                        onDismiss()
                    }
                }
                items(bgAssets) { asset ->
                    BasicText(
                        text = asset.substringBeforeLast('.'),
                        style = TextStyle(colors.contentColor, 12.sp, textAlign = TextAlign.Center),
                        modifier = Modifier
                            .width(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.contentColor.copy(alpha = 0.08f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                ReadBookConfig.durConfig.setCurBg(0, asset)
                                postEvent(EventBus.UP_CONFIG, arrayListOf(1))
                            }
                            .padding(vertical = 10.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    DisposableEffect(Unit) {
        onDispose { ReadBookConfig.save() }
    }
}

@Composable
private fun ActionChip(label: String, contentColor: Color, onClick: () -> Unit) {
    BasicText(
        text = label,
        style = TextStyle(contentColor, 13.sp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

@Composable
private fun ColorRow(label: String, contentColor: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(text = label, style = TextStyle(contentColor, 14.sp), modifier = Modifier.weight(1f))
        BasicText(text = "›", style = TextStyle(contentColor.copy(alpha = 0.5f), 16.sp))
    }
}
