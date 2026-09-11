package com.qreader.reader.ui.book.bookmark

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateTopPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.qreader.reader.R
import com.qreader.reader.data.entities.Bookmark
import com.qreader.reader.lib.theme.accentColor
import com.qreader.reader.lib.theme.backgroundColor
import com.qreader.reader.lib.theme.primaryTextColor
import com.qreader.reader.lib.theme.secondaryTextColor
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.GlassDropdownMenu
import com.qreader.reader.ui.compose.glass.GlassDropdownMenuItem
import com.qreader.reader.ui.compose.glass.GlassTopBar
import com.qreader.reader.ui.compose.glass.GlassTopBarIcon
import com.qreader.reader.ui.compose.glass.GlassTopBarReservedHeight

/**
 * 所有书签页（AllBookmarkActivity）的 Compose 实现。
 *
 * 原 View 版是「TitleBar + RecyclerView(BookmarkAdapter + BookmarkDecoration 粘性分组头)」。
 * 这里对齐既有 Compose 玻璃页范式（见 SearchScreen / BookInfoScreen）：
 *  - 内容放进 [layerBackdrop] 捕获层，供悬浮玻璃顶栏采样；
 *  - [GlassTopBar] 放在捕获层之外；
 *  - 书签按 (bookName, bookAuthor) 分组，用 LazyColumn 的 stickyHeader 还原原
 *    BookmarkDecoration 的粘性分组头。
 *
 * 右侧「更多」菜单对应原 options menu（R.menu.bookmark 的导出 / 导出 Markdown 两项），
 * 用 [GlassDropdownMenu] 承担（Compose 顶栏没有 options menu 体系）。
 */
@Composable
fun AllBookmarkScreen(
    title: String,
    bookmarks: List<Bookmark>,
    onBack: () -> Unit,
    onExportTxt: () -> Unit,
    onExportMd: () -> Unit,
    onItemClick: (Bookmark, Int) -> Unit,
    onItemLongClick: (Bookmark, Int) -> Unit,
) {
    val context = LocalContext.current
    val backdrop = rememberLayerBackdrop()
    val isLightTheme = GlassConfig.isLightTheme(context)
    val containerColor = GlassConfig.containerColor(isLightTheme)
    val contentColor = GlassConfig.contentColor(isLightTheme)
    val bgColor = Color(context.backgroundColor)

    var menuOpen by remember { mutableStateOf(false) }

    GlassConfig.SyncStatusBarToGlassTheme(context, isLightTheme)

    Box(Modifier.fillMaxSize()) {
        // ── 捕获层：书签列表（供玻璃顶栏采样）──
        Column(
            Modifier
                .fillMaxSize()
                .background(bgColor)
                .layerBackdrop(backdrop)
        ) {
            // 为悬浮玻璃顶栏留空间（状态栏 + 56dp 标题栏）
            Spacer(
                Modifier
                    .statusBarsPadding()
                    .height(GlassTopBarReservedHeight)
            )
            BookmarkList(
                bookmarks = bookmarks,
                onItemClick = onItemClick,
                onItemLongClick = onItemLongClick,
            )
        }

        // ── 玻璃顶栏（捕获层之外）──
        GlassTopBar(
            title = title,
            backdrop = backdrop,
            onBack = onBack,
            isLightTheme = isLightTheme,
        ) {
            GlassTopBarIcon(
                iconRes = R.drawable.ic_more_vert,
                contentDescription = context.getString(R.string.export),
                contentColor = contentColor,
                onClick = { menuOpen = true },
            )
        }

        // ── 导出下拉菜单（全屏层，捕获层之外）──
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        GlassDropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            backdrop = backdrop,
            containerColor = containerColor,
            contentColor = contentColor,
            topPadding = GlassConfig.titleBarHeight + statusBarTop + 4.dp,
        ) {
            GlassDropdownMenuItem(
                text = context.getString(R.string.export),
                contentColor = contentColor,
                onClick = {
                    menuOpen = false
                    onExportTxt()
                },
            )
            GlassDropdownMenuItem(
                text = context.getString(R.string.export_md),
                contentColor = contentColor,
                onClick = {
                    menuOpen = false
                    onExportMd()
                },
            )
        }
    }
}

@Composable
private fun BookmarkList(
    bookmarks: List<Bookmark>,
    onItemClick: (Bookmark, Int) -> Unit,
    onItemLongClick: (Bookmark, Int) -> Unit,
) {
    val context = LocalContext.current
    val headerBg = Color(context.backgroundColor)
    val headerTextColor = Color(context.accentColor)
    val primaryText = Color(context.primaryTextColor)
    val secondaryText = Color(context.secondaryTextColor)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        var lastName: String? = null
        var lastAuthor: String? = null
        bookmarks.forEachIndexed { index, bookmark ->
            val isHeader = index == 0 ||
                bookmark.bookName != lastName ||
                bookmark.bookAuthor != lastAuthor
            if (isHeader) {
                stickyHeader {
                    BasicText(
                        text = "${bookmark.bookName}(${bookmark.bookAuthor})",
                        style = TextStyle(color = headerTextColor, fontSize = 16.sp),
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(headerBg)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
            item {
                BookmarkRow(
                    bookmark = bookmark,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    onClick = { onItemClick(bookmark, index) },
                    onLongClick = { onItemLongClick(bookmark, index) },
                )
            }
            lastName = bookmark.bookName
            lastAuthor = bookmark.bookAuthor
        }
    }
}

@Composable
private fun BookmarkRow(
    bookmark: Bookmark,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        BasicText(
            text = bookmark.chapterName,
            style = TextStyle(color = primaryText, fontSize = 16.sp),
            maxLines = 1,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        if (bookmark.bookText.isNotEmpty()) {
            BasicText(
                text = bookmark.bookText,
                style = TextStyle(color = secondaryText, fontSize = 12.sp),
                maxLines = 1,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        if (bookmark.content.isNotEmpty()) {
            BasicText(
                text = bookmark.content,
                style = TextStyle(color = secondaryText, fontSize = 12.sp),
                maxLines = 1,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}
