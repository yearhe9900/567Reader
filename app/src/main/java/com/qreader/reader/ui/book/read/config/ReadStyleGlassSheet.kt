package com.qreader.reader.ui.book.read.config

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.liuyueyi.quick.transfer.constants.TransType
import com.kyant.backdrop.Backdrop
import com.qreader.reader.R
import com.qreader.reader.constant.EventBus
import com.qreader.reader.constant.PageAnim
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.help.config.ReadBookConfig
import com.qreader.reader.lib.dialogs.selector
import com.qreader.reader.lib.theme.accentColor
import com.qreader.reader.model.ReadBook
import com.qreader.reader.ui.book.read.ReadBookActivity
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog
import com.qreader.reader.ui.compose.liquid.LiquidSlider
import com.qreader.reader.ui.font.FontSelectDialog
import com.qreader.reader.ui.widget.image.CircleImageView
import com.qreader.reader.utils.ChineseUtils
import com.qreader.reader.utils.ColorUtils
import com.qreader.reader.utils.dpToPx
import com.qreader.reader.utils.postEvent
import com.qreader.reader.utils.showDialogFragment
import splitties.views.onLongClick

/**
 * 阅读页「界面」玻璃底部面板 — **纯 Compose**。
 *
 * 替代 AndroidView 托管 dialog_read_book_style（layout 中写 snapshot state / check RadioGroup
 * 会触发 loadContent 导致整页卡死）。功能与原 View 版一一对应。
 */
