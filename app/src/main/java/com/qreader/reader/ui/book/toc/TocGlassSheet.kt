package com.qreader.reader.ui.book.toc

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.BookChapter
import com.qreader.reader.help.book.simulatedTotalChapterNum
import com.qreader.reader.help.config.ReadBookConfig
import com.qreader.reader.model.ReadBook
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog
import com.qreader.reader.utils.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 目录玻璃面板（阅读页 in-tree，替代 TocActivity 的章节列表）。
 *
 * 纯 Compose LazyColumn；点章节 → [onOpenChapter] 由 Activity 打开并关闭面板。
 * 书签入口仍走原 TocActivity / BookmarkDialog。
 */
@Composable
fun TocGlassSheet(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    onOpenChapter: (index: Int, chapterPos: Int) -> Unit,
) {
    val activity = LocalContext.current as? androidx.appcompat.app.AppCompatActivity ?: return
    val bookUrl = ReadBook.book?.bookUrl ?: return
    val book = ReadBook.book ?: return
    val isLight = ColorUtils.isColorLight(ReadBookConfig.bgMeanColor)
    val contentColor = GlassConfig.contentColor(isLight)
    val durIndex = ReadBook.durChapterIndex
    val total = book.simulatedTotalChapterNum()
    val durTitle = book.durChapterTitle ?: ""
    val useReplace = com.qreader.reader.help.config.AppConfig.tocUiUseReplace
    val chineseConvert = com.qreader.reader.help.config.AppConfig.chineseConverterType != 0
    val listState = rememberLazyListState()

    val chapters by produceState<List<BookChapter>>(emptyList(), bookUrl) {
        value = withContext(Dispatchers.IO) {
            val end = (total - 1).coerceAtLeast(0)
            appDb.bookChapterDao.getChapterList(bookUrl, 0, end)
        }
    }

    LaunchedEffect(chapters, durIndex) {
        val pos = chapters.indexOfFirst { it.index >= durIndex }
        if (pos >= 0) listState.scrollToItem(pos)
    }

    LiquidGlassDialog(
        backdrop = backdrop,
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .navigationBarsPadding(),
        cardRadius = GlassConfig.sheetCornerRadius,
        alignment = androidx.compose.ui.Alignment.BottomCenter,
        showScrim = false,
    ) { colors ->
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = "$durTitle (${durIndex + 1}/$total)",
                    style = TextStyle(colors.contentColor.copy(alpha = 0.75f), 13.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            LazyColumn(state = listState) {
                items(chapters, key = { it.index }) { ch ->
                    val selected = ch.index == durIndex
                    BasicText(
                        text = ch.title ?: ch.getDisplayTitle(
                            useReplace = useReplace,
                            chineseConvert = chineseConvert,
                        ),
                        style = TextStyle(
                            color = if (selected) colors.accentColor else colors.contentColor,
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                onOpenChapter(ch.index, 0)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
