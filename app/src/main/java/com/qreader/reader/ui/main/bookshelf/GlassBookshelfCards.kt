package com.qreader.reader.ui.main.bookshelf

import android.widget.FrameLayout
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.qreader.reader.R
import com.qreader.reader.data.entities.Book
import com.qreader.reader.help.book.isLocal
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.lib.theme.accentColor
import com.qreader.reader.ui.widget.image.CoverImageView

/** 标题栏下方留白，与 MainScreen 玻璃标题栏对齐。 */
val BookshelfListTopPadding = 110.dp

/**
 * 书架书籍列表/网格（Compose，无玻璃卡片）。
 * 仅封面 + 文字；折射效果由导航栏/标题栏在更上层完成。
 */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassBookListItem(
    book: Book,
    isRefreshingBook: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val contentColor = colorResource(R.color.primaryText)
    val summaryColor = colorResource(R.color.tv_text_summary)
    val accent = Color(context.accentColor)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCoverView(book = book, widthDp = 66, heightDp = 90)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, end = 4.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = book.name,
                    color = contentColor,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 48.dp),
                )
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    BookStatusIndicator(book, isRefreshingBook, accent)
                }
            }
            MetaLine(R.drawable.ic_author, book.author.orEmpty(), summaryColor)
            MetaLine(R.drawable.ic_history, book.durChapterTitle.orEmpty(), summaryColor)
            MetaLine(R.drawable.ic_book_last, book.latestChapterTitle.orEmpty(), summaryColor)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassBookGridItem(
    book: Book,
    isRefreshingBook: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val contentColor = colorResource(R.color.primaryText)
    val accent = Color(context.accentColor)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(4.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            BookCoverView(
                book = book,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(4.dp)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
            ) {
                BookStatusIndicator(book, isRefreshingBook, accent)
            }
        }
        Text(
            text = book.name,
            color = contentColor,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 2.dp),
        )
    }
}

@Composable
private fun BookStatusIndicator(
    book: Book,
    isRefreshingBook: Boolean,
    accent: Color,
) {
    if (isRefreshingBook) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = accent,
        )
        return
    }
    if (!AppConfig.showUnread) return
    val unread = book.getUnreadChapterNum()
    if (unread <= 0) return
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (book.lastCheckCount > 0) accent else Color.Black.copy(alpha = 0.45f),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = if (unread > 999) "999+" else unread.toString(),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun BookCoverView(
    book: Book,
    modifier: Modifier = Modifier,
    widthDp: Int = 0,
    heightDp: Int = 0,
) {
    val sizeMod = if (widthDp > 0 && heightDp > 0) {
        Modifier.size(widthDp.dp, heightDp.dp)
    } else {
        Modifier
    }
    AndroidView(
        factory = { c ->
            CoverImageView(c).apply {
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                setImageResource(R.drawable.image_cover_default)
                if (widthDp > 0 && heightDp > 0) {
                    val density = c.resources.displayMetrics.density
                    layoutParams = FrameLayout.LayoutParams(
                        (widthDp * density).toInt(),
                        (heightDp * density).toInt(),
                    )
                }
            }
        },
        update = { view -> view.load(book, false) },
        modifier = sizeMod.then(modifier),
    )
}

@Composable
private fun MetaLine(
    iconRes: Int,
    text: String,
    color: Color,
) {
    if (text.isBlank()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 2.dp),
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(color),
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            color = color,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
fun GlassBookshelfGrid(
    books: List<Book>,
    isUpdate: (String) -> Boolean,
    onClick: (Book) -> Unit,
    onLongClick: (Book) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val columns = run {
        val dm = context.resources.displayMetrics
        val w = dm.widthPixels / dm.density
        (w / 100f).toInt().coerceIn(3, 6)
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(books, key = { it.bookUrl }) { book ->
            GlassBookGridItem(
                book = book,
                isRefreshingBook = !book.isLocal && isUpdate(book.bookUrl),
                onClick = { onClick(book) },
                onLongClick = { onLongClick(book) },
            )
        }
    }
}

@Composable
fun GlassBookshelfList(
    books: List<Book>,
    isUpdate: (String) -> Boolean,
    onClick: (Book) -> Unit,
    onLongClick: (Book) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(books, key = { it.bookUrl }) { book ->
            GlassBookListItem(
                book = book,
                isRefreshingBook = !book.isLocal && isUpdate(book.bookUrl),
                onClick = { onClick(book) },
                onLongClick = { onLongClick(book) },
            )
        }
    }
}
