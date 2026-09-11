package com.qreader.reader.ui.book.toc

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.qreader.reader.R
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.BookChapter
import com.qreader.reader.data.entities.Bookmark
import com.qreader.reader.help.book.BookHelp
import com.qreader.reader.help.book.ContentProcessor
import com.qreader.reader.help.book.isLocal
import com.qreader.reader.help.book.isLocalTxt
import com.qreader.reader.help.book.simulatedTotalChapterNum
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.help.config.ReadBookConfig
import com.qreader.reader.lib.theme.accentColor
import com.qreader.reader.model.ReadBook
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.GlassDropdownMenu
import com.qreader.reader.utils.ColorUtils
import com.qreader.reader.utils.getCompatColor
import com.qreader.reader.utils.toastOnUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 顶部导航栏玻璃高度（从屏幕顶边算起，把状态栏一起包住）。
 *
 * 与全 App 标题栏统一为 100dp。注意这个高度是**含状态栏**的：顶栏内部自己
 * `statusBarsPadding()` 把按钮行推离状态栏，玻璃底色则一直铺到屏幕顶边，
 * 这样状态栏背后和顶栏是同一块玻璃，不会出现断层色带
 * （对齐 MainScreen.BookshelfGlassTitleBar 的 `.height(100.dp).statusBarsPadding()`）。
 */
private val TocTopBarHeight = 100.dp

/** 目录页数据聚合（章节 + 预计算的显示标题 + 本地已缓存文件名集合）。 */
private data class TocData(
    val chapters: List<BookChapter>,
    val titles: Map<String, String>,
    val cache: Set<String>,
)

/**
 * 目录玻璃页（阅读页 in-tree，全屏）。
 *
 * 对齐 legado 原版 TocActivity（activity_chapter_list + fragment_chapter_list + item_chapter_list）：
 *  - 顶部玻璃导航栏：返回 / 「目录|书签」Tab（选中态 accentColor 下划线）/ 搜索 / 更多
 *  - 中部内容：目录 LazyColumn（当前章 accentColor 高亮、VIP 锁、tag、字数、缓存/已读图标、卷名底色）
 *              或书签列表（点按跳章、长按编辑）
 *  - 底部章节信息栏：当前章「标题(93/447)」+ 置顶 / 置底（对齐 ll_chapter_base_info）
 *
 * 玻璃全部用 [drawBackdrop] 采样宿主传入的 [backdrop]（阅读页正文捕获层），因此本组件必须
 * 渲染在 `layerBackdrop` 捕获层之外；返回键由内部 [BackHandler] 接管。
 *
 * @param onDismiss          关闭目录页（返回键 / 顶栏返回）
 * @param onOpenChapter      点击章节 / 书签 → 跳到指定章，由 Activity 执行
 * @param onDismissToMainMenu 「更多 → 主菜单」入口；默认等同 [onDismiss]
 */
