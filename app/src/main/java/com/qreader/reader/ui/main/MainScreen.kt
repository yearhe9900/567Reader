package com.qreader.reader.ui.main

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
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import com.qreader.reader.lib.theme.accentColor
import com.qreader.reader.lib.theme.backgroundColor
import com.qreader.reader.lib.theme.primaryColor
import com.qreader.reader.ui.compose.liquid.LiquidBottomTab
import com.qreader.reader.ui.compose.liquid.LiquidBottomTabs
import com.qreader.reader.ui.compose.liquid.LiquidGlassStyle
import com.qreader.reader.ui.compose.liquid.NavBarGlassConfig
import com.qreader.reader.utils.ColorUtils
import kotlin.math.abs

/**
 * 主界面 —— Compose 实现（整页迁移）
 *
 * 页面结构：
 *  - 底层主题背景（作为 Liquid Glass backdrop 的捕获源）
 *  - 内容区：HorizontalPager（书架/发现/设置三个 Compose 页面）
 *  - 底部导航栏：LiquidBottomTabs 玻璃态胶囊栏（EInk 墨水屏模式降级为纯色栏）
 *
 * 三个页面均为 Compose 函数，backdrop 可直接捕获滚动内容，玻璃折射效果自然作用于真实内容。
 *
 * @param selectedTabIndex       当前选中 tab 的读取函数
 * @param onTabSelected          切换到指定 position 的回调
 * @param badgeCount             书架 tab 的角标数字（待更新书籍数），0 表示不显示
 * @param showDiscovery          是否显示「发现」tab（对应页面数 2 或 3）
 * @param isEInkMode             是否墨水屏模式，为 true 时导航栏降级为纯色栏
 * @param bookshelfPage          书架页面 composable（接收注册 gotoTop / back 回调）
 * @param explorePage            发现页面 composable（接收注册 compressExplore 回调）
 * @param settingsPage           设置页面 composable
 * @param themeDialogOpen        主题模式弹框是否打开（打开时隐藏底部导航栏）
 * @param onThemeDialogOpenChange 主题模式弹框打开状态变化回调（由设置页内弹框上抛）
 * @param registerBookshelfBack  向 MainActivity 暴露书架 back 回调（供返回键使用）
 */
