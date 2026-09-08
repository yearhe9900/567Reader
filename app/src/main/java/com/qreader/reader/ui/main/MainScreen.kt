package com.qreader.reader.ui.main

import android.app.Activity
import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.qreader.reader.R
import com.qreader.reader.data.appDb
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.BookGroup
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.ui.book.group.GroupEditOverlay
import com.qreader.reader.ui.book.search.SearchActivity
import com.qreader.reader.lib.theme.accentColor
import com.qreader.reader.lib.theme.backgroundColor
import com.qreader.reader.lib.theme.primaryColor
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.GlassDropdownMenu
import com.qreader.reader.ui.compose.glass.GlassDropdownMenuItem
import com.qreader.reader.ui.compose.liquid.LiquidBottomTab
import com.qreader.reader.ui.compose.liquid.LiquidBottomTabs
import com.qreader.reader.utils.ColorUtils
import androidx.core.view.WindowCompat
import kotlin.math.abs

/**
 * 主界�? —�? Compose 实现（整页迁移）
 *
 * 页面结构�?
 *  - 底层主题背景（作�? Liquid Glass backdrop 的捕获源�?
 *  - 内容区：HorizontalPager（书�?/发现/设置三个 Compose 页面�?
 *  - 底部导航栏：LiquidBottomTabs 玻璃态胶囊栏（EInk 墨水屏模式降级为纯色栏）
 *
 * 三个页面均为 Compose 函数，backdrop 可直接捕获滚动内容，玻璃折射效果自然作用于真实内容�?
 *
 * @param selectedTabIndex       当前选中 tab 的读取函�?
 * @param onTabSelected          切换到指�? position 的回�?
 * @param badgeCount             书架 tab 的角标数字（待更新书籍数），0 表示不显�?
 * @param showDiscovery          是否显示「发现」tab（对应页面数 2 �? 3�?
 * @param isEInkMode             是否墨水屏模式，�? true 时导航栏降级为纯色栏
 * @param bookshelfPage          书架页面 composable（接收注�? gotoTop / back 回调�?
 * @param explorePage            发现页面 composable（接收注�? compressExplore 回调�?
 * @param settingsPage           设置页面 composable
 * @param themeDialogOpen        主题模式弹框是否打开（打开时隐藏底部导航栏�?
 * @param onThemeDialogOpenChange 主题模式弹框打开状态变化回调（由设置页内弹框上抛）
 * @param registerBookshelfBack  �? MainActivity 暴露书架 back 回调（供返回键使用）
 */