@Composable
fun ReadStyleGlassSheet(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
) {
    val activity = LocalContext.current as? ReadBookActivity ?: return
    val contentColor = GlassConfig.contentColor(
        ColorUtils.isColorLight(ReadBookConfig.bgMeanColor)
    )
    val accent = Color(activity.accentColor)

    // 供面板内 LiquidSlider 采样正文（纯 Compose，无 AndroidView layout 写状态）
    GlassSliderBackdrop.current = backdrop
    DisposableEffect(backdrop) {
        GlassSliderBackdrop.current = backdrop
        onDispose { GlassSliderBackdrop.current = null }
    }

    // 本地镜像，避免每帧读全局；变更时写回 ReadBookConfig
    var textBold by remember { mutableIntStateOf(ReadBookConfig.textBold) }
    var textSize by remember { mutableIntStateOf(ReadBookConfig.textSize) }
    var letterSpacing by remember { mutableIntStateOf((ReadBookConfig.letterSpacing * 100).toInt() + 50) }
    var lineSpacing by remember { mutableIntStateOf(ReadBookConfig.lineSpacingExtra) }
    var paragraphSpacing by remember { mutableIntStateOf(ReadBookConfig.paragraphSpacing) }
    var pageAnim by remember { mutableIntStateOf(ReadBook.pageAnim()) }
    var shareLayout by remember { mutableStateOf(ReadBookConfig.shareLayout) }
    var styleSelect by remember { mutableIntStateOf(ReadBookConfig.styleSelect) }
    var styleTick by remember { mutableIntStateOf(0) }

    LiquidGlassDialog(
        backdrop = backdrop,
        onDismiss = {
            ReadBookConfig.save()
            onDismiss()
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .navigationBarsPadding()
            .padding(horizontal = GlassConfig.sheetHorizontalPadding),
        cardRadius = GlassConfig.sheetCornerRadius,
        alignment = Alignment.BottomCenter,
        showScrim = false,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            // ── 顶部：粗细 / 字体 / 缩进 / 简繁 / 边距 / 信息 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FontWeightRow(
                    selected = textBold,
                    contentColor = contentColor,
                    accent = accent,
                ) { index ->
                    textBold = index
                    ReadBookConfig.textBold = index
                    postEvent(EventBus.UP_CONFIG, arrayListOf(8, 9, 6))
                }
                ActionChip(stringResource(R.string.text_font), contentColor) {
                    activity.showDialogFragment<FontSelectDialog>()
                }
                ActionChip(stringResource(R.string.text_indent), contentColor) {
                    activity.selector(
                        title = activity.getString(R.string.text_indent),
                        items = activity.resources.getStringArray(R.array.indent).toList(),
                    ) { _, index ->
                        ReadBookConfig.paragraphIndent = "　".repeat(index)
                        postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
                    }
                }
                ChineseChip(
                    type = AppConfig.chineseConverterType,
                    contentColor = contentColor,
                    accent = accent,
                ) { type ->
                    AppConfig.chineseConverterType = type
                    ChineseUtils.unLoad(*TransType.entries.toTypedArray())
                    postEvent(EventBus.UP_CONFIG, arrayListOf(5))
                }
                ActionChip(stringResource(R.string.padding), contentColor) {
                    activity.showPaddingConfig()
                }
                ActionChip(stringResource(R.string.information), contentColor) {
                    TipConfigDialog().show(activity.supportFragmentManager, "tipConfigDialog")
                }
            }

            StyleSliderRow(
                title = stringResource(R.string.text_size),
                value = textSize,
                max = 45,
                display = { (it + 5).toString() },
                contentColor = contentColor,
                accent = accent,
                onChange = { v ->
                    textSize = v
                    ReadBookConfig.textSize = v + 5
                    postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
                },
            )
            StyleSliderRow(
                title = stringResource(R.string.text_letter_spacing),
                value = letterSpacing,
                max = 100,
                display = { ((it - 50) / 100f).toString() },
                contentColor = contentColor,
                accent = accent,
                onChange = { v ->
                    letterSpacing = v
                    ReadBookConfig.letterSpacing = (v - 50) / 100f
                    postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
                },
            )
            StyleSliderRow(
                title = stringResource(R.string.line_size),
                value = lineSpacing,
                max = 20,
                display = { ((it - 10) / 10f).toString() },
                contentColor = contentColor,
                accent = accent,
                onChange = { v ->
                    lineSpacing = v
                    ReadBookConfig.lineSpacingExtra = v
                    postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
                },
            )
            StyleSliderRow(
                title = stringResource(R.string.paragraph_size),
                value = paragraphSpacing,
                max = 20,
                display = { (it / 10f).toString() },
                contentColor = contentColor,
                accent = accent,
                onChange = { v ->
                    paragraphSpacing = v
                    ReadBookConfig.paragraphSpacing = v
                    postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
                },
            )

            SheetDivider(contentColor)

            BasicText(
                text = stringResource(R.string.page_anim),
                style = TextStyle(contentColor.copy(alpha = 0.75f), 12.sp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            val animLabels = listOf(
                stringResource(R.string.page_anim_cover),
                stringResource(R.string.page_anim_slide),
                stringResource(R.string.page_anim_simulation),
                stringResource(R.string.page_anim_scroll),
                stringResource(R.string.page_anim_none),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                animLabels.forEachIndexed { index, label ->
                    val selected = pageAnim == index
                    BasicText(
                        text = label,
                        style = TextStyle(
                            color = if (selected) Color.White else contentColor,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) accent else contentColor.copy(alpha = 0.08f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                pageAnim = index
                                ReadBook.book?.setPageAnim(-1)
                                ReadBookConfig.pageAnim = index
                                activity.window.decorView.post {
                                    activity.upPageAnim()
                                    ReadBook.loadContent(false)
                                }
                            }
                            .padding(vertical = 8.dp),
                    )
                }
            }

            SheetDivider(contentColor)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = stringResource(R.string.text_bg_style),
                    style = TextStyle(contentColor.copy(alpha = 0.75f), 12.sp),
                    modifier = Modifier.weight(1f),
                )
                BasicText(
                    text = stringResource(R.string.share_layout),
                    style = TextStyle(contentColor, 14.sp),
                )
                Spacer(Modifier.width(8.dp))
                // 简易开关（与 SmoothCheckBox 观感接近）
                Box(
                    Modifier
                        .size(width = 40.dp, height = 22.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (shareLayout) accent else contentColor.copy(alpha = 0.25f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            shareLayout = !shareLayout
                            ReadBookConfig.shareLayout = shareLayout
                            postEvent(EventBus.UP_CONFIG, arrayListOf(1, 2, 5))
                        },
                    contentAlignment = if (shareLayout) Alignment.CenterEnd else Alignment.CenterStart,
                ) {
                    Box(
                        Modifier
                            .padding(3.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            // ── 文字/背景样式列表 ──
            val configs = ReadBookConfig.configList
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                items(configs.size + 1) { index ->
                    if (index < configs.size) {
                        val item = configs[index]
                        val selected = styleSelect == index
                        StyleCircle(
                            label = item.name.ifBlank { "文字" },
                            textColor = Color(item.curTextColor()),
                            bgDrawable = { w, h -> item.curBgDrawable(w, h) },
                            selected = selected,
                            accent = accent,
                            tick = styleTick,
                            onClick = {
                                if (styleSelect != index) {
                                    val old = styleSelect
                                    styleSelect = index
                                    ReadBookConfig.styleSelect = index
                                    styleTick++
                                    postEvent(EventBus.UP_CONFIG, arrayListOf(1, 2, 5))
                                    if (AppConfig.readBarStyleFollowPage) {
                                        postEvent(EventBus.UPDATE_READ_ACTION_BAR, true)
                                    }
                                    // old unused; force recompose via tick
                                    @Suppress("UNUSED_EXPRESSION")
                                    old
                                }
                            },
                            onLongClick = {
                                styleSelect = index
                                ReadBookConfig.styleSelect = index
                                styleTick++
                                activity.showBgTextConfig()
                            },
                        )
                    } else {
                        // 新增
                        val c = Color(ReadBookConfig.durConfig.curTextColor())
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(1.dp, c, CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    ReadBookConfig.configList.add(ReadBookConfig.Config())
                                    val newIndex = ReadBookConfig.configList.lastIndex
                                    styleSelect = newIndex
                                    ReadBookConfig.styleSelect = newIndex
                                    styleTick++
                                    activity.showBgTextConfig()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = null,
                                tint = c,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { ReadBookConfig.save() }
    }
}

@Composable
private fun ActionChip(
    label: String,
    contentColor: Color,
    onClick: () -> Unit,
) {
    BasicText(
        text = label,
        style = TextStyle(contentColor, 14.sp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
    )
}

@Composable
private fun FontWeightRow(
    selected: Int,
    contentColor: Color,
    accent: Color,
    onSelect: (Int) -> Unit,
) {
    val parts = listOf("中", "粗", "细")
    Row {
        parts.forEachIndexed { index, s ->
            BasicText(
                text = s,
                style = TextStyle(
                    color = if (selected == index) accent else contentColor,
                    fontSize = 14.sp,
                    fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Normal,
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(index) }
                    .padding(horizontal = 3.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ChineseChip(
    type: Int,
    contentColor: Color,
    accent: Color,
    onSelect: (Int) -> Unit,
) {
    Row {
        listOf("简", "繁").forEachIndexed { i, s ->
            val idx = i + 1
            BasicText(
                text = s,
                style = TextStyle(
                    color = if (type == idx) accent else contentColor,
                    fontSize = 14.sp,
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(idx) }
                    .padding(horizontal = 3.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun StyleSliderRow(
    title: String,
    value: Int,
    max: Int,
    display: (Int) -> String,
    contentColor: Color,
    accent: Color,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = title,
            style = TextStyle(contentColor, 14.sp),
            modifier = Modifier.width(48.dp),
        )
        StepBtn("−", contentColor) { if (value > 0) onChange(value - 1) }
        Spacer(Modifier.width(4.dp))
        Box(
            Modifier
                .weight(1f)
                .height(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            // 玻璃滑杆：调用方已保证在 Compose 树内，无 AndroidView layout 写状态问题
            val contextBackdrop = GlassSliderBackdrop.current
            if (contextBackdrop != null) {
                LiquidSlider(
                    value = { value.toFloat() },
                    onValueChange = { v -> onChange(v.toInt().coerceIn(0, max)) },
                    valueRange = 0f..max.toFloat().coerceAtLeast(1f),
                    visibilityThreshold = 1f,
                    backdrop = contextBackdrop,
                    onValueChangeFinished = { onChange(value) },
                )
            } else {
                // 兜底细轨
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(contentColor.copy(alpha = 0.2f))
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        StepBtn("+", contentColor) { if (value < max) onChange(value + 1) }
        Spacer(Modifier.width(4.dp))
        BasicText(
            text = display(value),
            style = TextStyle(contentColor, 14.sp, textAlign = TextAlign.End),
            modifier = Modifier.width(48.dp),
        )
        // accent 未用则占位；保留签名对称
        @Suppress("UNUSED_EXPRESSION")
        accent
    }
}

@Composable
private fun StepBtn(
    text: String,
    contentColor: Color,
    onClick: () -> Unit,
) {
    BasicText(
        text = text,
        style = TextStyle(contentColor, 18.sp),
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun SheetDivider(contentColor: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(0.8.dp)
            .background(contentColor.copy(alpha = 0.15f))
    )
}

/**
 * 样式圆钮：预览用 AndroidView 画 CircleImageView（不写 Compose state，不会卡死）。
 */
@Composable
private fun StyleCircle(
    label: String,
    textColor: Color,
    bgDrawable: (Int, Int) -> android.graphics.drawable.Drawable,
    selected: Boolean,
    accent: Color,
    tick: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val sizePx = 48.dpToPx()
    AndroidView(
        factory = { ctx ->
            CircleImageView(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(sizePx, sizePx)
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                borderWidth = 1.dpToPx()
            }
        },
        update = { view ->
            view.borderColor = if (selected) accent.toArgbCompat() else textColor.toArgbCompat()
            view.setTextBold(selected)
            view.setText(label)
            view.setTextColor(textColor.toArgbCompat())
            view.setImageDrawable(bgDrawable(100, 150))
            view.setOnClickListener { onClick() }
            view.onLongClick { onLongClick() }
            // tick 变化时强制刷新
            view.tag = tick
        },
    )
}

private fun Color.toArgbCompat(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt(),
)

/** 供 StyleSliderRow 取 backdrop（由 ReadStyleGlassSheet 写入）。 */
object GlassSliderBackdrop {
    var current: Backdrop? = null
}
