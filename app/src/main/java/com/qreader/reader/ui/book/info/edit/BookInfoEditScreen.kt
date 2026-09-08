package com.qreader.reader.ui.book.info.edit

import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.qreader.reader.R
import com.qreader.reader.data.entities.Book
import com.qreader.reader.lib.theme.accentColor
import com.qreader.reader.lib.theme.backgroundColor
import com.qreader.reader.model.BookCover
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.widget.image.CoverImageView

/**
 * 书籍信息编辑界面（Compose 版）。
 *
 * 保留原 XML 布局的结构与交互：
 *  - 顶部玻璃标题栏：返回 + 标题 + 保存（替代原 TitleBar 与 menu_save）
 *  - 封面(90×130) + 书名 / 作者输入
 *  - 类型下拉（[R.array.book_type]：Text/Audio/Image/File/Video，position 4/2/1/其余 ↔ video/image/audio/text）
 *  - 封面 URL 输入 + 三个封面操作（选择本地图片 / 换源 / 刷新）
 *  - 简介输入
 *
 * 字段值由宿主 Activity 通过 [androidx.compose.runtime.mutableStateOf] 持有并传入，
 * 本组合只负责渲染与回调；状态栏图标颜色走 [GlassConfig] 单一真源（[GlassConfig.SyncStatusBarToGlassTheme]）。
 */
@Composable
fun BookInfoEditScreen(
    book: Book?,
    name: String,
    onNameChange: (String) -> Unit,
    author: String,
    onAuthorChange: (String) -> Unit,
    typePos: Int,
    onTypePosChange: (Int) -> Unit,
    coverUrl: String,
    onCoverUrlChange: (String) -> Unit,
    intro: String,
    onIntroChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onChangeCover: () -> Unit,
    onSelectCover: () -> Unit,
    onRefreshCover: () -> Unit,
) {
    val context = LocalContext.current

    // 玻璃主题（明暗单一真源）
    val isLightTheme = GlassConfig.isLightTheme(context)
    GlassConfig.SyncStatusBarToGlassTheme(isLightTheme = isLightTheme)

    val bgColor = Color(context.backgroundColor)
    val barContainerColor = GlassConfig.containerColor(isLightTheme)
    val barContentColor = GlassConfig.contentColor(isLightTheme)
    val textColor = colorResource(R.color.primaryText)
    val hintColor = colorResource(R.color.secondaryText)
    val accent = Color(context.accentColor)

    val backdrop = rememberLayerBackdrop()
    val density = LocalDensity.current
    val coverW = with(density) { 90.dp.toPx().toInt() }
    val coverH = with(density) { 130.dp.toPx().toInt() }
    val typeLabels = stringArrayResource(R.array.book_type)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // 玻璃 backdrop 捕获源：编辑表单内容（玻璃标题栏模糊/折射其上）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(5.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // ── 封面 + 书名 / 作者 ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // 封面（90×130，CENTER_CROP，沿用 CoverImageView + BookCover.load 保持 Glide 缓存与占位一致）
                    AndroidView(
                        factory = { ctx ->
                            CoverImageView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(coverW, coverH)
                                scaleType = ImageView.ScaleType.CENTER_CROP
                                setImageResource(R.drawable.image_cover_default)
                            }
                        },
                        update = { view ->
                            if (coverUrl.isNotBlank()) {
                                BookCover.load(
                                    view.context,
                                    coverUrl,
                                    false,
                                    book?.origin
                                ).into(view)
                            } else {
                                view.setImageResource(R.drawable.image_cover_default)
                            }
                        },
                        modifier = Modifier
                            .size(90.dp, 130.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.width(5.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(5.dp)
                    ) {
                        EditField(
                            value = name,
                            onValueChange = onNameChange,
                            label = stringResource(R.string.book_name),
                            textColor = textColor,
                            hintColor = hintColor,
                            accent = accent,
                            singleLine = true
                        )
                        Spacer(Modifier.height(5.dp))
                        EditField(
                            value = author,
                            onValueChange = onAuthorChange,
                            label = stringResource(R.string.author),
                            textColor = textColor,
                            hintColor = hintColor,
                            accent = accent,
                            singleLine = true
                        )
                    }
                }

                Spacer(Modifier.height(5.dp))
                // ── 类型 ──
                BasicText(
                    text = stringResource(R.string.book_type),
                    style = TextStyle(textColor, 14.sp),
                    modifier = Modifier.padding(start = 12.dp, end = 3.dp, bottom = 3.dp)
                )
                TypeDropdown(
                    labels = typeLabels,
                    selectedIndex = typePos,
                    onSelected = onTypePosChange,
                    textColor = textColor,
                    hintColor = hintColor
                )

                Spacer(Modifier.height(5.dp))
                // ── 封面 URL ──
                EditField(
                    value = coverUrl,
                    onValueChange = onCoverUrlChange,
                    label = stringResource(R.string.cover_path),
                    textColor = textColor,
                    hintColor = hintColor,
                    accent = accent,
                    singleLine = false
                )

                // ── 三个封面操作（原 StrokeTextView，保持 clickable + 边框视觉接近）──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp, vertical = 5.dp)
                ) {
                    ActionTextButton(
                        text = stringResource(R.string.select_local_image),
                        onClick = onSelectCover,
                        textColor = textColor
                    )
                    Spacer(Modifier.width(10.dp))
                    ActionTextButton(
                        text = stringResource(R.string.change_cover_source),
                        onClick = onChangeCover,
                        textColor = textColor
                    )
                    Spacer(Modifier.width(10.dp))
                    ActionTextButton(
                        text = stringResource(R.string.refresh_cover),
                        onClick = onRefreshCover,
                        textColor = textColor
                    )
                }

                Spacer(Modifier.height(5.dp))
                // ── 简介 ──
                EditField(
                    value = intro,
                    onValueChange = onIntroChange,
                    label = stringResource(R.string.book_intro),
                    textColor = textColor,
                    hintColor = hintColor,
                    accent = accent,
                    singleLine = false,
                    minHeight = 100.dp
                )
            }
        }

        // ── 玻璃标题栏（与 BookInfoScreen 同款：vibrancy + blur + lens + statusBarsPadding）──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(0.dp) },
                    effects = {
                        vibrancy()
                        blur(GlassConfig.blur.toPx())
                        lens(GlassConfig.lensX.toPx(), GlassConfig.lensY.toPx())
                    },
                    onDrawSurface = { drawRect(barContainerColor) }
                )
                .statusBarsPadding()
                .height(56.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = barContentColor
                    )
                }
                BasicText(
                    text = stringResource(R.string.book_info_edit),
                    style = TextStyle(barContentColor, 18.sp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )
                // 原 menu_save（ic_save）改为玻璃标题栏右侧图标按钮
                IconButton(onClick = onSave) {
                    Icon(
                        painter = painterResource(R.drawable.ic_save),
                        contentDescription = stringResource(R.string.action_save),
                        tint = barContentColor
                    )
                }
            }
        }
    }
}