@Composable
fun MainScreen(
    selectedTabIndex: () -> Int,
    onTabSelected: (Int) -> Unit,
    badgeCount: Int,
    showDiscovery: Boolean,
    isEInkMode: Boolean,
    bookshelfPage: @Composable (
        registerGotoTop: ((() -> Unit)?) -> Unit,
        registerBack: ((() -> Boolean)?) -> Unit,
        registerMenuAction: (((BookshelfMenuAction) -> Unit)?) -> Unit,
        onRequestGroupEdit: (BookGroup) -> Unit,
        bookshelfSort: Int,
        onRequestSort: () -> Unit,
        currentGroupId: Long,
    ) -> Unit,
    explorePage: @Composable (
        registerCompress: ((() -> Unit)?) -> Unit,
        searchQuery: String,
        onSearchQueryChange: (String) -> Unit,
        backdrop: Backdrop,
    ) -> Unit,
    settingsPage: @Composable () -> Unit,
    themeDialogOpen: Boolean,
    onThemeDialogOpenChange: (Boolean) -> Unit,
    registerBookshelfBack: ((() -> Boolean)?) -> Unit,
) {
    val context = LocalContext.current

    // 玻璃与文字的明暗跟随「背景」而非主色：玻璃浮在背景之上，且主色通常是用户选的强调�?
    // （亮色背�? + 深色主色是常态），若按主色判断会把浅背景误判为暗色，玻璃永远走深�? =
    // 用户看到的「浅灰色」。改�? backgroundColor 判断后，浅背�? �? 玻璃白、文字黑�?
    val isLightTheme = GlassConfig.isLightTheme(context)
    val accentColor = Color(context.accentColor)
    val contentColor = GlassConfig.contentColor(isLightTheme)
    val containerColor = GlassConfig.containerColor(isLightTheme)
    val bgColor = Color(context.backgroundColor)

    // 状态栏图标颜色跟随玻璃主题（单一真相源 GlassConfig.isLightTheme）：
    // 亮色玻璃顶栏背景接近白 → 需要深色图标（APPEARANCE_LIGHT_STATUS_BARS=true）；
    // 暗色玻璃顶栏背景接近黑 → 浅色图标。BaseActivity.setupSystemBar 对透明状态栏误把背景当成
    // 深色（Color.TRANSPARENT 的 RGB 为 0 → isColorLight=false → 白图标），在亮色模式下白图标
    // 落在接近白的玻璃上不可见（电池/信号被「遮住」）。此处按实际玻璃背景纠正，且随主题切换重组同步。
    SideEffect {
        (context as? Activity)?.window?.let { win ->
            WindowCompat.getInsetsController(win, win.decorView)
                .isAppearanceLightStatusBars = isLightTheme
        }
    }

    // 玻璃导航栏的 backdrop 捕获源（捕获真实页面内容，供 lens 折射 / blur 作用其上�?
    val backdrop = rememberLayerBackdrop()

    // 编辑分组玻璃弹框状态（仅书架页长按入口触发；其�? XML 入口仍用�? GroupEditDialog�?
    var groupEditTarget by remember { mutableStateOf<BookGroup?>(null) }
    var groupEditOpen by remember { mutableStateOf(false) }

    // 排序玻璃弹框状态（排序值提升到 MainScreen，供 SortDialogOverlay 选择后回�? BookshelfPage�?
    var bookshelfSort by remember { mutableIntStateOf(AppConfig.bookshelfSort) }
    var sortDialogOpen by remember { mutableStateOf(false) }

    // 分组抽屉玻璃弹框状态（标题栏按钮触发）
    var groupDrawerOpen by remember { mutableStateOf(false) }
    var currentGroupId by remember { mutableLongStateOf(BookGroup.IdAll) }
    var currentGroupName by remember { mutableStateOf<String?>(null) }

    val tabItems = remember(showDiscovery, context) {
        buildTabItems(context, showDiscovery)
    }

    // ── 页面动作（由各页�? composable 注册）──
    var bookshelfGotoTop by remember { mutableStateOf<(() -> Unit)?>(null) }
    var bookshelfBack by remember { mutableStateOf<(() -> Boolean)?>(null) }
    var bookshelfMenuAction by remember { mutableStateOf<((BookshelfMenuAction) -> Unit)?>(null) }
    var bookshelfMenuOpen by remember { mutableStateOf(false) }
    var exploreCompress by remember { mutableStateOf<(() -> Unit)?>(null) }

    // ── 发现页搜索状态（提升�? MainScreen 供玻璃标题栏使用）──
    var exploreSearchQuery by remember { mutableStateOf("") }

    // ── 重选计时（双击触发 gotoTop / compressExplore）──
    var bookshelfReselected by remember { mutableLongStateOf(0L) }
    var exploreReselected by remember { mutableLongStateOf(0L) }

    // ── HorizontalPager ──
    val pageCount = tabItems.size
    val pagerState = rememberPagerState(initialPage = selectedTabIndex()) { pageCount }

    // 同步 pager �? onTabSelected（用户滑动翻页时通知 MainActivity�?
    LaunchedEffect(pagerState.settledPage) {
        onTabSelected(pagerState.settledPage)
    }

    // 兜底：翻页后强制复位主题弹框开关�?
    //
    // 「主题模式」弹框由设置页（Pager 内的某一页）承载，�? themeDialogOpen 状态由上层持有�?
    // 且标题栏/导航栏的显隐�? AnimatedVisibility(visible = !themeDialogOpen)�?
    // 一旦在弹框打开时发生翻页，该页会被 HorizontalPager 销�? �? 弹框随之消失�?
    // 但开关状态不会自动复位，标题栏与导航栏就会被永久隐藏。此处确保任何翻页都复位开关�?
    LaunchedEffect(pagerState.currentPage) {
        if (themeDialogOpen) onThemeDialogOpenChange(false)
    }

    // 用户是否正在用手指拖�? pager�?
    // 兜底保护：确保任何程序化滚动都不会在用户手势进行中插手�?
    // （真正的回环已在 LiquidBottomTabs 侧切断，此处防止其他时序下的边界情况。）
    // 注意：它只覆盖「手指按住」阶段，松手后的 fling 惯性阶段为 false，故不能作为唯一手段�?
    val isUserDragging by pagerState.interactionSource.collectIsDraggedAsState()

    // 程序化滚动期间禁�? backdrop GPU 效果，避�? animateScrollToPage �? backdrop 离屏渲染双重 GPU 压力
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    // 跨页切换�?0�?2）时整页的淡入进度�?
    // 只在 draw 阶段�? graphicsLayer 读取，因此动画期间只重绘、不触发重组�?
    val pageFade = remember { Animatable(1f) }

    // 同步 selectedTabIndex �? pager（外部设�? selectedTab 时切换到对应页）
    //
    // 分两种策略：相邻页做滑动动画，跨页瞬�? + 淡入�?
    //
    // 之所以跨页不滑动：HorizontalPager �? 0 �? 2 必然经过中间的发现页(1)�?
    // 用户反馈「快速掠过发现页」观感不好，因此改为瞬跳，并用淡入补足过渡感�?
    // （此前「跨页卡在发现页」是导航栏回环把目标页改写成�? 1，已在上游切断，
    //   与本处的瞬跳选择无关——瞬跳纯粹是为了观感，不�? workaround。）
    // 回环链路备忘：跨页动画过中点 �? currentPage=1 �? LiquidBottomTabs 反向回调
    // onTabSelected(1) �? selectedTab 被改写为 1 �? �? LaunchedEffect �? key 变化
    // �? 旧动画被取消 �? 页面停在发现页�?

    LaunchedEffect(selectedTabIndex()) {
        if (isUserDragging) return@LaunchedEffect // 手势滑动中不干预，交给用�?
        val target = selectedTabIndex()
        if (pagerState.settledPage == target) return@LaunchedEffect
        isProgrammaticScroll = true
        try {
            // 所有点击切换统一瞬跳 + 淡入，避�? animateScrollToPage 的程序驱动滚动卡�?
            pagerState.scrollToPage(target)
            pageFade.snapTo(CROSS_PAGE_FADE_START)
            pageFade.animateTo(
                1f,
                tween(durationMillis = 160, easing = FastOutLinearInEasing)
            )
        } finally {
            isProgrammaticScroll = false
        }
    }

    // Tab 点击处理（含双击重选逻辑�?
    fun handleTabClick(position: Int) {
        if (position == selectedTabIndex()) {
            when (position) {
                0 -> {
                    if (System.currentTimeMillis() - bookshelfReselected > 300) {
                        bookshelfReselected = System.currentTimeMillis()
                    } else {
                        bookshelfGotoTop?.invoke()
                    }
                }

                1 -> if (showDiscovery) {
                    if (System.currentTimeMillis() - exploreReselected > 300) {
                        exploreReselected = System.currentTimeMillis()
                    } else {
                        exploreCompress?.invoke()
                    }
                }
            }
        } else {
            onTabSelected(position)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 玻璃 backdrop 捕获源：底色 + 真实页面内容（导航栏玻璃折射/模糊作用其上�?
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .then(if (!isProgrammaticScroll) Modifier.layerBackdrop(backdrop) else Modifier)
        ) {
            // 内容区：HorizontalPager 全屏，延伸到状态栏和导航栏背后
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = pageFade.value },
                beyondViewportPageCount = 1, // 预组合相�? 1 页，平衡内存与切换流畅度
            ) { page ->
                when (page) {
                    0 -> bookshelfPage(
                        { bookshelfGotoTop = it },
                        { back ->
                            bookshelfBack = back
                            registerBookshelfBack(back)
                        },
                        { bookshelfMenuAction = it },
                        { group -> groupEditTarget = group; groupEditOpen = true },
                        bookshelfSort,
                        { sortDialogOpen = true },
                        currentGroupId,
                    )

                    1 -> if (showDiscovery) {
                        explorePage(
                            { exploreCompress = it },
                            exploreSearchQuery,
                            { exploreSearchQuery = it },
                            backdrop,
                        )
                    } else {
                        settingsPage()
                    }

                    2 -> settingsPage()
                }
            }
        }

        // 书架更多选项玻璃下拉：放在标题栏 Box 之前，使其位于标题栏之下�?
        // 标题栏点击不�? tap-outside 拦截，菜单按钮可正常切换开关�?
        if (pagerState.currentPage == 0) {
            GlassDropdownMenu(
                expanded = bookshelfMenuOpen,
                onDismissRequest = { bookshelfMenuOpen = false },
                backdrop = backdrop,
                containerColor = containerColor,
                contentColor = contentColor,
            ) {
                enumValues<BookshelfMenuAction>().forEach { action ->
                    GlassDropdownMenuItem(
                        text = context.getString(action.titleRes),
                        iconRes = action.iconRes,
                        contentColor = contentColor,
                        onClick = {
                            bookshelfMenuOpen = false
                            bookshelfMenuAction?.invoke(action)
                        },
                    )
                }
            }
        }

        // 玻璃标题栏：悬浮在顶部（overlay，不占内容流�?
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            AnimatedVisibility(
                visible = !themeDialogOpen,
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(120))
            ) {
                val title = when (pagerState.currentPage) {
                    0 -> currentGroupName ?: context.getString(R.string.bookshelf)
                    1 -> if (showDiscovery) context.getString(R.string.discovery) else context.getString(R.string.setting)
                    2 -> context.getString(R.string.setting)
                    else -> ""
                }
                if (isEInkMode) {
                    EInkTitleBar(
                        title = title,
                        bgColor = bgColor,
                        contentColor = contentColor,
                    )
                } else if (pagerState.currentPage == 0) {
                    BookshelfGlassTitleBar(
                        backdrop = backdrop,
                        title = title,
                        containerColor = containerColor,
                        contentColor = contentColor,
                        onSearch = { SearchActivity.start(context, "") },
                        onGroups = { groupDrawerOpen = true },
                        menuOpen = bookshelfMenuOpen,
                        onMenuAction = { bookshelfMenuAction?.invoke(it) },
                        onMenuOpenChange = { bookshelfMenuOpen = it },
                    )
                } else if (pagerState.currentPage == 1 && showDiscovery) {
                    ExploreGlassTitleBar(
                        backdrop = backdrop,
                        searchQuery = exploreSearchQuery,
                        onSearchQueryChange = { exploreSearchQuery = it },
                        containerColor = containerColor,
                        contentColor = contentColor,
                    )
                } else {
                    GlassTitleBar(
                        backdrop = backdrop,
                        title = title,
                        containerColor = containerColor,
                        contentColor = contentColor,
                    )
                }
            }
        }

        // 底部导航栏：悬浮透明胶囊栏（overlay，不占内容流�?
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
        ) {
            AnimatedVisibility(
                visible = !themeDialogOpen,
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(120))
            ) {
                if (isEInkMode) {
                    EInkBottomBar(
                        tabItems = tabItems,
                        selectedTabIndex = { pagerState.currentPage },
                        onTabSelected = { handleTabClick(it) },
                        badgeCount = badgeCount,
                        bgColor = bgColor,
                        accentColor = accentColor,
                    )
                } else {
                    LiquidBottomTabs(
                        selectedTabIndex = { pagerState.currentPage },
                        onTabSelected = { handleTabClick(it) },
                        backdrop = backdrop,
                        tabsCount = tabItems.size,
                        accentColor = accentColor,
                        containerColor = containerColor,
                        isLightTheme = isLightTheme,
                    ) {
                        tabItems.forEach { item ->
                            LiquidBottomTab(
                                onClick = { handleTabClick(item.position) }
                            ) {
                                TabIconWithBadge(
                                    item = item,
                                    selected = item.position == pagerState.currentPage,
                                    accentColor = accentColor,
                                    contentColor = contentColor,
                                    badgeCount = if (item.position == 0) badgeCount else 0
                                )
                                BasicText(
                                    text = item.label,
                                    style = TextStyle(
                                        color = if (item.position == pagerState.currentPage) {
                                            accentColor
                                        } else {
                                            contentColor
                                        },
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    // 编辑分组玻璃弹框覆盖层（真·毛玻璃，采样真实书架页�?
    // 注意：必须作为「全屏外�? Box」的直接子节点，不能嵌套进底部导航栏那种 align+fillMaxWidth 的小 Box�?
    // 否则 fillMaxSize 至多只填满底部条，蒙板只会盖住一小块区域�?
    AnimatedVisibility(
        visible = groupEditOpen,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(160)),
        exit = fadeOut(tween(120))
    ) {
        GroupEditOverlay(
            backdrop = backdrop,
            target = groupEditTarget,
            onDismiss = { groupEditOpen = false }
        )
    }

    // 排序玻璃弹框覆盖层（�? GroupEditOverlay 同：全屏外层 Box 直接子节点，采样真实书架页）
    AnimatedVisibility(
        visible = sortDialogOpen,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(160)),
        exit = fadeOut(tween(120))
    ) {
        SortDialogOverlay(
            backdrop = backdrop,
            currentSort = bookshelfSort,
            onSelect = { index ->
                bookshelfSort = index
                AppConfig.bookshelfSort = index
                sortDialogOpen = false
            },
            onDismiss = { sortDialogOpen = false }
        )
    }

    // 分组抽屉玻璃弹框覆盖层（右边缘滑出，�? GroupEditOverlay 同模式）
    AnimatedVisibility(
        visible = groupDrawerOpen,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(160)),
        exit = fadeOut(tween(120))
    ) {
        GroupDrawerOverlay(
            backdrop = backdrop,
            currentGroupId = currentGroupId,
            onSelectGroup = { group ->
                currentGroupId = group.groupId
                currentGroupName = if (group.groupId == BookGroup.IdAll) null
                    else group.groupName
            },
            onDismiss = { groupDrawerOpen = false },
        )
    }
    }
}

/**
 * 单个 tab 的图�? + 可选角�?
 */
@Composable
private fun TabIconWithBadge(
    item: BottomTabItem,
    selected: Boolean,
    accentColor: Color,
    contentColor: Color,
    badgeCount: Int,
) {
    val tint = if (selected) accentColor else contentColor
    Box {
        Image(
            painter = painterResource(id = if (selected) item.iconSelected else item.iconEmpty),
            contentDescription = item.label,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(tint)
        )
        if (badgeCount > 0) {
            BadgeDot(
                count = badgeCount,
                color = accentColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-6).dp)
            )
        }
    }
}

