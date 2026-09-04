package com.qreader.reader.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
import com.qreader.reader.R
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.ui.compose.glass.GlassDialogTokens
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog

/**
 * 「排序」选择弹框（Compose 玻璃态，基于通用 [LiquidGlassDialog]）。
 *
 * 视觉风格与主题模式弹框（ThemeModeDialogOverlay）完全对齐：
 * 窄卡片（0.78f）、大圆角（48dp）、玻璃态圆形单选圈（drawBackdrop + Capsule）、
 * 即点即用（click → apply + dismiss，无需确认按钮）。
 *
 * @param backdrop    玻璃模糊源（书架页内容），由 MainScreen 传入同一 Backdrop 实例
 * @param currentSort 当前排序索引（用于高亮选中项）
 * @param onSelect    选中某个排序项的回调，参数为排序索引
 * @param onDismiss   关闭回调
 */
@Composable
fun SortDialogOverlay(
    backdrop: Backdrop,
    currentSort: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    LiquidGlassDialog(
        backdrop = backdrop,
        onDismiss = onDismiss,
        modifier = Modifier.fillMaxWidth(0.78f),
        cardRadius = 48.dp,
        contentPadding = PaddingValues(0.dp),
    ) { colors ->
        val contentColor = colors.contentColor
        val accentColor = colors.accentColor
        val containerColor = colors.containerColor

        // 标题
        BasicText(
            text = stringResource(R.string.sort),
            modifier = Modifier.padding(start = 28.dp, top = 24.dp, end = 28.dp, bottom = 12.dp),
            style = TextStyle(contentColor, 24.sp, FontWeight.Medium),
        )

        // 排序选项列表（玻璃态圆形单选圈）
        val sortOptions = listOf(
            R.string.bookshelf_px_0 to 0,
            R.string.bookshelf_px_1 to 1,
            R.string.bookshelf_px_2 to 2,
            R.string.bookshelf_px_3 to 3,
            R.string.bookshelf_px_4 to 4,
            R.string.bookshelf_px_5 to 5,
        )
        sortOptions.forEach { (resId, sortIndex) ->
            val isSelected = currentSort == sortIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        onSelect(sortIndex)
                    }
                    .padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 玻璃态单选圈（drawBackdrop 采样 backdrop）
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule() },
                            effects = {
                                if (!AppConfig.isEInkMode) {
                                    colorControls(
                                        brightness = GlassDialogTokens.cardBrightness,
                                        saturation = GlassDialogTokens.cardSaturation,
                                    )
                                    blur(GlassDialogTokens.widgetBlur.toPx())
                                    lens(
                                        GlassDialogTokens.widgetLensX.toPx(),
                                        GlassDialogTokens.widgetLensY.toPx(),
                                        depthEffect = true,
                                    )
                                }
                            },
                            highlight = { Highlight.Plain },
                            onDrawSurface = {
                                drawRect(
                                    if (isSelected) accentColor
                                    else containerColor.copy(0.3f),
                                )
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(Capsule())
                                .background(Color.White),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier.height(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = stringResource(resId),
                        style = TextStyle(contentColor.copy(0.9f), 16.sp),
                    )
                }
            }
        }

        // 底部留白（与主题弹框同款间距）
        Spacer(modifier = Modifier.height(24.dp))
    }
}