@Composable
fun TocGlassSheet(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    onOpenChapter: (index: Int, chapterPos: Int) -> Unit,
    onDismissToMainMenu: () -> Unit = onDismiss,
) {
    val ctx = LocalContext.current
    val book = ReadBook.book ?: return
    val bookUrl = book.bookUrl
    val durIndex = ReadBook.durChapterIndex
    val total = book.simulatedTotalChapterNum()
    val durTitle = book.durChapterTitle ?: ""

    // 玻璃色：跟随「书页背景」判定明暗（阅读页特有不依赖 App 主题）
    val isLightPage = ColorUtils.isColorLight(ReadBookConfig.bgMeanColor)
    val containerColor = GlassConfig.containerColor(isLightPage)
    val contentColor = GlassConfig.contentColor(isLightPage)

    // 列表色：对齐内容层目录页（原版 item_chapter_list 用主题色，不用玻璃反色）
    val primaryText = Color(ctx.getCompatColor(R.color.primaryText))
    val secondaryText = Color(ctx.getCompatColor(R.color.secondaryText))
    val accent = Color(ctx.accentColor)
    val volumeBg = Color(ctx.getCompatColor(R.color.btn_bg_press))

    val isLocal = book.isLocal
    val isLocalTxt = book.isLocalTxt

    var tab by remember { mutableIntStateOf(0) }
    var searchActive by remember { mutableStateOf(false) }
    var searchKey by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var pendingBookmark by remember { mutableStateOf<Bookmark?>(null) }
    var pendingBookmarkPos by remember { mutableIntStateOf(-1) }

    // 两个设置开关提升为本地状态：改完立即重组（AppConfig 不是 Compose 可观察的）
    var useReplace by remember { mutableStateOf(AppConfig.tocUiUseReplace) }
    var showWordCount by remember { mutableStateOf(AppConfig.tocCountWords) }

    val chapterListState = rememberLazyListState()
    val bookmarkListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    BackHandler {
        when {
            pendingBookmark != null -> {
                pendingBookmark = null
                pendingBookmarkPos = -1
            }

            menuOpen -> menuOpen = false

            searchActive -> {
                searchActive = false
                searchKey = ""
            }

            else -> onDismiss()
        }
    }

    // 章节数据（跟随搜索词过滤）
    // displayTitle 在 IO 线程预计算（对齐原版 ChapterListAdapter.upDisplayTitles，
    // getDisplayTitle 内部会跑正则替换，放 Compose 渲染里会掉帧）
    val tocData by produceState(TocData(emptyList(), emptyMap(), emptySet()), bookUrl, searchKey, useReplace) {
        val end = (book.simulatedTotalChapterNum() - 1).coerceAtLeast(0)
        val chineseConvert = AppConfig.chineseConverterType != 0
        val (chs, titles) = withContext(Dispatchers.IO) {
            val list = if (searchKey.isBlank()) {
                appDb.bookChapterDao.getChapterList(bookUrl, 0, end)
            } else {
                appDb.bookChapterDao.search(bookUrl, searchKey, 0, end)
            }
            val rules = if (useReplace) {
                ContentProcessor.get(book.name, book.origin).getTitleReplaceRules()
            } else {
                null
            }
            val replaceBook = book.toReplaceBook()
            val map = list.associate { ch ->
                ch.url to ch.getDisplayTitle(
                    replaceRules = rules,
                    useReplace = useReplace && book.getUseReplaceRule(),
                    chineseConvert = chineseConvert,
                    replaceBook = replaceBook,
                )
            }
            list to map
        }
        val cached = withContext(Dispatchers.IO) { BookHelp.getChapterFiles(book).toHashSet() }
        value = TocData(chs, titles, cached)
    }

    // 书签数据（跟随搜索词过滤）
    val bookmarks by produceState<List<Bookmark>>(emptyList(), book.name, book.author, searchKey) {
        val list = withContext(Dispatchers.IO) {
            if (searchKey.isBlank()) {
                appDb.bookmarkDao.getByBook(book.name, book.author)
            } else {
                appDb.bookmarkDao.search(book.name, book.author, searchKey)
            }
        }
        value = list
    }

    // 首屏滚动到当前章（对齐 ChapterListFragment.onListChanged）
    LaunchedEffect(tocData.chapters, durIndex, searchKey) {
        if (searchKey.isNotBlank()) return@LaunchedEffect
        val pos = tocData.chapters.indexOfFirst { it.index >= durIndex }
        if (pos >= 0) chapterListState.scrollToItem(pos)
    }

    Box(Modifier.fillMaxSize()) {
        // ── 全屏玻璃底板 ──
        Box(
            Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(0.dp) },
                    effects = {
                        vibrancy()
                        blur(GlassConfig.blur.toPx())
                        lens(GlassConfig.lensX.toPx(), GlassConfig.lensY.toPx())
                    },
                    onDrawSurface = { drawRect(containerColor) },
                )
        )

        // 内容层**不加** statusBarsPadding：顶栏玻璃要一直延伸到屏幕顶边与状态栏连成一片。
        // 顶栏自己吃掉状态栏高度（见 TocTopBar 内部的 statusBarsPadding），
        // 这样状态栏背后也是同一块玻璃，不会出现「状态栏一条、顶栏另一条」的断层。
        Column(
            Modifier.fillMaxSize()
        ) {
            TocTopBar(
                backdrop = backdrop,
                tab = tab,
                onTabChange = {
                    if (tab != it) {
                        tab = it
                        searchActive = false
                        searchKey = ""
                    }
                },
                contentColor = contentColor,
                accent = accent,
                searchActive = searchActive,
                searchKey = searchKey,
                onSearchKeyChange = { searchKey = it },
                onSearchToggle = {
                    searchActive = !searchActive
                    if (!searchActive) searchKey = ""
                },
                onMenuClick = { menuOpen = true },
                onBack = onDismiss,
            )

            // ── 内容区 ──
            Box(Modifier.weight(1f)) {
                if (tab == 0) {
                    ChapterList(
                        chapters = tocData.chapters,
                        titles = tocData.titles,
                        cache = tocData.cache,
                        durIndex = durIndex,
                        isLocal = isLocal,
                        showWordCount = showWordCount,
                        accent = accent,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        volumeBg = volumeBg,
                        listState = chapterListState,
                        onOpenChapter = onOpenChapter,
                    )
                } else {
                    BookmarkList(
                        bookmarks = bookmarks,
                        accent = accent,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        listState = bookmarkListState,
                        onClick = { bm -> onOpenChapter(bm.chapterIndex, bm.chapterPos) },
                        onLongClick = { bm, pos ->
                            pendingBookmark = bm
                            pendingBookmarkPos = pos
                        },
                    )
                }
            }

            // ── 底部章节信息栏（对齐 ll_chapter_base_info）──
            TocBottomBar(
                backdrop = backdrop,
                durTitle = durTitle,
                durIndex = durIndex,
                total = total,
                contentColor = primaryText,
                onInfoClick = {
                    if (tab == 0) {
                        val pos = tocData.chapters.indexOfFirst { it.index >= durIndex }
                        if (pos >= 0) scope.launch { chapterListState.scrollToItem(pos) }
                    }
                },
                onTop = {
                    if (tab == 0) {
                        scope.launch { chapterListState.scrollToItem(0) }
                    } else {
                        scope.launch { bookmarkListState.scrollToItem(0) }
                    }
                },
                onBottom = {
                    if (tab == 0) {
                        val last = tocData.chapters.lastIndex
                        if (last >= 0) scope.launch { chapterListState.scrollToItem(last) }
                    } else {
                        val last = bookmarks.lastIndex
                        if (last >= 0) scope.launch { bookmarkListState.scrollToItem(last) }
                    }
                },
            )
        }

        // ── 更多下拉菜单（必须挂全屏层，否则 tap-outside 失效）──
        GlassDropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            backdrop = backdrop,
            containerColor = containerColor,
            contentColor = contentColor,
        ) {
            if (tab == 0) {
                if (isLocalTxt) {
                    TocMenuItem(
                        text = ctx.getString(R.string.txt_toc_rule),
                        contentColor = contentColor,
                        onClick = { menuOpen = false; ctx.toastOnUi("TXT 目录规则请在书籍详情操作") },
                    )
                }
                TocMenuItem(
                    text = ctx.getString(R.string.reverse_toc),
                    contentColor = contentColor,
                    onClick = { menuOpen = false; ctx.toastOnUi("反转目录请在书籍详情操作") },
                )
                TocMenuItem(
                    text = ctx.getString(R.string.use_replace),
                    contentColor = contentColor,
                    checked = useReplace,
                    onClick = {
                        useReplace = !useReplace
                        AppConfig.tocUiUseReplace = useReplace
                    },
                )
                TocMenuItem(
                    text = ctx.getString(R.string.load_word_count),
                    contentColor = contentColor,
                    checked = showWordCount,
                    onClick = {
                        showWordCount = !showWordCount
                        AppConfig.tocCountWords = showWordCount
                    },
                )
            } else {
                TocMenuItem(
                    text = ctx.getString(R.string.export),
                    contentColor = contentColor,
                    onClick = { menuOpen = false; ctx.toastOnUi("导出书签请在独立目录页操作") },
                )
                TocMenuItem(
                    text = ctx.getString(R.string.export_md),
                    contentColor = contentColor,
                    onClick = { menuOpen = false; ctx.toastOnUi("导出 MD 请在独立目录页操作") },
                )
            }
            TocMenuItem(
                text = ctx.getString(R.string.back),
                contentColor = contentColor,
                onClick = {
                    menuOpen = false
                    onDismissToMainMenu()
                },
            )
        }

        // ── 书签编辑（复用阅读页书签玻璃面板）──
        pendingBookmark?.let { bm ->
            com.qreader.reader.ui.book.read.config.BookmarkGlassSheet(
                backdrop = backdrop,
                bookmark = bm,
                editPos = pendingBookmarkPos,
                onDismiss = {
                    pendingBookmark = null
                    pendingBookmarkPos = -1
                },
            )
        }
    }
}

