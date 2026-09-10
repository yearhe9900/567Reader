package com.qreader.reader.ui.book.toc

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.qreader.reader.R
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.BookChapter
import com.qreader.reader.help.book.BookHelp
import com.qreader.reader.help.book.isLocal
import com.qreader.reader.help.book.simulatedTotalChapterNum
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.help.config.ReadBookConfig
import com.qreader.reader.lib.theme.accentColor
import com.qreader.reader.model.ReadBook
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog
import com.qreader.reader.utils.ColorUtils
import com.qreader.reader.utils.getCompatColor
import com.qreader.reader.utils.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 目录玻璃面板（阅读页 in-tree）。
 *
 * 对齐 legado 原版 ChapterListAdapter / item_chapter_list 视觉：
 * - 当前章高亮 accentColor
 * - VIP 锁、tag（时间）、字数、缓存/已读图标
 * - 卷名加底色
 */
@Composable
fun TocGlassSheet(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    onOpenChapter: (index: Int, chapterPos: Int) -> Unit,
) {
    val ctx = LocalContext.current
    val book = ReadBook.book ?: return
    val bookUrl = book.bookUrl
    val durIndex = ReadBook.durChapterIndex
    val total = book.simulatedTotalChapterNum()
    val durTitle = book.durChapterTitle ?: ""
    val isLocal = book.isLocal
    val accent = Color(ctx.accentColor)
    val contentColor = GlassConfig.contentColor(
        ColorUtils.isColorLight(ReadBookConfig.bgMeanColor)
    )
    val primaryText = Color(ctx.getCompatColor(R.color.primaryText))
    val secondaryText = Color(ctx.getCompatColor(R.color.secondaryText))
    val bgPress = Color(ctx.getCompatColor(R.color.btn_bg_press))
    val showWordCount = AppConfig.tocCountWords
    val listState = rememberLazyListState()

    data class TocData(val chapters: List<BookChapter>, val cache: Set<String>)
    val tocData by produceState(TocData(emptyList(), emptySet()), bookUrl) {
        val end = (book.simulatedTotalChapterNum() - 1).coerceAtLeast(0)
        val chs = withContext(Dispatchers.IO) { appDb.bookChapterDao.getChapterList(bookUrl, 0, end) }
        val cached = withContext(Dispatchers.IO) { BookHelp.getChapterFiles(book).toHashSet() }
        value = TocData(chs, cached)
    }

    LaunchedEffect(tocData.chapters, durIndex) {
        val pos = tocData.chapters.indexOfFirst { it.index >= durIndex }
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
        alignment = Alignment.BottomCenter,
        showScrim = false,
    ) { colors ->
        Column(Modifier.fillMaxWidth()) {
            // 当前章信息行
            BasicText(
                text = "$durTitle (${durIndex + 1}/$total)",
                style = TextStyle(contentColor.copy(alpha = 0.75f), 13.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )

            LazyColumn(state = listState) {
                items(tocData.chapters) { ch ->
                    val isDur = ch.index == durIndex
                    val cached = isLocal || ch.isVolume || tocData.cache.contains(ch.getFileName())
                    val bg = if (ch.isVolume) bgPress else Color.Transparent
                    val titleColor = if (isDur) accent else primaryText

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(bg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onOpenChapter(ch.index, 0) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // VIP 锁
                        if (ch.isVip && !ch.isPay) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lock_outline),
                                contentDescription = null,
                                tint = secondaryText,
                                modifier = Modifier.size(20.dp).padding(end = 4.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                        }

                        // 标题 + tag/字数
                        Column(Modifier.weight(1f)) {
                            BasicText(
                                text = ch.getDisplayTitle(),
                                style = TextStyle(titleColor, 15.sp,
                                    fontWeight = if (isDur) FontWeight.Medium else FontWeight.Normal,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!ch.tag.isNullOrEmpty() || (showWordCount && !ch.wordCount.isNullOrEmpty() && !ch.isVolume)) {
                                val tag = ch.tag
                                val wc = ch.wordCount
                                Row {
                                    if (!tag.isNullOrEmpty()) {
                                        BasicText(text = tag, style = TextStyle(secondaryText, 12.sp))
                                    }
                                    if (showWordCount && !wc.isNullOrEmpty() && !ch.isVolume) {
                                        if (!tag.isNullOrEmpty()) Spacer(Modifier.width(8.dp))
                                        BasicText(text = wc, style = TextStyle(secondaryText, 12.sp))
                                    }
                                }
                            }
                        }

                        // 缓存/已读图标
                        if (isDur) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp),
                            )
                        } else if (!cached) {
                            Icon(
                                painter = painterResource(R.drawable.ic_outline_cloud_24),
                                contentDescription = null,
                                tint = secondaryText,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
