package com.qreader.reader.ui.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qreader.reader.R
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.Book
import com.qreader.reader.data.entities.BookGroup
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.lib.theme.backgroundColor
import com.qreader.reader.ui.book.import.local.ImportBookActivity
import com.qreader.reader.ui.book.import.remote.RemoteBookActivity
import com.qreader.reader.ui.book.manage.BookshelfManageActivity
import com.qreader.reader.ui.book.group.GroupManageDialog
import com.qreader.reader.ui.main.bookshelf.BookshelfListTopPadding
import com.qreader.reader.ui.main.bookshelf.GlassBookshelfGrid
import com.qreader.reader.ui.main.bookshelf.GlassBookshelfList
import com.qreader.reader.utils.cnCompare
import com.qreader.reader.utils.showDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * 书架页 —— Compose 页面（供 HorizontalPager 使用）。
 * 列表/网格无玻璃卡片折射；导航栏/标题栏玻璃仍在 MainScreen 层。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfPage(
    registerGotoTop: ((() -> Unit)?) -> Unit,
    registerBack: ((() -> Boolean)?) -> Unit,
    registerMenuAction: (((BookshelfMenuAction) -> Unit)?) -> Unit,
    onBookClick: (Book) -> Unit,
    onBookLongClick: (Book) -> Unit,
    onGroupLongClick: (BookGroup) -> Unit,
    onRefresh: (List<Book>, Boolean) -> Unit,
    isUpdate: (String) -> Boolean,
    bookshelfSort: Int,
    onRequestSort: () -> Unit,
    currentGroupId: Long,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LocalLifecycleOwner.current
    val activity = context as AppCompatActivity
    val coroutineScope = rememberCoroutineScope()

    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var bookshelfLayout by remember { mutableIntStateOf(AppConfig.bookshelfLayout) }
    var isRefreshing by remember { mutableStateOf(false) }
    val listScroll = rememberScrollState()

    LaunchedEffect(Unit) {
        if (bookshelfLayout == 1) {
            BookshelfMenuAction.ToggleLayout.titleRes = R.string.list_layout
            BookshelfMenuAction.ToggleLayout.iconRes = R.drawable.ic_chapter_list
        } else {
            BookshelfMenuAction.ToggleLayout.titleRes = R.string.grid_layout
            BookshelfMenuAction.ToggleLayout.iconRes = R.drawable.ic_view_quilt
        }
    }

    fun handleMenuAction(action: BookshelfMenuAction) {
        when (action) {
            BookshelfMenuAction.AddLocal ->
                activity.startActivity(Intent(activity, ImportBookActivity::class.java))
            BookshelfMenuAction.Remote ->
                activity.startActivity(Intent(activity, RemoteBookActivity::class.java))
            BookshelfMenuAction.BookshelfManage ->
                activity.startActivity(Intent(activity, BookshelfManageActivity::class.java).apply {
                    putExtra("groupId", BookGroup.IdAll)
                })
            BookshelfMenuAction.GroupManage ->
                activity.showDialogFragment<GroupManageDialog>()
            BookshelfMenuAction.Sort -> onRequestSort()
            BookshelfMenuAction.ToggleLayout -> {
                bookshelfLayout = if (bookshelfLayout == 1) 0 else 1
                AppConfig.bookshelfLayout = bookshelfLayout
                if (bookshelfLayout == 1) {
                    BookshelfMenuAction.ToggleLayout.titleRes = R.string.list_layout
                    BookshelfMenuAction.ToggleLayout.iconRes = R.drawable.ic_chapter_list
                } else {
                    BookshelfMenuAction.ToggleLayout.titleRes = R.string.grid_layout
                    BookshelfMenuAction.ToggleLayout.iconRes = R.drawable.ic_view_quilt
                }
            }
        }
    }

    DisposableEffect(Unit) {
        registerGotoTop {
            coroutineScope.launch { listScroll.scrollTo(0) }
        }
        registerBack { false }
        registerMenuAction { handleMenuAction(it) }
        onDispose {
            registerGotoTop(null)
            registerBack(null)
            registerMenuAction(null)
        }
    }

    LaunchedEffect(bookshelfSort, currentGroupId) {
        appDb.bookDao.flowByGroup(currentGroupId)
            .map { list ->
                when (bookshelfSort) {
                    1 -> list.sortedByDescending { it.latestChapterTime }
                    2 -> list.sortedWith { o1, o2 -> o1.name.cnCompare(o2.name) }
                    3 -> list.sortedBy { it.order }
                    4 -> list.sortedByDescending { max(it.latestChapterTime, it.durChapterTime) }
                    else -> list.sortedByDescending { it.durChapterTime }
                }
            }
            .conflate()
            .flowOn(Dispatchers.Default)
            .collect { sortedBooks -> books = sortedBooks }
    }

    val contentPadding = PaddingValues(
        start = 12.dp,
        top = BookshelfListTopPadding,
        end = 12.dp,
        bottom = 88.dp,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(context.backgroundColor)),
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    onRefresh(books, false)
                    delay(800)
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            if (books.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = context.getString(R.string.empty),
                        style = TextStyle(color = Color.Gray, fontSize = 16.sp),
                    )
                }
            } else if (bookshelfLayout == 1) {
                GlassBookshelfGrid(
                    books = books,
                    isUpdate = isUpdate,
                    onClick = onBookClick,
                    onLongClick = onBookLongClick,
                    contentPadding = contentPadding,
                )
            } else {
                GlassBookshelfList(
                    books = books,
                    isUpdate = isUpdate,
                    onClick = onBookClick,
                    onLongClick = onBookLongClick,
                    contentPadding = contentPadding,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

enum class BookshelfMenuAction(var titleRes: Int, var iconRes: Int) {
    AddLocal(R.string.book_local, R.drawable.ic_add),
    Remote(R.string.add_remote_book, R.drawable.ic_add),
    BookshelfManage(R.string.bookshelf_management, R.drawable.ic_arrange),
    GroupManage(R.string.group_manage, R.drawable.ic_groups),
    Sort(R.string.sort, R.drawable.ic_sort),
    ToggleLayout(R.string.bookshelf_layout, R.drawable.ic_view_quilt),
}