@Composable
fun MainScreen(
    selectedTabIndex: () -> Int,
    onTabSelected: (Int) -> Unit,
    badgeCount: Int,
    showDiscovery: Boolean,
    isEInkMode: Boolean,
    glassConfig: NavBarGlassConfig,
    bookshelfPage: @Composable (
        registerGotoTop: ((() -> Unit)?) -> Unit,
        registerBack: ((() -> Boolean)?) -> Unit,
    ) -> Unit,
    explorePage: @Composable (
        registerCompress: ((() -> Unit)?) -> Unit,
    ) -> Unit,
    settingsPage: @Composable () -> Unit,
    themeDialogOpen: Boolean,
    onThemeDialogOpenChange: (Boolean) -> Unit,
    registerBookshelfBack: ((() -> Boolean)?) -> Unit,
) {
    val context = LocalContext.current

    // 与 BaseActivity.initTheme 一致：以主色深浅判断明暗主题
    val isLightTheme = ColorUtils.isColorLight(context.primaryColor)
    val accentColor = Color(context.accentColor)
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val containerColor = glassConfig.containerColor(isLightTheme)
    val bgColor = Color(context.backgroundColor)

    // 玻璃导航栏的 backdrop 捕获源（捕获真实页面内容，供 lens 折射 / blur 作用其上）
    val backdrop = rememberLayerBackdrop()

    val tabItems = remember(showDiscovery, context) {
        buildTabItems(context, showDiscovery)
    }

    // ── 页面动作（由各页面 composable 注册）──
    var bookshelfGotoTop by remember { mutableStateOf<(() -> Unit)?>(null) }
    var bookshelfBack by remember { mutableStateOf<(() -> Boolean)?>(null) }
    var exploreCompress by remember { mutableStateOf<(() -> Unit)?>(null) }

    // ── 重选计时（双击触发 gotoTop / compressExplore）──
    var bookshelfReselected by remember { mutableLongStateOf(0L) }
    var exploreReselected by remember { mutableLongStateOf(0L) }

    // ── HorizontalPager ──
    val pageCount = tabItems.size
    val pagerState = rememberPagerState(initialPage = selectedTabIndex()) { pageCount }

    // 同步 pager → onTabSelected（用户滑动翻页时通知 MainActivity）
    LaunchedEffect(pagerState.settledPage) {
        onTabSelected(pagerState.settledPage)
    }

    // 用户是否正在用手指拖动 pager。
    // 兜底保护：确保任何程序化滚动都不会在用户手势进行中插手。
    // （真正的回环已在 LiquidBottomTabs 侧切断，此处防止其他时序下的边界情况。）
    // 注意：它只覆盖「手指按住」阶段，松手后的 fling 惯性阶段为 false，故不能作为唯一手段。
    val isUserDragging by pagerState.interactionSource.collectIsDraggedAsState()

    // 跨页切换（0↔2）时整页的淡入进度。
    // 只在 draw 阶段被 graphicsLayer 读取，因此动画期间只重绘、不触发重组。
    val pageFade = remember { Animatable(1f) }

    // 同步 selectedTabIndex → pager（外部设置 selectedTab 时切换到对应页）
    //
    // 分两种策略：相邻页做滑动动画，跨页瞬跳 + 淡入。
    //
    // 之所以跨页不滑动：HorizontalPager 从 0 到 2 必然经过中间的发现页(1)，
    // 用户反馈「快速掠过发现页」观感不好，因此改为瞬跳，并用淡入补足过渡感。
    // （此前「跨页卡在发现页」是导航栏回环把目标页改写成了 1，已在上游切断，
    //   与本处的瞬跳选择无关——瞬跳纯粹是为了观感，不是 workaround。）
    // 回环链路备忘：跨页动画过中点 → currentPage=1 → LiquidBottomTabs 反向回调
    // onTabSelected(1) → selectedTab 被改写为 1 → 本 LaunchedEffect 的 key 变化
    // → 旧动画被取消 → 页面停在发现页。

    LaunchedEffect(selectedTabIndex()) {
        if (isUserDragging) return@LaunchedEffect // 手势滑动中不干预，交给用户
        val target = selectedTabIndex()
        if (pagerState.settledPage == target) return@LaunchedEffect
        if (abs(pagerState.settledPage - target) > 1) {
            // 跨页：直接跳转，不让页面从中间的发现页「掠过」（观感差），
            // 改为瞬跳 + 目标页淡入，兼顾干脆与平滑。
            pagerState.scrollToPage(target)
            pageFade.snapTo(CROSS_PAGE_FADE_START)
            pageFade.animateTo(
                1f,
                tween(durationMillis = 160, easing = FastOutLinearInEasing)
            )
        } else {
            // 相邻页：短促 tween 平滑滑过。用 tween 而非默认 spring：
            // spring 收尾段速度衰减极慢，长距离下会明显拖沓。
            pagerState.animateScrollToPage(
                page = target,
                animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing),
            )
        }
    }

    // Tab 点击处理（含双击重选逻辑）
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
        // 玻璃 backdrop 捕获源：底色 + 真实页面内容（导航栏玻璃折射/模糊作用其上）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .layerBackdrop(backdrop)
        ) {
            // 内容区：HorizontalPager 全屏，延伸到状态栏和导航栏背后
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = pageFade.value },
                beyondViewportPageCount = 2, // 预组合全部 3 页，避免跳页时中间页组合卡顿
            ) { page ->
                when (page) {
                    0 -> bookshelfPage(
                        { bookshelfGotoTop = it },
                        { back ->
                            bookshelfBack = back
                            registerBookshelfBack(back)
                        },
                    )

                    1 -> if (showDiscovery) {
                        explorePage(
                            { exploreCompress = it },
                        )
                    } else {
                        settingsPage()
                    }

                    2 -> settingsPage()
                }
            }
        }

        // 玻璃标题栏：悬浮在顶部（overlay，不占内容流）
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
                    0 -> context.getString(R.string.bookshelf)
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

        // 底部导航栏：悬浮透明胶囊栏（overlay，不占内容流）
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
                        glassStyle = glassConfig.toLiquidGlassStyle(),
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
    }
}

/**
 * 单个 tab 的图标 + 可选角标
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
 * 角标：accentColor 背景胶囊 + 白/黑数字。
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
 * EInk 墨水屏模式的降级导航栏：纯色背景 + 图标 + 文字，不使用玻璃效果。
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
 * 跨页切换（书架 ↔ 设置）瞬跳后，目标页淡入的起始透明度。
 *
 * 取 0.35 而非 0：淡入期间始终能看到内容轮廓，不会闪出底层背景色。
 * 若希望完全无过渡，把 [MainScreen] 中 pageFade 的 snapTo / animateTo 两行删掉即可。
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
 * 玻璃态标题栏：使用 Liquid Glass 效果（vibrancy + blur + lens）模糊背后页面内容。
 *
 * 与底部导航栏（LiquidBottomTabs）共享同一个 [backdrop]，视觉风格统一。
 * 形状为 Capsule（胶囊），高度 56dp，与原标题栏一致。
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
                    blur(8f.dp.toPx())
                    lens(24f.dp.toPx(), 24f.dp.toPx())
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
 * EInk 墨水屏模式的降级标题栏：纯色背景 + 标题文字，不使用玻璃效果。
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
