package com.qreader.reader.ui.compose

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.qreader.reader.ui.compose.liquid.FlightIcon
import com.qreader.reader.ui.compose.liquid.LiquidBottomTab
import com.qreader.reader.ui.compose.liquid.LiquidBottomTabs
import com.qreader.reader.ui.compose.liquid.LiquidDialog
import com.qreader.reader.ui.compose.glass.GlassConfig

/**
 * Liquid Glass 效果演示 Activity
 */
class GlassDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlassDemoScreen()
        }
    }
}

@Composable
fun GlassDemoScreen() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var showCard by remember { mutableStateOf(false) }
    var selectedTab3 by remember { mutableIntStateOf(0) }
    var selectedTab4 by remember { mutableIntStateOf(0) }

    val backdrop = rememberLayerBackdrop()
    val scrollState = rememberScrollState()

    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val airplaneModeIcon = rememberVectorPainter(FlightIcon)
    val iconColorFilter = ColorFilter.tint(contentColor)

    Box(modifier = Modifier.fillMaxSize()) {
        // 壁纸背景 - 作为 backdrop 捕获源（与官方 demo 一致）
        Image(
            painter = painterResource(id = com.qreader.reader.R.drawable.wallpaper_light),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            contentScale = ContentScale.Crop
        )

        // 内容区域 - 可滚动，展示官方 LiquidBottomTabs 效果
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(32.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
        ) {
            // 标题 - 玻璃效果
            Box(
                modifier = Modifier
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(16f.dp.toPx(), 24f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.3f))
                        }
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                BasicText(
                    text = "Liquid Glass Demo",
                    style = TextStyle(Color.White, 24.sp, FontWeight.Bold)
                )
            }

            // 显示玻璃对话框按钮
            GlassButton(
                text = "显示玻璃对话框",
                backdrop = backdrop,
                onClick = { showDialog = true }
            )

            // 显示玻璃卡片按钮
            GlassButton(
                text = if (showCard) "隐藏玻璃卡片" else "显示玻璃卡片",
                backdrop = backdrop,
                onClick = { showCard = !showCard }
            )

            // 玻璃卡片
            if (showCard) {
                GlassCard(backdrop = backdrop, selectedTab = selectedTab3)
            }

            // 官方 LiquidBottomTabs 演示（3 tab）
            LiquidBottomTabs(
                selectedTabIndex = { selectedTab3 },
                onTabSelected = { selectedTab3 = it },
                backdrop = backdrop,
                tabsCount = 3,
                modifier = Modifier.padding(horizontal = 36.dp)
            ) {
                repeat(3) { index ->
                    LiquidBottomTab({ selectedTab3 = index }) {
                        Box(
                            Modifier
                                .size(28f.dp)
                                .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                        )
                        BasicText(
                            "Tab ${index + 1}",
                            style = TextStyle(contentColor, 12f.sp)
                        )
                    }
                }
            }

            // 官方 LiquidBottomTabs 演示（4 tab）
            LiquidBottomTabs(
                selectedTabIndex = { selectedTab4 },
                onTabSelected = { selectedTab4 = it },
                backdrop = backdrop,
                tabsCount = 4,
                modifier = Modifier.padding(horizontal = 36.dp)
            ) {
                repeat(4) { index ->
                    LiquidBottomTab({ selectedTab4 = index }) {
                        Box(
                            Modifier
                                .size(28f.dp)
                                .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                        )
                        BasicText(
                            "Tab ${index + 1}",
                            style = TextStyle(contentColor, 12f.sp)
                        )
                    }
                }
            }

            // 底部按钮（与截图一致）
            GlassButton(
                text = "Pick an image",
                backdrop = backdrop,
                onClick = {
                    Toast.makeText(context, "Pick an image clicked", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 玻璃对话框（官方 DialogContent 移植）
        if (showDialog) {
            LiquidDialog(
                backdrop = backdrop,
                title = "提示",
                message = "这是一个真正的 Liquid Glass 效果对话框！\n\n你可以在阅读器的弹窗中使用这种效果。",
                confirmText = "确定",
                cancelText = "取消",
                onConfirm = { showDialog = false },
                onCancel = { showDialog = false }
            )
        }
    }
}

@Composable
private fun GlassButton(
    text: String,
    backdrop: com.kyant.backdrop.Backdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(GlassConfig.titleBarHeight)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(4f.dp.toPx())
                    lens(12f.dp.toPx(), 20f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.25f))
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = TextStyle(Color.White, 18.sp)
        )
    }
}

@Composable
private fun GlassCard(
    backdrop: com.kyant.backdrop.Backdrop,
    selectedTab: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(24.dp) },
                effects = {
                    vibrancy()
                    blur(16f.dp.toPx())
                    lens(20f.dp.toPx(), 32f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.2f))
                }
            )
            .padding(20.dp)
    ) {
        Column {
            BasicText(
                text = "玻璃卡片",
                style = TextStyle(Color.White, 20.sp, FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            BasicText(
                text = "这是一个真正的 Liquid Glass 效果卡片！\n当前选中: Tab ${selectedTab + 1}",
                style = TextStyle(Color.White.copy(alpha = 0.9f), 14.sp)
            )
        }
    }
}
