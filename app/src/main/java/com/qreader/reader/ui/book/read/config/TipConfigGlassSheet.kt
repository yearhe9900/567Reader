package com.qreader.reader.ui.book.read.config

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.kyant.backdrop.Backdrop
import com.qreader.reader.constant.EventBus
import com.qreader.reader.help.config.ReadBookConfig
import com.qreader.reader.help.config.ReadTipConfig
import com.qreader.reader.lib.dialogs.selector
import com.qreader.reader.ui.book.read.ReadBookActivity
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.GlassSliderRow
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog
import com.qreader.reader.utils.ColorUtils
import com.qreader.reader.utils.hexString
import com.qreader.reader.utils.postEvent

/**
 * 页眉/页脚信息玻璃面板（替代 TipConfigDialog）。
 * 自定义颜色仍走 ColorPicker。
 */
@Composable
fun TipConfigGlassSheet(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
) {
    val activity = LocalContext.current as? ReadBookActivity ?: return
    val isLight = ColorUtils.isColorLight(ReadBookConfig.bgMeanColor)
    val contentColor = GlassConfig.contentColor(isLight)

    var titleMode by remember { mutableIntStateOf(ReadBookConfig.titleMode.coerceIn(0, 2)) }
    var titleSize by remember { mutableIntStateOf(ReadBookConfig.titleSize) }
    var titleTop by remember { mutableIntStateOf(ReadBookConfig.titleTopSpacing) }
    var titleBottom by remember { mutableIntStateOf(ReadBookConfig.titleBottomSpacing) }
    var headerModeText by remember {
        mutableStateOf(ReadTipConfig.getHeaderModes(activity)[ReadTipConfig.headerMode] ?: "")
    }
    var footerModeText by remember {
        mutableStateOf(ReadTipConfig.getFooterModes(activity)[ReadTipConfig.footerMode] ?: "")
    }
    var headerLeft by remember { mutableStateOf(tipNameOf(ReadTipConfig.tipHeaderLeft)) }
    var headerMiddle by remember { mutableStateOf(tipNameOf(ReadTipConfig.tipHeaderMiddle)) }
    var headerRight by remember { mutableStateOf(tipNameOf(ReadTipConfig.tipHeaderRight)) }
    var footerLeft by remember { mutableStateOf(tipNameOf(ReadTipConfig.tipFooterLeft)) }
    var footerMiddle by remember { mutableStateOf(tipNameOf(ReadTipConfig.tipFooterMiddle)) }
    var footerRight by remember { mutableStateOf(tipNameOf(ReadTipConfig.tipFooterRight)) }
    var tipColorText by remember { mutableStateOf(tipColorLabel()) }
    var tipDividerText by remember { mutableStateOf(tipDividerColorLabel()) }

    LiquidGlassDialog(
        backdrop = backdrop,
        onDismiss = onDismiss,
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
            BasicText(
                text = "信息栏",
                style = TextStyle(colors.contentColor, 16.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                listOf("默认", "居中", "隐藏").forEachIndexed { i, label ->
                    val selected = titleMode == i
                    BasicText(
                        text = label,
                        style = TextStyle(
                            color = if (selected) colors.accentColor else colors.contentColor,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                titleMode = i
                                ReadBookConfig.titleMode = i
                                postEvent(EventBus.UP_CONFIG, arrayListOf(5))
                            }
                            .padding(vertical = 8.dp),
                    )
                }
            }

            GlassSliderRow("字号", titleSize, 40, colors.contentColor, backdrop) {
                titleSize = it
                ReadBookConfig.titleSize = it
                postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
            }
            GlassSliderRow("上间距", titleTop, 100, colors.contentColor, backdrop) {
                titleTop = it
                ReadBookConfig.titleTopSpacing = it
                postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
            }
            GlassSliderRow("下间距", titleBottom, 100, colors.contentColor, backdrop) {
                titleBottom = it
                ReadBookConfig.titleBottomSpacing = it
                postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
            }

            SelectRow("页眉显示", headerModeText, colors.contentColor) {
                val modes = ReadTipConfig.getHeaderModes(activity)
                activity.selector(items = modes.values.toList()) { _, i ->
                    ReadTipConfig.headerMode = modes.keys.toList()[i]
                    headerModeText = modes[ReadTipConfig.headerMode] ?: ""
                    postEvent(EventBus.UP_CONFIG, arrayListOf(2))
                }
            }
            SelectRow("页脚显示", footerModeText, colors.contentColor) {
                val modes = ReadTipConfig.getFooterModes(activity)
                activity.selector(items = modes.values.toList()) { _, i ->
                    ReadTipConfig.footerMode = modes.keys.toList()[i]
                    footerModeText = modes[ReadTipConfig.footerMode] ?: ""
                    postEvent(EventBus.UP_CONFIG, arrayListOf(2))
                }
            }

            TipSlotsRow("页眉", headerLeft, headerMiddle, headerRight, colors.contentColor) { which ->
                activity.selector(items = ReadTipConfig.tipNames) { _, i ->
                    val v = ReadTipConfig.tipValues[i]
                    clearRepeatTip(v)
                    when (which) {
                        0 -> {
                            ReadTipConfig.tipHeaderLeft = v
                            headerLeft = ReadTipConfig.tipNames[i]
                        }
                        1 -> {
                            ReadTipConfig.tipHeaderMiddle = v
                            headerMiddle = ReadTipConfig.tipNames[i]
                        }
                        2 -> {
                            ReadTipConfig.tipHeaderRight = v
                            headerRight = ReadTipConfig.tipNames[i]
                        }
                    }
                    postEvent(EventBus.UP_CONFIG, arrayListOf(2, 6))
                }
            }
            TipSlotsRow("页脚", footerLeft, footerMiddle, footerRight, colors.contentColor) { which ->
                activity.selector(items = ReadTipConfig.tipNames) { _, i ->
                    val v = ReadTipConfig.tipValues[i]
                    clearRepeatTip(v)
                    when (which) {
                        0 -> {
                            ReadTipConfig.tipFooterLeft = v
                            footerLeft = ReadTipConfig.tipNames[i]
                        }
                        1 -> {
                            ReadTipConfig.tipFooterMiddle = v
                            footerMiddle = ReadTipConfig.tipNames[i]
                        }
                        2 -> {
                            ReadTipConfig.tipFooterRight = v
                            footerRight = ReadTipConfig.tipNames[i]
                        }
                    }
                    postEvent(EventBus.UP_CONFIG, arrayListOf(2, 6))
                }
            }

            SelectRow("信息颜色", tipColorText, colors.contentColor) {
                activity.selector(items = ReadTipConfig.tipColorNames) { _, i ->
                    if (i == 0) {
                        ReadTipConfig.tipColor = 0
                        tipColorText = ReadTipConfig.tipColorNames.first()
                        postEvent(EventBus.UP_CONFIG, arrayListOf(2))
                    } else {
                        ColorPickerDialog.newBuilder()
                            .setShowAlphaSlider(false)
                            .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                            .setDialogId(TipConfigDialog.TIP_COLOR)
                            .show(activity)
                    }
                }
            }
            SelectRow("分割线颜色", tipDividerText, colors.contentColor) {
                activity.selector(items = ReadTipConfig.tipDividerColorNames) { _, i ->
                    if (i == 0 || i == 1) {
                        ReadTipConfig.tipDividerColor = i - 1
                        tipDividerText = ReadTipConfig.tipDividerColorNames[i]
                        postEvent(EventBus.UP_CONFIG, arrayListOf(2))
                    } else {
                        ColorPickerDialog.newBuilder()
                            .setShowAlphaSlider(false)
                            .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                            .setDialogId(TipConfigDialog.TIP_DIVIDER_COLOR)
                            .show(activity)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SelectRow(
    label: String,
    value: String,
    contentColor: Color,
    onClick: () -> Unit,
) {
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
        BasicText(text = value, style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp))
    }
}

@Composable
private fun TipSlotsRow(
    label: String,
    left: String,
    middle: String,
    right: String,
    contentColor: Color,
    onSlotClick: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        BasicText(text = label, style = TextStyle(contentColor, 14.sp, fontWeight = FontWeight.Medium))
        Row(Modifier.fillMaxWidth()) {
            listOf(left, middle, right).forEachIndexed { i, text ->
                BasicText(
                    text = text,
                    style = TextStyle(contentColor.copy(alpha = 0.85f), 13.sp, textAlign = TextAlign.Center),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSlotClick(i) }
                        .padding(vertical = 8.dp),
                )
            }
        }
    }
}

private fun tipNameOf(value: Int): String {
    val idx = ReadTipConfig.tipValues.indexOf(value)
    return if (idx >= 0) ReadTipConfig.tipNames[idx] else ReadTipConfig.tipNames[ReadTipConfig.none]
}

private fun clearRepeatTip(repeat: Int) {
    if (repeat == ReadTipConfig.none) return
    if (ReadTipConfig.tipHeaderLeft == repeat) ReadTipConfig.tipHeaderLeft = ReadTipConfig.none
    if (ReadTipConfig.tipHeaderMiddle == repeat) ReadTipConfig.tipHeaderMiddle = ReadTipConfig.none
    if (ReadTipConfig.tipHeaderRight == repeat) ReadTipConfig.tipHeaderRight = ReadTipConfig.none
    if (ReadTipConfig.tipFooterLeft == repeat) ReadTipConfig.tipFooterLeft = ReadTipConfig.none
    if (ReadTipConfig.tipFooterMiddle == repeat) ReadTipConfig.tipFooterMiddle = ReadTipConfig.none
    if (ReadTipConfig.tipFooterRight == repeat) ReadTipConfig.tipFooterRight = ReadTipConfig.none
}

private fun tipColorLabel(): String {
    val tipColor = ReadTipConfig.tipColor
    return if (tipColor == 0) ReadTipConfig.tipColorNames.first() else "#${tipColor.hexString}"
}

private fun tipDividerColorLabel(): String {
    val c = ReadTipConfig.tipDividerColor
    return when (c) {
        -1, 0 -> ReadTipConfig.tipDividerColorNames[c + 1]
        else -> "#${c.hexString}"
    }
}
