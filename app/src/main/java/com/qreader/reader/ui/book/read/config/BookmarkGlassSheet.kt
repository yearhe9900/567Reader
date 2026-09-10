package com.qreader.reader.ui.book.read.config

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.Bookmark
import com.qreader.reader.help.config.ReadBookConfig
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog
import com.qreader.reader.utils.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 书签编辑玻璃面板（仅阅读页 in-tree）。
 * 全部书签/目录页仍用 [com.qreader.reader.ui.book.bookmark.BookmarkDialog]。
 */
@Composable
fun BookmarkGlassSheet(
    backdrop: Backdrop,
    bookmark: Bookmark,
    editPos: Int,
    onDismiss: () -> Unit,
) {
    val isLight = ColorUtils.isColorLight(ReadBookConfig.bgMeanColor)
    val contentColor = GlassConfig.contentColor(isLight)
    val accent = GlassConfig.toggleAccentColor(isLight)
    val scope = rememberCoroutineScope()
    var bookText by remember { mutableStateOf(bookmark.bookText ?: "") }
    var content by remember { mutableStateOf(bookmark.content ?: "") }

    LiquidGlassDialog(
        backdrop = backdrop,
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding(),
        cardRadius = GlassConfig.dialogCardRadius,
        alignment = Alignment.BottomCenter,
        showScrim = false,
    ) { colors ->
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            BasicText(
                text = bookmark.chapterName ?: "",
                style = TextStyle(colors.contentColor, 14.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            FieldBox(bookText, { bookText = it }, colors.contentColor, 48.dp)
            Spacer(Modifier.height(12.dp))
            FieldBox(content, { content = it }, colors.contentColor, 120.dp)
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (editPos >= 0) {
                    ActionBtn(
                        "删除",
                        colors.containerColor.copy(alpha = 0.3f),
                        colors.contentColor,
                        Modifier.weight(1f),
                    ) {
                        scope.launch {
                            withContext(Dispatchers.IO) { appDb.bookmarkDao.delete(bookmark) }
                            onDismiss()
                        }
                    }
                }
                ActionBtn(
                    "取消",
                    colors.containerColor.copy(alpha = 0.3f),
                    colors.contentColor,
                    Modifier.weight(1f),
                    onDismiss,
                )
                ActionBtn(
                    "确定",
                    accent,
                    Color.White,
                    Modifier.weight(1f),
                ) {
                    bookmark.bookText = bookText
                    bookmark.content = content
                    scope.launch {
                        withContext(Dispatchers.IO) { appDb.bookmarkDao.insert(bookmark) }
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldBox(
    value: String,
    onValueChange: (String) -> Unit,
    contentColor: Color,
    minHeight: Dp,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(contentColor, 15.sp),
        cursorBrush = SolidColor(contentColor),
        modifier = Modifier
            .fillMaxWidth()
            .height(minHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.08f))
            .padding(12.dp),
    )
}

@Composable
private fun ActionBtn(
    text: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    BasicText(
        text = text,
        style = TextStyle(
            color = fg,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        ),
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 12.dp),
    )
}