/**
 * 顶部玻璃导航栏：返回 / Tab「目录|书签」/ 搜索 / 更多。
 *
 * 与全 App 标题栏一致的关键点是**玻璃一直延伸到屏幕顶边**、把状态栏也包进来，
 * 状态栏背后和顶栏是同一块玻璃（对齐 MainScreen / SearchScreen 顶栏 / ReadMenuOverlay 的做法），
 * 而不是「状态栏一条 + 顶栏另一条」的色带断层。
 *
 * 高度安排（对齐 MainScreen.BookshelfGlassTitleBar）：
 *  - 玻璃本体 `.height(100.dp)`，从屏幕顶边起算；
 *  - 内部先 `statusBarsPadding()` 把内容推离状态栏，再往下排按钮行；
 *  - 按钮行底部对齐（`Alignment.Bottom` + `bottom = 12.dp`），与其它页标题栏同款。
 *
 * 搜索展开时就地把「目录|书签」Tab 替换为输入框（对齐原版 SearchView 展开时 tabLayout.gone()）。
 */
@Composable
private fun TocTopBar(
    backdrop: Backdrop,
    tab: Int,
    onTabChange: (Int) -> Unit,
    contentColor: Color,
    accent: Color,
    searchActive: Boolean,
    searchKey: String,
    onSearchKeyChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onMenuClick: () -> Unit,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val containerColor = GlassConfig.containerColor(
        ColorUtils.isColorLight(ReadBookConfig.bgMeanColor)
    )

    Column(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(0.dp) },
                effects = {
                    vibrancy()
                    blur(GlassConfig.blur.toPx())
                    lens(GlassConfig.lensX.toPx(), GlassConfig.lensY.toPx())
                },
                onDrawSurface = { drawRect(containerColor) },
            )
            // 玻璃整体从屏幕顶边起算 100dp，把状态栏一起包进来
            .height(TocTopBarHeight)
            // 内容让开状态栏；玻璃本身仍铺满状态栏区域
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            // 返回
            TocIconButton(
                iconRes = R.drawable.ic_arrow_back,
                contentDescription = ctx.getString(R.string.back),
                contentColor = contentColor,
                onClick = onBack,
            )

            // Tab 或搜索框
            if (searchActive) {
                // 搜索框：对齐 SearchScreen 的「胶囊玻璃 + 放大镜 + 自动聚焦」范式。
                // 原先只有一个裸 BasicTextField，无焦点/无光标/无键盘、视觉上近乎不可见，
                // 用户点完搜索图标会觉得「没反应」。
                val focusRequester = remember { FocusRequester() }
                val keyboard = LocalSoftwareKeyboardController.current
                LaunchedEffect(Unit) {
                    // 等一帧让输入框挂进视图树再请求焦点，否则 requestFocus 无效
                    kotlinx.coroutines.delay(80)
                    focusRequester.requestFocus()
                    keyboard?.show()
                }
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
                            onDrawSurface = { drawRect(containerColor.copy(alpha = GlassConfig.glassButtonSurfaceAlpha)) },
                        )
                        .height(40.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = searchKey,
                        onValueChange = onSearchKeyChange,
                        singleLine = true,
                        textStyle = TextStyle(contentColor, 16.sp),
                        cursorBrush = SolidColor(accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (searchKey.isEmpty()) {
                                    BasicText(
                                        text = ctx.getString(R.string.search),
                                        style = TextStyle(contentColor.copy(alpha = 0.5f), 16.sp),
                                    )
                                }
                                inner()
                            }
                        },
                    )
                    // 清除（有输入时显示）
                    if (searchKey.isNotEmpty()) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = ctx.getString(R.string.clear),
                            tint = contentColor.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onSearchKeyChange("") },
                                ),
                        )
                    }
                }
            } else {
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    TocTabItem(
                        text = ctx.getString(R.string.chapter_list),
                        selected = tab == 0,
                        contentColor = contentColor,
                        accent = accent,
                        onClick = { onTabChange(0) },
                    )
                    Spacer(Modifier.width(24.dp))
                    TocTabItem(
                        text = ctx.getString(R.string.bookmark),
                        selected = tab == 1,
                        contentColor = contentColor,
                        accent = accent,
                        onClick = { onTabChange(1) },
                    )
                }
            }

            // 搜索
            TocIconButton(
                iconRes = R.drawable.ic_search,
                contentDescription = ctx.getString(R.string.search),
                contentColor = contentColor,
                onClick = onSearchToggle,
            )

            // 更多
            TocIconButton(
                iconRes = R.drawable.ic_more_vert,
                contentDescription = "more",
                contentColor = contentColor,
                onClick = onMenuClick,
            )
        }
    }
}