/**
 * 角标：accentColor 背景胶囊 + �?/黑数字�?
 */
@Composable
private fun BadgeDot(
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val textColor = if (ColorUtils.isColorLight(color.toArgb())) Color.Black else Color.White
    Box(
        modifier = modifier
            .background(color, CircleShape)
            .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
            .padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = count.toString(),
            style = TextStyle(color = textColor, fontSize = 11.sp)
        )
    }
}

/**
 * EInk 墨水屏模式的降级导航栏：纯色背景 + 图标 + 文字，不使用玻璃效果�?
 */
@Composable
private fun EInkBottomBar(
    tabItems: List<BottomTabItem>,
    selectedTabIndex: () -> Int,
    onTabSelected: (Int) -> Unit,
    badgeCount: Int,
    bgColor: Color,
    accentColor: Color,
) {
    val contentColor = if (ColorUtils.isColorLight(bgColor.toArgb())) Color.Black else Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(bgColor, RoundedCornerShape(28.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabItems.forEach { item ->
            val selected = item.position == selectedTabIndex()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clickable { onTabSelected(item.position) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TabIconWithBadge(
                    item = item,
                    selected = selected,
                    accentColor = accentColor,
                    contentColor = contentColor,
                    badgeCount = if (item.position == 0) badgeCount else 0
                )
                BasicText(
                    text = item.label,
                    style = TextStyle(
                        color = if (selected) accentColor else contentColor,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

/**
 * 跨页切换（书�? �? 设置）瞬跳后，目标页淡入的起始透明度�?
 *
 * �? 0.35 而非 0：淡入期间始终能看到内容轮廓，不会闪出底层背景色�?
 * 若希望完全无过渡，把 [MainScreen] �? pageFade �? snapTo / animateTo 两行删掉即可�?
 */
private const val CROSS_PAGE_FADE_START = 0.35f

private data class BottomTabItem(
    val position: Int,
    val label: String,
    val iconEmpty: Int,
    val iconSelected: Int,
)

private fun buildTabItems(context: Context, showDiscovery: Boolean): List<BottomTabItem> {
    val items = mutableListOf<BottomTabItem>()
    items.add(
        BottomTabItem(
            position = 0,
            label = context.getString(R.string.bookshelf),
            iconEmpty = R.drawable.ic_bottom_books_e,
            iconSelected = R.drawable.ic_bottom_books_s,
        )
    )
    var position = 1
    if (showDiscovery) {
        items.add(
            BottomTabItem(
                position = position++,
                label = context.getString(R.string.discovery),
                iconEmpty = R.drawable.ic_bottom_explore_e,
                iconSelected = R.drawable.ic_bottom_explore_s,
            )
        )
    }
    items.add(
        BottomTabItem(
            position = position,
            label = context.getString(R.string.setting),
            iconEmpty = R.drawable.ic_bottom_person_e,
            iconSelected = R.drawable.ic_bottom_person_s,
        )
    )
    return items
}

/**
 * 玻璃态标题栏：使�? Liquid Glass 效果（vibrancy + blur + lens）模糊背后页面内容�?
 *
 * 与底部导航栏（LiquidBottomTabs）共享同一�? [backdrop]，视觉风格统一�?
 * 形状�? Capsule（胶囊），高�? 56dp，与原标题栏一致�?
 */
@Composable
private fun GlassTitleBar(
    backdrop: com.kyant.backdrop.Backdrop,
    title: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
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
            .fillMaxWidth(),
        contentAlignment = Alignment.BottomStart
    ) {
        BasicText(
            text = title,
            style = TextStyle(contentColor, 20.sp),
            modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
        )
    }
}

/**
 * 书架页玻璃态标题栏：标题（左） + 玻璃态搜索按�? + 玻璃态「更多」溢出菜单（右）�?
 *
 * 复用�? [ExploreGlassTitleBar] 相同�? drawBackdrop 玻璃按钮 + DropdownMenu 模式�?
 * 菜单项对应原�? R.menu.main_bookshelf 的溢出菜单（BookshelfMenuAction）�?
 * 不渲染分组切�? Tab（按需求不需要分�? tab 功能），仅保留功能入口�?
 */
@Composable
private fun BookshelfGlassTitleBar(
    backdrop: Backdrop,
    title: String,
    containerColor: Color,
    contentColor: Color,
    onSearch: () -> Unit,
    onGroups: () -> Unit,
    menuOpen: Boolean,
    onMenuAction: (BookshelfMenuAction) -> Unit,
    onMenuOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
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
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            // 标题
            BasicText(
                text = title,
                style = TextStyle(contentColor, 20.sp),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            )

            // 搜索按钮（玻璃态）
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(12.dp) },
                        effects = {
                            vibrancy()
                            blur(GlassConfig.blur.toPx())
                            lens(GlassConfig.lensX.toPx(), GlassConfig.lensY.toPx())
                        },
                        onDrawSurface = { drawRect(containerColor.copy(alpha = 0.6f)) }
                    )
                    .size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = onSearch) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = context.getString(R.string.search),
                        tint = contentColor,
                    )
                }
            }

            // 分组按钮（玻璃态）
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(12.dp) },
                        effects = {
                            vibrancy()
                            blur(GlassConfig.blur.toPx())
                            lens(GlassConfig.lensX.toPx(), GlassConfig.lensY.toPx())
                        },
                        onDrawSurface = { drawRect(containerColor.copy(alpha = 0.6f)) }
                    )
                    .size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = onGroups) {
                    Icon(
                        painter = painterResource(R.drawable.ic_groups),
                        contentDescription = context.getString(R.string.group_manage),
                        tint = contentColor,
                    )
                }
            }

            // 更多选项按钮（玻璃态）
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(12.dp) },
                        effects = {
                            vibrancy()
                            blur(GlassConfig.blur.toPx())
                            lens(GlassConfig.lensX.toPx(), GlassConfig.lensY.toPx())
                        },
                        onDrawSurface = { drawRect(containerColor.copy(alpha = 0.6f)) }
                    )
                    .size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = { onMenuOpenChange(!menuOpen) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = "more",
                        tint = contentColor,
                    )
                }
            }
        }
    }
}

