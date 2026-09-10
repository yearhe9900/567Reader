package com.qreader.reader.ui.book.read

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.qreader.reader.R
import com.qreader.reader.help.config.ReadBookConfig
import com.qreader.reader.model.ReadBook
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog
import com.qreader.reader.utils.ColorUtils

/**
 * 书内搜索浮层玻璃面板（替代 SearchMenu / view_search_menu）。
 *
 * 上一/下一条 + 信息行 + 搜索结果/主菜单/退出；采样 [backdrop] 正文。
 */
@Composable
fun SearchMenuGlassSheet(
    backdrop: Backdrop,
    state: ReadPageOverlayState,
    onPrevResult: () -> Unit,
    onNextResult: () -> Unit,
    onOpenSearch: () -> Unit,
    onMainMenu: () -> Unit,
    onExit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isLight = ColorUtils.isColorLight(ReadBookConfig.bgMeanColor)
    val contentColor = GlassConfig.contentColor(isLight)
    val chapterTitle = ReadBook.curTextChapter?.title ?: state.chapterName
    val info = "${stringResource(R.string.search_content_size)}: ${state.searchResults.size} / 当前章节: $chapterTitle"

    // 上一/下一条：悬浮玻璃圆钮（结果存在时才显示）
    if (state.searchResults.isNotEmpty()) {
        Box(Modifier.fillMaxSize()) {
            SmallGlassFab(
                icon = R.drawable.ic_arrow_back,
                contentDescription = "上一条",
                contentColor = contentColor,
                backdrop = backdrop,
                onClick = onPrevResult,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp),
            )
            SmallGlassFab(
                icon = R.drawable.ic_arrow_right,
                contentDescription = "下一条",
                contentColor = contentColor,
                backdrop = backdrop,
                onClick = onNextResult,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
            )
        }
    }

    LiquidGlassDialog(
        backdrop = backdrop,
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        cardRadius = GlassConfig.sheetCornerRadius,
        alignment = Alignment.BottomCenter,
        showScrim = false,
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            BasicText(
                text = info,
                style = TextStyle(contentColor, 13.sp),
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                SheetAction(
                    icon = R.drawable.ic_search,
                    label = stringResource(R.string.search_content),
                    tint = contentColor,
                    onClick = onOpenSearch,
                )
                SheetAction(
                    icon = R.drawable.ic_more,
                    label = stringResource(R.string.main_menu),
                    tint = contentColor,
                    onClick = onMainMenu,
                )
                SheetAction(
                    icon = R.drawable.ic_close_with__shadow,
                    label = stringResource(R.string.exit),
                    tint = contentColor,
                    onClick = onExit,
                )
            }
        }
    }
}

@Composable
private fun SheetAction(
    icon: Int,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(4.dp))
        BasicText(text = label, style = TextStyle(tint, 12.sp))
    }
}

@Composable
private fun SmallGlassFab(
    icon: Int,
    contentDescription: String,
    contentColor: Color,
    backdrop: Backdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(GlassConfig.containerColor(ColorUtils.isColorLight(ReadBookConfig.bgMeanColor)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
    }
}
