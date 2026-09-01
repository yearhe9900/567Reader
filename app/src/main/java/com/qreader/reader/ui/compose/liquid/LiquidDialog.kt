package com.qreader.reader.ui.compose.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle

/**
 * 官方 AndroidLiquidGlass DialogContent 的忠实移植：
 * 全屏 dim 遮罩 + 玻璃态对话框卡片（colorControls + blur + lens(depthEffect) + Highlight.Plain）。
 */
@Composable
fun LiquidDialog(
    backdrop: Backdrop,
    title: String,
    message: String,
    confirmText: String = "Okay",
    cancelText: String? = "Cancel",
    onConfirm: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val accentColor =
        if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)
    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.6f)
        else Color(0xFF121212).copy(0.4f)
    val dimColor =
        if (isLightTheme) Color(0xFF29293A).copy(0.23f)
        else Color(0xFF121212).copy(0.56f)

    Box(modifier = Modifier.fillMaxSize()) {
        // 1) dim 遮罩层：仅盖在背景（wallpaper/内容）之上，不压暗卡片
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    drawRect(dimColor)
                }
        )

        // 2) 居中玻璃卡片层（点外部区域 = 取消）
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onCancel?.invoke() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* 点卡片内部不关闭 */ }
                    .padding(40f.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(48f.dp) },
                        effects = {
                            colorControls(
                                brightness = if (isLightTheme) 0.2f else 0f,
                                saturation = 1.5f
                            )
                            blur(if (isLightTheme) 16f.dp.toPx() else 8f.dp.toPx())
                            lens(24f.dp.toPx(), 48f.dp.toPx(), depthEffect = true)
                        },
                        highlight = { Highlight.Plain },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .fillMaxWidth()
            ) {
                BasicText(
                    title,
                    Modifier.padding(28f.dp, 24f.dp, 28f.dp, 12f.dp),
                    style = TextStyle(contentColor, 24f.sp, FontWeight.Medium)
                )

                BasicText(
                    message,
                    Modifier
                        .padding(24f.dp, 12f.dp, 24f.dp, 12f.dp),
                    style = TextStyle(contentColor.copy(0.68f), 15f.sp),
                    maxLines = 5
                )

                Row(
                    Modifier
                        .padding(24f.dp, 12f.dp, 24f.dp, 24f.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16f.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (cancelText != null && onCancel != null) {
                        Row(
                            Modifier
                                .clip(Capsule())
                                .background(containerColor.copy(0.2f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onCancel.invoke() }
                                .height(48f.dp)
                                .weight(1f)
                                .padding(horizontal = 16f.dp),
                            horizontalArrangement = Arrangement.spacedBy(4f.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicText(
                                cancelText,
                                style = TextStyle(contentColor, 16f.sp)
                            )
                        }
                    }

                    Row(
                        Modifier
                            .clip(Capsule())
                            .background(accentColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onConfirm.invoke() }
                            .height(48f.dp)
                            .weight(1f)
                            .padding(horizontal = 16f.dp),
                        horizontalArrangement = Arrangement.spacedBy(4f.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            confirmText,
                            style = TextStyle(Color.White, 16f.sp)
                        )
                    }
                }
            }
        }
    }
}
