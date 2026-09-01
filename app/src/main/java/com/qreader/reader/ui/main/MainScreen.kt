package com.qreader.reader.ui.main

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.viewpager.widget.ViewPager
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.qreader.reader.R
import com.qreader.reader.lib.theme.accentColor
import com.qreader.reader.lib.theme.backgroundColor
import com.qreader.reader.lib.theme.primaryColor
import com.qreader.reader.ui.compose.liquid.LiquidBottomTab
import com.qreader.reader.ui.compose.liquid.LiquidBottomTabs
import com.qreader.reader.ui.compose.liquid.NavBarGlassConfig
import com.qreader.reader.utils.ColorUtils

/**
 * 主界面 —— Compose 实现（整页迁移）
 *
 * 页面结构：
 *  - 底层主题背景（作为 Liquid Glass backdrop 的捕获源）
 *  - 内容区：AndroidView 包 ViewPager（书架/发现/我的三个 Fragment 暂保留 View 体系，
 *    后续阶段再逐页 Compose 化）
 *  - 底部导航栏：LiquidBottomTabs 玻璃态胶囊栏（EInk 墨水屏模式降级为纯色栏）
 *
 * 注意：内容区当前是 ViewPager（View 体系），Compose 的 backdrop 无法跨 View 边界捕获，
 * 因此玻璃栏的「真实模糊」对象是底层背景色而非滚动内容；液态变形/高光/色差/拖拽动画
 * 等效果不受影响，待内容页 Compose 化后模糊自然作用于滚动内容。
 *
 * @param viewPager        由 MainActivity 创建并配置好 adapter 的 ViewPager
 * @param selectedTabIndex 当前选中 tab（即 ViewPager 的 position）的读取函数
 * @param onTabSelected    切换到指定 position 的回调
 * @param onTabReselected  点击已选中 tab（重选）的回调，用于书架回顶/发现压缩
 * @param badgeCount       书架 tab 的角标数字（待更新书籍数），0 表示不显示
 * @param showDiscovery    是否显示「发现」tab（对应 ViewPager 页数 2 或 3）
 * @param isEInkMode       是否墨水屏模式，为 true 时导航栏降级为纯色栏
 */
@Composable
fun MainScreen(
    viewPager: ViewPager,
    selectedTabIndex: () -> Int,
    onTabSelected: (Int) -> Unit,
    onTabReselected: (Int) -> Unit,
    badgeCount: Int,
    showDiscovery: Boolean,
    isEInkMode: Boolean,
    glassConfig: NavBarGlassConfig,
) {
    val context = LocalContext.current

    // 与 BaseActivity.initTheme 一致：以主色深浅判断明暗主题
    val isLightTheme = ColorUtils.isColorLight(context.primaryColor)
    val accentColor = Color(context.accentColor)
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val containerColor = glassConfig.containerColor(isLightTheme)
    val bgColor = Color(context.backgroundColor)

    // 玻璃导航栏的 backdrop 捕获源。
    // 内容区是 ViewPager（View 体系），Compose 的 drawBackdrop 无法跨 View 边界捕获，
    // 因此真实滚动内容不会被玻璃采样。若 backdrop 为纯色，lens 扭曲无空间变化 → 折射效果“看不见”。
    // 这里用一个带明暗变化的渐变层作为 backdrop 源：它位于最底层、被 ViewPager 完全覆盖，
    // 仅被玻璃栏采样，使 lens 折射/扭曲产生可见的形变。真正的内容折射需待 Fragment Compose 化。
    val backdrop = rememberLayerBackdrop()
    val glassBackdropTint = Color(
        red = (bgColor.red + 0.16f).coerceAtMost(1f),
        green = (bgColor.green + 0.16f).coerceAtMost(1f),
        blue = (bgColor.blue + 0.16f).coerceAtMost(1f),
        alpha = 1f
    )

    val tabItems = remember(showDiscovery, context) {
        buildTabItems(context, showDiscovery)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 玻璃 backdrop 捕获源（仅被玻璃采样，不直接显示）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(bgColor, glassBackdropTint)))
                .layerBackdrop(backdrop)
        )

        // 视觉背景（实际可见底色，覆盖在 backdrop 源之上，保证全局背景观感不变）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
        )

        // 内容区：ViewPager 全屏，延伸到悬浮导航栏背后
        AndroidView(
            factory = { viewPager },
            modifier = Modifier.fillMaxSize()
        )

        // 底部导航栏：悬浮透明胶囊栏（overlay，不占内容流）
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
        ) {
                if (isEInkMode) {
                    EInkBottomBar(
                        tabItems = tabItems,
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = onTabSelected,
                        onTabReselected = onTabReselected,
                        badgeCount = badgeCount,
                        bgColor = bgColor,
                        accentColor = accentColor,
                    )
                } else {
                    LiquidBottomTabs(
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = onTabSelected,
                        backdrop = backdrop,
                        tabsCount = tabItems.size,
                        accentColor = accentColor,
                        containerColor = containerColor,
                        isLightTheme = isLightTheme,
                        glassStyle = glassConfig.toLiquidGlassStyle(),
                    ) {
                        tabItems.forEach { item ->
                            LiquidBottomTab(
                                onClick = {
                                    if (item.position == selectedTabIndex()) {
                                        onTabReselected(item.position)
                                    } else {
                                        onTabSelected(item.position)
                                    }
                                }
                            ) {
                                TabIconWithBadge(
                                    item = item,
                                    selected = item.position == selectedTabIndex(),
                                    accentColor = accentColor,
                                    contentColor = contentColor,
                                    badgeCount = if (item.position == 0) badgeCount else 0
                                )
                                BasicText(
                                    text = item.label,
                                    style = TextStyle(
                                        color = if (item.position == selectedTabIndex()) {
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

/**
 * 单个 tab 的图标 + 可选角标
 *
 * 图标根据选中态着色（选中用 accentColor，未选中用 contentColor），
 * 角标仅在书架 tab（position == 0）且 count > 0 时显示。
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
 * 角标（对应原 BadgeView）：accentColor 背景胶囊 + 白/黑数字。
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
    onTabReselected: (Int) -> Unit,
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
                    .clickable {
                        if (selected) onTabReselected(item.position) else onTabSelected(item.position)
                    },
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
