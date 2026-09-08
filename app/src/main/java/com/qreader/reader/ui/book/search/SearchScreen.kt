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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
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
import android.widget.FrameLayout
import com.qreader.reader.R
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.Book
import com.qreader.reader.data.entities.SearchBook
import com.qreader.reader.data.entities.SearchKeyword
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.lib.theme.accentColor
import com.qreader.reader.lib.theme.backgroundColor
import com.qreader.reader.lib.theme.primaryColor
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.widget.LabelsBar
import com.qreader.reader.ui.widget.image.CoverImageView
import com.qreader.reader.ui.widget.text.BadgeView
import com.qreader.reader.utils.ColorUtils
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import com.qreader.reader.constant.PreferKey
import com.qreader.reader.utils.getPrefBoolean
import com.qreader.reader.utils.putPrefBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val accentColor = Color(context.accentColor)
    val bgColor = Color(context.backgroundColor)
    val containerColor = GlassConfig.containerColor(isLightTheme)
    val contentColor = if (isLightTheme) Color.Black else Color.White
    // 正文颜色一律走 legado 的日/夜资源色，不自己算 alpha
    val primaryTextColor = colorResource(R.color.primaryText)
    val focusManager = LocalFocusManager.current

    var query by remember { mutableStateOf(initialKey) }
    var showInputHelp by remember { mutableStateOf(initialKey.isBlank()) }
    // 是否为用户手动停止（对齐 legado SearchActivity.isManualStopSearch：
    // 手动停止后不再显示「继续搜索」，自然结束且还有更多时才显示）
    var manualStopSearch by remember { mutableStateOf(false) }
    var historyKeywords by remember { mutableStateOf(emptyList<SearchKeyword>()) }
    var matchedBooks by remember { mutableStateOf(emptyList<Book>()) }
    // 搜索结果为空弹窗（对齐 legado searchFinishLiveData 观察）
    var showEmptyDialog by remember { mutableStateOf(false) }
    val searchFinishEmpty by viewModel.searchFinishLiveData.observeAsState(false)
    val backdrop = rememberLayerBackdrop()
    val scope = rememberCoroutineScope()

    fun doSearch(key: String) {
        val trimmed = key.trim()
        if (trimmed.isNotEmpty()) {
            manualStopSearch = false
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

    // 滚动到底部自动加载更多（对齐 legado SearchActivity.scrollToBottom：
    // 手动停止后不再自动加载）
    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .distinctUntilChanged()
            .collect { layoutInfo ->
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
                if (lastVisible != null && lastVisible.index >= layoutInfo.totalItemsCount - 3) {
                    if (!manualStopSearch && !isSearching &&
                        viewModel.searchKey.isNotEmpty() && viewModel.hasMore
                    ) {
                        viewModel.search("")
                    }
                }
            }
    }

    // 搜索结果为空提示（对齐 legado observeLiveBus：搜索结束为空且非全部分组时弹窗）
    LaunchedEffect(searchFinishEmpty) {
        if (searchFinishEmpty && !viewModel.searchScope.isAll()) {
            showEmptyDialog = true
        }
    }

    // 搜索范围变化后自动重搜（对齐 legado searchScope.stateLiveData 观察：
    // 输入帮助隐藏时把当前 query 重新提交，触发换源搜索）
    val searchScope by viewModel.searchScope.stateLiveData.observeAsState(viewModel.searchScope.toString())
    var firstScopeEmit by remember { mutableStateOf(true) }
    LaunchedEffect(searchScope) {
        if (firstScopeEmit) {
            firstScopeEmit = false
            return@LaunchedEffect
        }
        if (!showInputHelp && query.isNotBlank()) {
            doSearch(query)
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
                        // 对齐 legado SearchActivity.searchHistory：点历史关键词时，
                        // 若该书已在书架则只回填不重搜（直接呈现「已知书架」匹配），
                        // 否则（或当前 query 已是该词）才发起网络搜索。
                        scope.launch {
                            val inShelf = withContext(Dispatchers.IO) {
                                appDb.bookDao.findByName(keyword).isNotEmpty()
                            }
                            if (query == keyword || !inShelf) {
                                query = keyword
                                doSearch(keyword)
                            } else {
                                query = keyword
                                showInputHelp = true
                            }
                        }
                    },
                    onHistoryDelete = { keyword -> viewModel.deleteHistory(keyword) },
                    onClearHistory = { viewModel.clearHistory() },
                    onBookClick = { book -> onBookshelfBookClick(book) },
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

        // 开始/停止 FAB（对齐 legado activity_book_search.xml#fb_start_stop：
        // fabSize=mini、底色 accentColor、搜索中 ic_stop_black_24dp / 可继续 ic_play_24dp、
        // 手动停止后隐藏，自然结束且还有更多时显示「继续」）
        AnimatedVisibility(
            visible = !showInputHelp && (isSearching ||
                (!manualStopSearch && viewModel.hasMore && viewModel.searchKey.isNotEmpty())),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SmallFloatingActionButton(
                onClick = {
                    if (isSearching) {
                        manualStopSearch = true
                        viewModel.stop()
                    } else {
                        manualStopSearch = false
                        viewModel.search("")
                    }
                },
                containerColor = accentColor
            ) {
                Icon(
                    painter = painterResource(
                        if (isSearching) R.drawable.ic_stop_black_24dp else R.drawable.ic_play_24dp
                    ),
                    contentDescription = stringResource(R.string.stop),
                    // 与 legado 的 setImageResource 一致：不做 tint，保留图标自身配色
                    tint = Color.Unspecified
                )
            }
        }

        // 搜索结果为空弹窗（对齐 legado observeLiveBus：
        // 精准搜索分组为空 → 提示关闭精准搜索；普通分组为空 → 提示切换到全部分组）
        if (showEmptyDialog) {
            AlertDialog(
                onDismissRequest = { showEmptyDialog = false },
                title = { Text(text = "搜索结果为空", color = primaryTextColor) },
                text = {
                    val precision = appCtx.getPrefBoolean(PreferKey.precisionSearch)
                    Text(
                        text = if (precision)
                            "${viewModel.searchScope.display}分组搜索结果为空，是否关闭精准搜索？"
                        else
                            "${viewModel.searchScope.display}分组搜索结果为空，是否切换到全部分组？",
                        color = primaryTextColor
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showEmptyDialog = false
                        if (appCtx.getPrefBoolean(PreferKey.precisionSearch)) {
                            appCtx.putPrefBoolean(PreferKey.precisionSearch, false)
                            viewModel.searchKey = ""
                            viewModel.search(query)
                        } else {
                            viewModel.searchScope.update("")
                        }
                    }) {
                        Text(text = stringResource(R.string.sure), color = primaryTextColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyDialog = false }) {
                        Text(text = stringResource(R.string.cancel), color = primaryTextColor)
                    }
                }
            )
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
                    ClearHistoryButton(
                        onClick = onClearHistory,
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
 * 「清除」按钮，对齐 legado 的 `activity_book_search.xml#tv_clear_history`：
 * 普通文字（`primaryText`）+ 内边距 6dp + 点击水波纹（`selectableItemBackground`）。
 *
 * ⚠️ 这里**不能**加 `drawBackdrop`：本按钮位于内容层内部，而内容层就是
 * `layerBackdrop(backdrop)` 的捕获层，在捕获层内对同一个 backdrop 调用 drawBackdrop
 * 会循环捕获导致进入页面即崩溃（与排序弹框当初的坑同源）。
 * 玻璃效果只适用于捕获层之外的元素（标题栏 / 全屏弹框）。
 */
@Composable
private fun ClearHistoryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.clear),
        color = colorResource(R.color.primaryText),
        fontSize = 14.sp,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
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
