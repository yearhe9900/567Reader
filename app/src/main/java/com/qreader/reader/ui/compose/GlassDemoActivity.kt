package com.qreader.reader.ui.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
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
        // 背景图片 - 提供给 backdrop捕获
        Image(
            painter = painterResource(id = android.R.drawable.ic_menu_gallery),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            contentScale = ContentScale.Crop
        )

        // 渐变覆盖层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF667eea).copy(alpha = 0.7f),
                            Color(0xFF764ba2).copy(alpha = 0.7f),
                            Color(0xFFf093fb).copy(alpha = 0.7f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Liquid Glass Demo",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 显示玻璃对话框按钮 - 使用真正的 Liquid Glass
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            blur(8f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.3f))
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "显示玻璃对话框", fontSize = 18.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 显示玻璃卡片按钮 - 使用真正的 Liquid Glass
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            blur(8f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.3f))
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showCard = !showCard },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (showCard) "隐藏玻璃卡片" else "显示玻璃卡片",
                    fontSize = 18.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 玻璃卡片 - 使用真正的 Liquid Glass
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
                            },
                            onDrawSurface = {
                                drawRect(Color.White.copy(alpha = 0.2f))
                            }
                        )
                        .clip(RoundedRectangle(24.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "玻璃卡片",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "这是一个真正的 Liquid Glass 效果卡片！\n当前选中: Tab ${selectedTab + 1}",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // 底部导航栏 - 使用真正的 Liquid Glass
        GlassBottomNavBar(
            backdrop = backdrop,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 玻璃对话框
        if (showDialog) {
            GlassDialog(
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
 * Liquid Glass 风格的底部导航栏
 */
@Composable
fun GlassBottomNavBar(
    backdrop: com.kyant.backdrop.Backdrop,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("首页", "书架", "发现", "我的")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.25f))
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
                                        blur(4f.dp.toPx())
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
                        Text(
                            text = when (index) {
                                0 -> "🏠"
                                1 -> "📚"
                                2 -> "🔍"
                                3 -> "👤"
                                else -> "📱"
                            },
                            fontSize = 20.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Liquid Glass 风格的对话框
 */
@Composable
fun GlassDialog(
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
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCancel?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(24.dp) },
                    effects = {
                        vibrancy()
                        blur(20f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.3f))
                    }
                )
                .clip(RoundedRectangle(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
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
                        Text(text = cancelText, fontSize = 16.sp, color = Color.White)
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
                        Text(text = confirmText, fontSize = 16.sp, color = Color.White)
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
                    Text(text = confirmText, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}
