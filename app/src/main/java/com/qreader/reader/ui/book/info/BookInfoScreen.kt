package com.qreader.reader.ui.book.info

import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.qreader.reader.model.BookCover
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.widget.LabelsBar
import com.qreader.reader.ui.widget.image.CoverImageView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 书籍信息 Compose 页面。
 *
 * 替代原 activity_book_info.xml 布局，保留 VMBaseActivity 宿主。
 * WebView / CoverImageView / LabelsBar 等自定义控件通过 AndroidView 包装。
 *
 * @param book             书籍对象（用于封面加载等）
 * @param bookName         书名
 * @param author           作者显示文案（含前缀）
 * @param origin           来源显示文案（含前缀）
 * @param latestChapter    最新章节显示文案（含前缀）
 * @param groupText        分组显示文案（含前缀）
 * @param tocText          目录显示文案（含前缀）
 * @param tocVisible       目录行是否可见
 * @param shelfText        书架按钮文案
 * @param introContainer   简介容器（FrameLayout，由 Activity 管理 WebView/TextView 切换）
 * @param kinds            标签列表
 * @param onCoverClick     封面点击
 * @param onCoverLongClick 封面长按
 * @param onReadClick      阅读按钮点击
 * @param onShelfClick     书架按钮点击
 * @param onOriginClick    来源点击
 * @param onChangeSource    换源点击
 * @param onTocClick       目录点击
 * @param onGroupChange    换分组点击
 * @param onNameClick      书名点击
 * @param onNameLongClick  书名长按
 * @param onAuthorClick    作者点击
 * @param onAuthorLongClick 作者长按
 * @param onLabelClick     标签点击（用于搜索）
 * @param onLabelLongClick 标签长按（用于 SourceCallBack）
 * @param onBack           返回按钮点击
 * @param onRefresh        下拉刷新
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookInfoScreen(
    book: Book?,
    bookName: String,
    author: String,
    origin: String,
    latestChapter: String,
    groupText: String,
    tocText: String,
    tocVisible: Boolean,
    shelfText: String,
    introContainer: FrameLayout?,
    kinds: List<String>,
    onCoverClick: () -> Unit,
    onCoverLongClick: () -> Unit,
    onReadClick: () -> Unit,
    onShelfClick: () -> Unit,
    onOriginClick: () -> Unit,
    onChangeSource: () -> Unit,
    onTocClick: () -> Unit,
    onGroupChange: () -> Unit,
    onNameClick: () -> Unit,
    onNameLongClick: (() -> Unit)?,
    onAuthorClick: () -> Unit,
    onAuthorLongClick: (() -> Unit)?,
    onLabelClick: ((String) -> Unit)?,
    onLabelLongClick: ((String) -> Unit)?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    deleteDialogOpen: Boolean = false,
    deleteDialogShowCheckBox: Boolean = false,
    deleteDialogCheckBoxChecked: Boolean = false,
    onDeleteDialogConfirm: (Boolean) -> Unit = {},
    onDeleteDialogCancel: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // 标题栏玻璃明暗跟随背景（与 MainScreen / SearchScreen 一致：以 backgroundColor 判定而非主色）
    val isBookInfoLightTheme = GlassConfig.isLightTheme(context)
    val barContainerColor = GlassConfig.containerColor(isBookInfoLightTheme)
    val barContentColor = GlassConfig.contentColor(isBookInfoLightTheme)
    val bgColor = Color(context.getColor(R.color.background))
    val bottomBg = Color(context.getColor(R.color.background_menu))
    val textColor = Color(context.getColor(R.color.primaryText))
    val summaryColor = Color(context.getColor(R.color.tv_text_summary))
    var isRefreshing by remember { mutableStateOf(false) }
    val backdrop = rememberLayerBackdrop()
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        // ── 模糊背景（全屏，仿 legado bg_book: match_parent × match_parent + centerCrop）──
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setImageResource(R.drawable.image_cover_default)
                }
            },
            update = { view ->
                book?.let { b ->
                    BookCover.loadBlur(view.context, b.getDisplayCover(), false, b.origin)
                        .into(view)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        // 半透明遮罩（仿 legado vw_bg: #50000000，即 31% 黑色）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x50000000))
        )

        // ── 背景内容层（作为玻璃态采样源）──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            // ── 可滚动内容区（支持拉动刷新）──
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    coroutineScope.launch {
                        isRefreshing = true
                        onRefresh()
                        // 等待至少 1 秒让刷新动画显示，但不超过 5 秒
                        delay(1000L)
                        isRefreshing = false
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 70.dp), // 为标题栏留空间，刷新指示器在此之下
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val viewportHeight = maxHeight
                    val density = LocalDensity.current
                    var contentHeight by remember { mutableStateOf(0.dp) }
                    var introHeight by remember { mutableStateOf(0.dp) }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // ── 封面 + 信息整体区域 ──
                        // 注意：在 verticalScroll 内 fillMaxSize 拿到的是无限高度约束，
                        // 不会填满视口，必须用 onSizeChanged 实测内容高度再补底。
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { contentHeight = with(density) { it.height.toDp() } }
                                .padding(top = 40.dp), // 微调与模糊背景的间距
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                        // 封面
                        AndroidView(
                            factory = { ctx ->
                                CoverImageView(ctx).apply {
                                    layoutParams = FrameLayout.LayoutParams(
                                        110.dpToPx(ctx), 160.dpToPx(ctx)
                                    )
                                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                                    setImageResource(R.drawable.image_cover_default)
                                    setOnClickListener { onCoverClick() }
                                    setOnLongClickListener { onCoverLongClick(); true }
                                }
                            },
                            update = { view ->
                                book?.let { view.load(it) }
                            },
                            modifier = Modifier
                                .size(110.dp, 160.dp)
                                .clip(RoundedCornerShape(5.dp))
                        )

                        // 弧形装饰（仿 legado ArcView, arcDirectionTop=true, arcHeight=36dp）
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                        ) {
                            val w = size.width
                            val h = size.height
                            drawPath(
                                path = Path().apply {
                                    moveTo(0f, h)
                                    quadraticBezierTo(w / 2f, 0f, w, h)
                                    close()
                                },
                                color = bgColor
                            )
                        }

                        // ── 信息区 ──
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bgColor)
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        // 书名
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = bookName,
                                style = TextStyle(textColor, 18.sp, FontWeight.Medium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onNameClick,
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }

                        // 标签（kinds）
                        if (kinds.isNotEmpty()) {
                            AndroidView(
                                factory = { ctx ->
                                    LabelsBar(ctx).apply {
                                        setLabels(
                                            kinds,
                                            onLabelClick?.let { click ->
                                                { kind -> click(kind) }
                                            },
                                            onLabelLongClick?.let { longClick ->
                                                { kind -> longClick(kind); true }
                                            }
                                        )
                                    }
                                },
                                update = { view ->
                                    view.setLabels(
                                        kinds,
                                        onLabelClick?.let { click ->
                                            { kind -> click(kind) }
                                        },
                                        onLabelLongClick?.let { longClick ->
                                            { kind -> longClick(kind); true }
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 作者行
                        InfoRow(
                            iconRes = R.drawable.ic_author,
                            text = author,
                            summaryColor = summaryColor,
                            onClick = onAuthorClick,
                        )

                        // 来源行
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_web_outline),
                                contentDescription = null,
                                modifier = Modifier.size(18.sp.value.dp),
                                colorFilter = ColorFilter.tint(summaryColor),
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            BasicText(
                                text = origin,
                                style = TextStyle(summaryColor, 13.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onOriginClick,
                                    ),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AccentButton(
                                text = stringResource(R.string.change_origin),
                                onClick = onChangeSource,
                            )
                        }

                        // 最新章节行
                        InfoRow(
                            iconRes = R.drawable.ic_book_last,
                            text = latestChapter,
                            summaryColor = summaryColor,
                        )

                        // 分组行
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_groups),
                                contentDescription = null,
                                modifier = Modifier.size(18.sp.value.dp),
                                colorFilter = ColorFilter.tint(summaryColor),
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            BasicText(
                                text = groupText,
                                style = TextStyle(summaryColor, 13.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AccentButton(
                                text = stringResource(R.string.change_group),
                                onClick = onGroupChange,
                            )
                        }

                        // 目录行
                        if (tocVisible) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_folder_open),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.sp.value.dp),
                                    colorFilter = ColorFilter.tint(summaryColor),
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                BasicText(
                                    text = tocText,
                                    style = TextStyle(summaryColor, 13.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                AccentButton(
                                    text = stringResource(R.string.view_toc),
                                    onClick = onTocClick,
                                )
                            }
                        }
                    }
                    }

                    // ── 简介区域 ──
                    if (introContainer != null) {
                        AndroidView(
                            factory = { introContainer },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { introHeight = with(density) { it.height.toDp() } }
                                .background(bgColor)
                                .padding(horizontal = 8.dp),
                        )
                    }

                    // ── 底部填充：内容不足一屏时用背景色补满剩余空间，遮住模糊背景 ──
                    // contentHeight 为封面+信息区实测高度（含顶部 40dp padding），introHeight 为简介区实测高度
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                (viewportHeight - contentHeight - introHeight)
                                    .coerceAtLeast(0.dp)
                            )
                            .background(bgColor)
                    )
                }
                }
            }

            // ── 底部分割线 + 操作栏 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(context.getColor(R.color.bg_divider_line)))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bottomBg)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.Center,
            ) {
                // 书架按钮
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onShelfClick,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = shelfText,
                        style = TextStyle(textColor, 15.sp),
                    )
                }
                // 阅读按钮
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(context.accentColor))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onReadClick,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = stringResource(R.string.reading),
                        style = TextStyle(Color.White, 15.sp),
                    )
                }
            }
        }

        // ── 玻璃态标题栏（悬浮在顶部）──
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
                .height(56.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = barContentColor,
                    )
                }
                BasicText(
                    text = stringResource(R.string.book_info),
                    style = TextStyle(barContentColor, 18.sp),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        // ── 删除确认弹框 ──
        if (deleteDialogOpen) {
            DeleteBookDialogOverlay(
                backdrop = backdrop,
                showCheckBox = deleteDialogShowCheckBox,
                checkBoxText = if (deleteDialogShowCheckBox) stringResource(R.string.delete_book_file) else "",
                checkBoxChecked = deleteDialogCheckBoxChecked,
                onConfirm = onDeleteDialogConfirm,
                onCancel = onDeleteDialogCancel,
            )
        }
    }
}

/**
 * 通用信息行（图标 + 单行文字）
 */
@Composable
private fun InfoRow(
    iconRes: Int,
    text: String,
    summaryColor: Color,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .let { mod ->
                if (onClick != null) {
                    mod.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                } else mod
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.sp.value.dp),
            colorFilter = ColorFilter.tint(summaryColor),
        )
        Spacer(modifier = Modifier.width(2.dp))
        BasicText(
            text = text,
            style = TextStyle(summaryColor, 13.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 强调色小按钮（换源 / 换分组 / 查看目录）
 */
@Composable
private fun AccentButton(text: String, onClick: () -> Unit) {
    val context = LocalContext.current
    BasicText(
        text = text,
        style = TextStyle(Color(context.accentColor), 13.sp),
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(Color(context.accentColor).copy(alpha = 0.1f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

private fun Int.dpToPx(context: android.content.Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}