/**
 * 顶栏图标按钮：裸图标 + 40dp 触摸区，**不加**玻璃框。
 *
 * 顶栏本身已经是一整块玻璃，按钮再套一层 40dp 玻璃块会在玻璃上叠出可见的方块边界，
 * 视觉上像贴了三个按钮底板。这里保持纯图标（与阅读页 ReadMenuOverlay 顶栏返回键同款），
 * 只有图标本身，触摸区靠 40dp 保证。
 */
@Composable
private fun TocIconButton(
    iconRes: Int,
    contentDescription: String,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = contentDescription,
        tint = contentColor,
        modifier = Modifier
            .size(40.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(8.dp),
    )
}

/**
 * 目录页下拉菜单项：文字 + 可选勾选标记（对齐原版 MenuItem 的 checkable 项）。
 *
 * 不复用 [GlassDropdownMenuItem]：它的 `checked` 为 null 时右侧留空，而本页需要在
 * 同一菜单里混排「可勾选项」与「纯动作项」，这里包一层让调用点更直观。
 */
@Composable
private fun TocMenuItem(
    text: String,
    contentColor: Color,
    onClick: () -> Unit,
    checked: Boolean? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = text,
            style = TextStyle(contentColor, 15.sp),
            modifier = Modifier.weight(1f),
        )
        if (checked != null) {
            BasicText(
                text = if (checked) "✓" else "",
                style = TextStyle(contentColor, 15.sp),
            )
        }
    }
}

