package com.qreader.reader.ui.book.read

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.qreader.reader.R
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.liquid.LiquidSlider
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.TextStyle

/**
 * 阅读页菜单覆盖层。
 *
 * 顶栏：100dp 高度（延伸到状态栏），与 MainScreen 标题栏统一。
 * 底栏：LiquidSlider 玻璃进度条 + 快捷操作（搜索/自动翻页/替换/日夜）+ 目录·朗读·界面·设置。
 */
@Composable
fun ReadMenuOverlay(
    state: ReadPageOverlayState,
    backdrop: Backdrop,
    onBack: () -> Unit = {},
    onPrevChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    onSeekTo: (Int) -> Unit = {},
    onSearch: () -> Unit = {},
    onAutoPage: () -> Unit = {},
    onReplaceRule: () -> Unit = {},
    onToggleNightTheme: () -> Unit = {},
    onCatalog: () -> Unit = {},
    onReadAloud: () -> Unit = {},
    onFont: () -> Unit = {},
    onSetting: () -> Unit = {},
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isLight = state.isLightPage
    val containerColor = GlassConfig.containerColor(isLight)
    val contentColor = GlassConfig.contentColor(isLight)

    AnimatedVisibility(
        visible = state.menuVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(Modifier.fillMaxSize()) {
            // ── 蒙板（点击关闭）──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onDismiss() }
            )

            // ── 顶栏（100dp，延伸到状态栏，与 MainScreen GlassTitleBar 统一）──
            AnimatedVisibility(
                visible = state.menuVisible,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Box(
                    modifier = Modifier
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
                        .height(100.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    // 消费层放内容之下：空白处吃掉点击防误关菜单；子节点（返回键等）优先命中
                    Box(
                        Modifier
                            .matchParentSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {},
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = "返回",
                                tint = contentColor,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        BasicText(
                            text = state.chapterName,
                            style = TextStyle(color = contentColor, fontSize = 16.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ── 底栏（玻璃延伸到手势栏）──
            AnimatedVisibility(
                visible = state.menuVisible,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
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
                            onDrawSurface = { drawRect(containerColor) },
                        )
                        .windowInsetsPadding(WindowInsets.navigationBars),
                ) {
                    // 消费层铺满整栏（含 padding 区），空白处吃掉点击防误关菜单；
                    // 内容作为上层兄弟，Slider/按钮优先命中，不被 clickable 抢拖拽
                    Box(
                        Modifier
                            .matchParentSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {},
                            )
                    )
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        // 进度条行：上一章 / LiquidSlider / 下一章
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BasicText(
                                text = "上一章",
                                style = TextStyle(
                                    color = if (state.prevEnabled) contentColor else contentColor.copy(alpha = 0.3f),
                                    fontSize = 14.sp,
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = state.prevEnabled) { onPrevChapter() }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                            )
                            LiquidSlider(
                                value = { state.seekProgress.toFloat() },
                                onValueChange = {
                                    state.isDraggingSeek = true
                                    state.seekProgress = it.toInt()
                                },
                                valueRange = 0f..state.seekMax.toFloat().coerceAtLeast(1f),
                                visibilityThreshold = 1f,
                                backdrop = backdrop,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                onValueChangeFinished = {
                                    state.isDraggingSeek = false
                                    onSeekTo(state.seekProgress)
                                },
                            )
                            BasicText(
                                text = "下一章",
                                style = TextStyle(
                                    color = if (state.nextEnabled) contentColor else contentColor.copy(alpha = 0.3f),
                                    fontSize = 14.sp,
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = state.nextEnabled) { onNextChapter() }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // 快捷操作行：搜索 / 自动翻页 / 替换净化 / 日夜切换（对齐原版 FAB 行）
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            MenuButton(R.drawable.ic_search, "搜索", contentColor, onSearch)
                            MenuButton(
                                if (state.autoPage) R.drawable.ic_auto_page_stop else R.drawable.ic_auto_page,
                                if (state.autoPage) "停止" else "自动",
                                contentColor,
                                onAutoPage,
                            )
                            MenuButton(R.drawable.ic_find_replace, "净化", contentColor, onReplaceRule)
                            MenuButton(
                                if (state.isNightTheme) R.drawable.ic_daytime else R.drawable.ic_brightness,
                                if (state.isNightTheme) "日间" else "夜间",
                                contentColor,
                                onToggleNightTheme,
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // 功能按钮行：目录 / 朗读 / 界面 / 设置
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            MenuButton(R.drawable.ic_toc, "目录", contentColor, onCatalog)
                            MenuButton(
                                if (state.isReadAloud) R.drawable.ic_stop_black_24dp else R.drawable.ic_read_aloud,
                                if (state.isReadAloud) "停止" else "朗读",
                                contentColor, onReadAloud
                            )
                            MenuButton(R.drawable.ic_interface_setting, "界面", contentColor, onFont)
                            MenuButton(R.drawable.ic_settings, "设置", contentColor, onSetting)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuButton(
    iconRes: Int,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(4.dp))
        BasicText(
            text = label,
            style = TextStyle(color = tint, fontSize = 12.sp),
        )
    }
}
