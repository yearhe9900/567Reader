package com.qreader.reader.ui.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
    var showDialog by remember { mutableStateOf(false) }
    var showCard by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val backdrop = rememberLayerBackdrop()

    Box(modifier = Modifier.fillMaxSize()) {
        // 壁纸背景 - 作为 backdrop捕获源（与官方 demo 一致）
        Image(
            painter = painterResource(id = com.qreader.reader.R.drawable.wallpaper_light),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            contentScale = ContentScale.Crop
        )

        // 内容区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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

            Spacer(modifier = Modifier.height(32.dp))

            // 显示玻璃对话框按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
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
                    ) { showDialog = true },
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "显示玻璃对话框",
                    style = TextStyle(Color.White, 18.sp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 显示玻璃卡片按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
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
                    ) { showCard = !showCard },
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = if (showCard) "隐藏玻璃卡片" else "显示玻璃卡片",
                    style = TextStyle(Color.White, 18.sp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 玻璃卡片
            if (showCard) {
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
        }

        // 底部导航栏 - 简化版玻璃效果
        SimpleGlassBottomNavBar(
            backdrop = backdrop,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 玻璃对话框
        if (showDialog) {
            SimpleGlassDialog(
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

/**
 * 简化版玻璃底部导航栏
 */
@Composable
fun SimpleGlassBottomNavBar(
    backdrop: com.kyant.backdrop.Backdrop,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("首页", "书架", "发现", "我的")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(12f.dp.toPx())
                        lens(24f.dp.toPx(), 24f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.2f))
                    }
                )
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) }
                        .then(
                            if (isSelected) {
                                Modifier.drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { Capsule() },
                                    effects = {
                                        blur(6f.dp.toPx())
                                        lens(8f.dp.toPx(), 12f.dp.toPx())
                                    },
                                    onDrawSurface = {
                                        drawRect(Color.White.copy(alpha = 0.4f))
                                    }
                                )
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BasicText(
                            text = when (index) {
                                0 -> "🏠"
                                1 -> "📚"
                                2 -> "🔍"
                                3 -> "👤"
                                else -> "📱"
                            },
                            style = TextStyle(fontSize = 20.sp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        BasicText(
                            text = title,
                            style = TextStyle(
                                Color.White,
                                12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * 简化版玻璃对话框
 */
@Composable
fun SimpleGlassDialog(
    backdrop: com.kyant.backdrop.Backdrop,
    title: String,
    message: String,
    confirmText: String = "确定",
    cancelText: String? = "取消",
    onConfirm: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCancel?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(24.dp) },
                    effects = {
                        vibrancy()
                        blur(24f.dp.toPx())
                        lens(32f.dp.toPx(), 48f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.3f))
                    }
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText(
                    text = title,
                    style = TextStyle(Color.White, 20.sp, FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(16.dp))

                BasicText(
                    text = message,
                    style = TextStyle(Color.White.copy(alpha = 0.9f), 16.sp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (cancelText != null && onCancel != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { Capsule() },
                                    effects = {
                                        blur(4f.dp.toPx())
                                        lens(8f.dp.toPx(), 12f.dp.toPx())
                                    },
                                    onDrawSurface = {
                                        drawRect(Color.White.copy(alpha = 0.2f))
                                    }
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onCancel.invoke() },
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                text = cancelText,
                                style = TextStyle(Color.White, 16.sp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { Capsule() },
                                    effects = {
                                        blur(4f.dp.toPx())
                                        lens(8f.dp.toPx(), 12f.dp.toPx())
                                    },
                                    onDrawSurface = {
                                        drawRect(Color.White.copy(alpha = 0.4f))
                                    }
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onConfirm.invoke() },
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                text = confirmText,
                                style = TextStyle(Color.White, 16.sp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { Capsule() },
                                effects = {
                                    blur(4f.dp.toPx())
                                    lens(8f.dp.toPx(), 12f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha = 0.4f))
                                }
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onConfirm.invoke() },
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            text = confirmText,
                            style = TextStyle(Color.White, 16.sp)
                        )
                    }
                }
            }
        }
    }
}