/** 顶栏 Tab 项：选中态加粗 + 底部 accentColor 指示条（对齐 TabLayout indicator）。 */
@Composable
private fun TocTabItem(
    text: String,
    selected: Boolean,
    contentColor: Color,
    accent: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            ),
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .width(20.dp)
                .height(2.dp)
                .background(if (selected) accent else Color.Transparent)
        )
    }
}

/**
 * 底部章节信息栏：当前章信息 + 置顶 / 置底。
 *
 * 对齐原版 ll_chapter_base_info（36dp 行高、12sp、左右 10dp）。
 */
@Composable
private fun TocBottomBar(
    backdrop: Backdrop,
    durTitle: String,
    durIndex: Int,
    total: Int,
    contentColor: Color,
    onInfoClick: () -> Unit,
    onTop: () -> Unit,
    onBottom: () -> Unit,
) {
    val ctx = LocalContext.current
    val containerColor = GlassConfig.containerColor(
        ColorUtils.isColorLight(ReadBookConfig.bgMeanColor)
    )

    Row(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(0.dp) },
                effects = {
                    vibrancy()
                    blur(GlassConfig.blur.toPx())
                    lens(GlassConfig.lensX.toPx(), GlassConfig.lensY.toPx())
                },
                onDrawSurface = { drawRect(containerColor) },
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 10.dp)
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = "$durTitle(${durIndex + 1}/$total)",
            style = TextStyle(contentColor, 12.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onInfoClick,
                )
                .padding(horizontal = 10.dp),
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow_drop_up),
            contentDescription = ctx.getString(R.string.go_to_top),
            tint = contentColor,
            modifier = Modifier
                .size(36.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTop,
                )
                .padding(7.dp),
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow_drop_down),
            contentDescription = ctx.getString(R.string.go_to_bottom),
            tint = contentColor,
            modifier = Modifier
                .size(36.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBottom,
                )
                .padding(7.dp),
        )
    }
}