/**
 * 可编辑字段：上方小标签 + 带边框输入框，空值时显示占位文字（沿用项目无 MaterialTheme 风格）。
 */
@Composable
private fun EditField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    textColor: Color,
    hintColor: Color,
    accent: Color,
    singleLine: Boolean,
    minHeight: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val borderColor = hintColor.copy(alpha = 0.5f)
    Column {
        BasicText(
            text = label,
            style = TextStyle(textColor.copy(alpha = 0.8f), 12.sp),
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (minHeight > 0.dp) Modifier.heightIn(min = minHeight) else Modifier)
                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            if (value.isEmpty()) {
                BasicText(
                    text = label,
                    style = TextStyle(hintColor, 14.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(textColor, 16.sp),
                cursorBrush = SolidColor(accent),
                singleLine = singleLine,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 类型下拉：点开显示 [R.array.book_type] 全部条目，点击回写到 selectedIndex。
 * 用 M3 DropdownMenu（项目内 MainScreen 等已用，无 MaterialTheme 包裹）。
 */
@Composable
private fun TypeDropdown(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    textColor: Color,
    hintColor: Color,
) {
    var expanded by remember { mutableStateOf(false) }
    val borderColor = hintColor.copy(alpha = 0.5f)
    Box {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = labels.getOrNull(selectedIndex) ?: "",
                style = TextStyle(textColor, 16.sp),
                modifier = Modifier.weight(1f)
            )
            BasicText(
                text = "▾",
                style = TextStyle(textColor, 16.sp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            labels.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { BasicText(label, style = TextStyle(textColor, 16.sp)) },
                    onClick = {
                        onSelected(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * 三个封面操作的描边文字按钮：保持原 StrokeTextView 的「可点击 + 边框」外观与点击交互。
 */
@Composable
private fun ActionTextButton(
    text: String,
    onClick: () -> Unit,
    textColor: Color,
) {
    BasicText(
        text = text,
        style = TextStyle(textColor, 14.sp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .border(1.dp, textColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 5.dp)
    )
}