/**
 * 发现页玻璃态标题栏：液态玻璃搜索栏 + 分组按钮�?
 * 搜索栏替�?"发现"文字位于左下角，自带 drawBackdrop 玻璃效果�?
 */
@Composable
private fun ExploreGlassTitleBar(
    backdrop: Backdrop,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val groups by appDb.bookSourceDao.flowExploreGroups()
        .collectAsState(initial = emptyList())
    var showGroupMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
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
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            // 液态玻璃搜索栏（左下角�?
            Box(
                modifier = Modifier
                    .weight(1f)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(12.dp) },
                        effects = {
                            vibrancy()
                            blur(GlassConfig.blur.toPx())
                            lens(GlassConfig.lensX.toPx(), GlassConfig.lensY.toPx())
                        },
                        onDrawSurface = { drawRect(containerColor.copy(alpha = 0.6f)) }
                    )
                    .height(40.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    textStyle = TextStyle(contentColor, 14.sp),
                    cursorBrush = SolidColor(Color(context.accentColor)),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (searchQuery.isEmpty()) {
                                BasicText(
                                    text = "搜索书源�?",
                                    style = TextStyle(contentColor.copy(0.5f), 14.sp)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            // 分组按钮（右下角，液态玻璃）
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(12.dp) },
                        effects = {
                            vibrancy()
                            blur(GlassConfig.blur.toPx())
                            lens(GlassConfig.lensX.toPx(), GlassConfig.lensY.toPx())
                        },
                        onDrawSurface = { drawRect(containerColor.copy(alpha = 0.6f)) }
                    )
                    .size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = { showGroupMenu = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_groups),
                        contentDescription = "分组",
                        tint = contentColor,
                    )
                }
                DropdownMenu(
                    expanded = showGroupMenu,
                    onDismissRequest = { showGroupMenu = false },
                ) {
                    if (groups.isEmpty()) {
                        DropdownMenuItem(
                            text = { BasicText("无分�?") },
                            onClick = { showGroupMenu = false },
                        )
                    } else {
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = { BasicText(group) },
                                onClick = {
                                    onSearchQueryChange("group:$group")
                                    showGroupMenu = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * EInk 墨水屏模式的降级标题栏：纯色背景 + 标题文字，不使用玻璃效果�?
 */
@Composable
private fun EInkTitleBar(
    title: String,
    bgColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(100.dp)
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.BottomStart
    ) {
        BasicText(
            text = title,
            style = TextStyle(contentColor, 20.sp),
            modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
        )
    }
}