/**
 * 章节列表（对齐 item_chapter_list）：
 *  - 当前章 accentColor 高亮
 *  - VIP 锁（isVip && !isPay）
 *  - tag（更新时间）/ 字数（tocCountWords）
 *  - 右侧：当前章 ic_check；未缓存章节 ic_outline_cloud_24
 *  - 卷名：btn_bg_press 底色
 */
@Composable
private fun ChapterList(
    chapters: List<BookChapter>,
    titles: Map<String, String>,
    cache: Set<String>,
    durIndex: Int,
    isLocal: Boolean,
    showWordCount: Boolean,
    accent: Color,
    primaryText: Color,
    secondaryText: Color,
    volumeBg: Color,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onOpenChapter: (Int, Int) -> Unit,
) {
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(chapters, key = { it.url + it.index }) { ch ->
            val isDur = ch.index == durIndex
            val cached = isLocal || ch.isVolume || cache.contains(ch.getFileName())
            val bg = if (ch.isVolume) volumeBg else Color.Transparent
            val titleColor = if (isDur) accent else primaryText
            val tag = ch.tag
            val wc = ch.wordCount
            val showTagLine = !tag.isNullOrEmpty() ||
                (showWordCount && !wc.isNullOrEmpty() && !ch.isVolume)

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(bg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onOpenChapter(ch.index, 0) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // VIP 锁
                if (ch.isVip && !ch.isPay) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock_outline),
                        contentDescription = null,
                        tint = secondaryText,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp),
                    )
                }

                Column(Modifier.weight(1f)) {
                    BasicText(
                        text = titles[ch.url] ?: ch.title,
                        style = TextStyle(
                            color = titleColor,
                            fontSize = 15.sp,
                            fontWeight = if (isDur) FontWeight.Medium else FontWeight.Normal,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (showTagLine) {
                        Spacer(Modifier.height(4.dp))
                        Row {
                            if (!tag.isNullOrEmpty()) {
                                BasicText(
                                    text = tag,
                                    style = TextStyle(secondaryText, 12.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (showWordCount && !wc.isNullOrEmpty() && !ch.isVolume) {
                                if (!tag.isNullOrEmpty()) Spacer(Modifier.width(8.dp))
                                BasicText(
                                    text = wc,
                                    style = TextStyle(secondaryText, 12.sp),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                // 右侧图标：当前章 = ✓；未缓存 = 云
                if (isDur) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(4.dp),
                    )
                } else if (!cached) {
                    Icon(
                        painter = painterResource(R.drawable.ic_outline_cloud_24),
                        contentDescription = null,
                        tint = secondaryText,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(4.dp),
                    )
                }
            }
        }
    }
}

/**
 * 书签列表（对齐 item_bookmark）：章节名 + 正文片段 + 备注。
 */
@Composable
private fun BookmarkList(
    bookmarks: List<Bookmark>,
    accent: Color,
    primaryText: Color,
    secondaryText: Color,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onClick: (Bookmark) -> Unit,
    onLongClick: (Bookmark, Int) -> Unit,
) {
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(bookmarks, key = { it.time }) { bm ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onClick(bm) }
                    .padding(8.dp),
            ) {
                BasicText(
                    text = bm.chapterName,
                    style = TextStyle(primaryText, 14.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(4.dp),
                )
                if (bm.bookText.isNotEmpty()) {
                    BasicText(
                        text = bm.bookText,
                        style = TextStyle(secondaryText, 12.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(4.dp),
                    )
                }
                if (bm.content.isNotEmpty()) {
                    BasicText(
                        text = bm.content,
                        style = TextStyle(accent, 12.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(4.dp),
                    )
                }
            }
        }
    }
}
