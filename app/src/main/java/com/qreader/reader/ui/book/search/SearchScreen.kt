package com.qreader.reader.ui.book.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import android.widget.FrameLayout
import com.qreader.reader.R
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.Book
import com.qreader.reader.data.entities.SearchBook
import com.qreader.reader.data.entities.SearchKeyword
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.lib.theme.backgroundColor
import com.qreader.reader.lib.theme.primaryColor
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.widget.LabelsBar
import com.qreader.reader.ui.widget.image.CoverImageView
import com.qreader.reader.ui.widget.text.BadgeView
import com.qreader.reader.utils.ColorUtils
import kotlinx.coroutines.flow.distinctUntilChanged
import splitties.init.appCtx

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBookClick: (String, String, String) -> Unit,
    onBookshelfBookClick: (Book) -> Unit,
    onSearchScopeClick: () -> Unit,
    onSourceManageClick: () -> Unit,
    onBack: () -> Unit,
    initialKey: String = "",
    modifier: Modifier = Modifier
) {
    val isSearching by viewModel.isSearchLiveData.observeAsState(false)
    val searchBooks by viewModel.searchBookLiveData.observeAsState(emptyList<SearchBook>())

    val context = LocalContext.current
    // 与 MainScreen 一致：以主色深浅判断明暗主题（仅用于玻璃容器色 / 标题栏前景色）
    val isLightTheme = ColorUtils.isColorLight(context.primaryColor)
    val primaryColor = Color(context.primaryColor)
    val bgColor = Color(context.backgroundColor)
    val containerColor = GlassConfig.containerColor(isLightTheme)
    val contentColor = if (isLightTheme) Color.Black else Color.White
    // 正文颜色一律走 legado 的日/夜资源色，不自己算 alpha
    val primaryTextColor = colorResource(R.color.primaryText)
    val focusManager = LocalFocusManager.current

    var query by remember { mutableStateOf(initialKey) }
    var showInputHelp by remember { mutableStateOf(initialKey.isBlank()) }
    var historyKeywords by remember { mutableStateOf(emptyList<SearchKeyword>()) }
    var matchedBooks by remember { mutableStateOf(emptyList<Book>()) }
    val backdrop = rememberLayerBackdrop()

    fun doSearch(key: String) {
        val trimmed = key.trim()
        if (trimmed.isNotEmpty()) {
            viewModel.saveSearchKey(trimmed)
            viewModel.searchKey = ""
            viewModel.search(trimmed)
            showInputHelp = false
            focusManager.clearFocus()
        }
    }

    // 收集搜索历史
    LaunchedEffect(query) {
        if (query.isBlank()) {
            appDb.searchKeywordDao.flowByTime().collect { historyKeywords = it }
        } else {
            appDb.searchKeywordDao.flowSearch(query).collect { historyKeywords = it }
        }
    }

    // 收集书架匹配
    LaunchedEffect(query) {
        if (query.isBlank()) {
            matchedBooks = emptyList()
        } else {
            appDb.bookDao.flowSearch(query).collect { matchedBooks = it }
        }
    }

    // 滚动到底部自动加载更多
    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .distinctUntilChanged()
            .collect { layoutInfo ->
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
                if (lastVisible != null && lastVisible.index >= layoutInfo.totalItemsCount - 3) {
                    if (!isSearching && viewModel.searchKey.isNotEmpty() && viewModel.hasMore) {
                        viewModel.search("")
                    }
                }
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // ── 内容层（作为玻璃态采样源）──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .layerBackdrop(backdrop)
        ) {
            // 为悬浮玻璃标题栏留空间
            Spacer(modifier = Modifier.height(100.dp))

            // 搜索进度条
            AnimatedVisibility(
                visible = isSearching,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = primaryColor
                )
            }

            // 输入帮助（搜索历史 + 书架匹配）或搜索结果
            if (showInputHelp) {
                InputHelpContent(
                    historyKeywords = historyKeywords,
                    matchedBooks = matchedBooks,
                    onHistoryClick = { keyword ->
                        query = keyword
                        doSearch(keyword)
                    },
                    onHistoryDelete = { keyword -> viewModel.deleteHistory(keyword) },
                    onClearHistory = { viewModel.clearHistory() },
                    onBookClick = { book -> onBookshelfBookClick(book) },
                    isLightTheme = isLightTheme,
                    backdrop = backdrop,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    if (searchBooks.isEmpty() && !isSearching) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = primaryTextColor
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(
                                items = searchBooks,
                                key = { "${it.name}-${it.author}-${it.bookUrl}" }
                            ) { searchBook ->
                                SearchBookItem(
                                    searchBook = searchBook,
                                    isInBookshelf = viewModel.isInBookShelf(searchBook),
                                    onClick = {
                                        onBookClick(searchBook.name, searchBook.author, searchBook.bookUrl)
                                    }
                                )
                            }

                            if (isSearching) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = primaryColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 玻璃态标题栏（悬浮在顶部，不占内容流）──
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
                    onDrawSurface = { drawRect(containerColor) }
                )
                .height(100.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                // 返回箭头
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = contentColor,
                    )
                }

                // 胶囊搜索框（液态玻璃）
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedCornerShape(50) },
                            effects = {
                                vibrancy()
                                blur(GlassConfig.blur.toPx())
                                lens(GlassConfig.lensX.toPx(), GlassConfig.lensY.toPx())
                            },
                            onDrawSurface = { drawRect(containerColor.copy(alpha = 0.6f)) }
                        )
                        .height(40.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { newQuery ->
                            query = newQuery
                            viewModel.stop()
                            showInputHelp = true
                        },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(contentColor, 15.sp),
                        cursorBrush = SolidColor(contentColor),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { doSearch(query) }),
                        decorationBox = { innerTextField ->
                            Box {
                                if (query.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.search_book_key),
                                        style = TextStyle(contentColor.copy(alpha = 0.5f), 15.sp)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    // 提交箭头（有输入时显示）
                    if (query.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { doSearch(query) }
                        )
                    }
                }

                // 三点菜单
                IconButton(onClick = onSearchScopeClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = contentColor,
                    )
                }
            }
        }

        // 开始/停止 FAB
        AnimatedVisibility(
            visible = !showInputHelp && (isSearching || (viewModel.hasMore && viewModel.searchKey.isNotEmpty())),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            androidx.compose.material3.FloatingActionButton(
                onClick = {
                    if (isSearching) viewModel.stop() else viewModel.search("")
                },
                containerColor = primaryColor
            ) {
                Icon(
                    imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (isSearching) stringResource(R.string.stop) else "继续搜索"
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InputHelpContent(
    historyKeywords: List<SearchKeyword>,
    matchedBooks: List<Book>,
    onHistoryClick: (String) -> Unit,
    onHistoryDelete: (SearchKeyword) -> Unit,
    onClearHistory: () -> Unit,
    onBookClick: (Book) -> Unit,
    isLightTheme: Boolean,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // 书架匹配（对应 legado ll_input_help 里的 tv_book_show + rv_bookshelf_search）
        if (matchedBooks.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.bookshelf),
                    fontSize = 14.sp,
                    color = colorResource(R.color.primaryText),
                    modifier = Modifier.padding(6.dp)
                )
            }
            item {
                FlowRow(modifier = Modifier.padding(horizontal = 3.dp)) {
                    matchedBooks.forEach { book ->
                        FilletText(
                            text = book.name,
                            onClick = { onBookClick(book) }
                        )
                    }
                }
            }
        }

        // 搜索历史（对应 legado 固定的「搜索历史 / 清除」标题行 + 可滚动 rv_history_key）
        if (historyKeywords.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.searchHistory),
                        fontSize = 14.sp,
                        color = colorResource(R.color.primaryText),
                        modifier = Modifier
                            .weight(1f)
                            .padding(6.dp)
                    )
                    GlassClearButton(
                        onClick = onClearHistory,
                        backdrop = backdrop,
                        isLightTheme = isLightTheme,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }
            item {
                FlowRow(modifier = Modifier.padding(horizontal = 3.dp)) {
                    historyKeywords.forEach { keyword ->
                        FilletText(
                            text = keyword.word,
                            onClick = { onHistoryClick(keyword.word) },
                            onLongClick = { onHistoryDelete(keyword) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 玻璃态「清除」按钮。
 *
 * 基础样式对齐 legado 的 `activity_book_search.xml#tv_clear_history`：
 * 文字走 `primaryText`、内边距 6dp、点击有水波纹（selectableItemBackground）。
 * 玻璃部分复用页面主 [Backdrop]，用与发现页/标题栏同一套
 * `drawBackdrop + blur + lens + GlassConfig.containerColor` 实现。
 */
@Composable
private fun GlassClearButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    isLightTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(16.dp) },
                effects = {
                    vibrancy()
                    blur(GlassConfig.blur.toPx())
                    lens(GlassConfig.lensX.toPx(), GlassConfig.lensY.toPx())
                },
                onDrawSurface = { drawRect(GlassConfig.containerColor(isLightTheme)) }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.clear),
            color = colorResource(R.color.primaryText),
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * 圆角标签，对齐 legado `item_fillet_text.xml`：
 * 外边距 3dp、内边距 上下4dp/左右12dp、14sp、`primaryText` 文字色、
 * 背景 `selector_fillet_btn_bg`（16dp 圆角 + btn_bg_press）。
 * 点击搜索，长按删除（对齐 HistoryKeyAdapter 的点击/长按语义）。
 */
@Composable
private fun FilletText(
    text: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Text(
        text = text,
        color = colorResource(R.color.primaryText),
        fontSize = 14.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(3.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colorResource(R.color.btn_bg_press))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

/**
 * 搜索结果条目，对齐 legado `item_search.xml` + `SearchAdapter.bind()`：
 * 封面 80×110（CoverImageView centerCrop）| 已在书架 8dp 绿点 | 书名 16sp |
 * 右上角 BadgeView 源数量 | 作者 / 分类 LabelsBar / 最新章节 / 简介 均 12sp。
 */
@Composable
private fun SearchBookItem(
    searchBook: SearchBook,
    isInBookshelf: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val coverWidth = with(density) { 80.dp.roundToPx() }
    val coverHeight = with(density) { 110.dp.roundToPx() }
    val kinds = remember(searchBook.kind) { searchBook.getKindList() }
    val originCount = searchBook.origins.size

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // 封面（iv_cover）
        AndroidView(
            factory = { ctx ->
                CoverImageView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(coverWidth, coverHeight)
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    setImageResource(R.drawable.image_cover_default)
                }
            },
            update = { view -> view.load(searchBook, AppConfig.loadCoverOnlyWifi) },
            modifier = Modifier.size(80.dp, 110.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, top = 3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 已在书架（iv_in_bookshelf：8dp 绿点）
                if (isInBookshelf) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(colorResource(R.color.md_green_600), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = searchBook.name,
                    color = colorResource(R.color.primaryText),
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // 源数量（bv_originCount，count<=0 时 BadgeView 自动隐藏）
                AndroidView(
                    factory = { ctx -> BadgeView(ctx).apply { setBadgeCount(originCount) } },
                    update = { view -> view.setBadgeCount(originCount) }
                )
            }

            // 作者（tv_author）
            Text(
                text = stringResource(R.string.author_show, searchBook.author),
                color = colorResource(R.color.primaryText),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 分类（ll_kind）
            if (kinds.isNotEmpty()) {
                AndroidView(
                    factory = { ctx -> LabelsBar(ctx).apply { setLabels(kinds) } },
                    update = { view -> view.setLabels(kinds) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 最新章节（tv_lasted）
            val latestChapter = searchBook.latestChapterTitle
            if (!latestChapter.isNullOrEmpty()) {
                Text(
                    text = stringResource(R.string.lasted_show, latestChapter),
                    color = colorResource(R.color.primaryText),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 简介（tv_introduce）
            if (!searchBook.intro.isNullOrEmpty()) {
                Text(
                    text = searchBook.trimIntro(appCtx),
                    color = colorResource(R.color.primaryText),
                    fontSize = 12.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
