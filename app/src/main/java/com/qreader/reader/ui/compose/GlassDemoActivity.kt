package com.qreader.reader.ui.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

    // 渐变背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF667eea),
                        Color(0xFF764ba2),
                        Color(0xFFf093fb)
                    )
                )
            )
    ) {
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

            // 显示玻璃对话框按钮
            Button(
                onClick = { showDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.3f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "显示玻璃对话框", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 显示玻璃卡片按钮
            Button(
                onClick = { showCard = !showCard },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.3f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (showCard) "隐藏玻璃卡片" else "显示玻璃卡片",
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 模拟的玻璃卡片（简化版）
            if (showCard) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "玻璃卡片（简化版）",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "这是一个模拟的玻璃效果卡片。\n当前选中: Tab ${selectedTab + 1}",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // 底部导航栏（简化版）
        SimpleBottomNavBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 玻璃对话框（简化版）
        if (showDialog) {
            SimpleGlassDialog(
                title = "提示",
                message = "这是一个使用玻璃效果的对话框。\n\n你可以在阅读器的弹窗中使用这种效果。",
                confirmText = "确定",
                cancelText = "取消",
                onConfirm = { showDialog = false },
                onCancel = { showDialog = false }
            )
        }
    }
}

/**
 * 简化版底部导航栏
 */
@Composable
fun SimpleBottomNavBar(
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
                .background(
                    Color.White.copy(alpha = 0.2f),
                    RoundedCornerShape(50)
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
                                Modifier.background(
                                    Color.White.copy(alpha = 0.3f),
                                    RoundedCornerShape(50)
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
 * 简化版玻璃对话框
 */
@Composable
fun SimpleGlassDialog(
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
                .background(
                    Color.White.copy(alpha = 0.25f),
                    RoundedCornerShape(24.dp)
                )
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
                    Button(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = cancelText, fontSize = 16.sp)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.4f),
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = confirmText, fontSize = 16.sp)
                    }
                }
            } else {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.4f),
                        contentColor = Color.White
                    )
                ) {
                    Text(text = confirmText, fontSize = 16.sp)
                }
            }
        }
    }
}
