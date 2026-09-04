package com.qreader.reader.ui.main

import android.content.Intent
import android.graphics.Rect
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.qreader.reader.R
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.Book
import com.qreader.reader.data.entities.BookGroup
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.lib.theme.accentColor
import com.qreader.reader.lib.theme.backgroundColor
import com.qreader.reader.lib.theme.primaryColor
import com.qreader.reader.ui.book.import.local.ImportBookActivity
import com.qreader.reader.ui.book.import.remote.RemoteBookActivity
import com.qreader.reader.ui.book.manage.BookshelfManageActivity
import com.qreader.reader.ui.main.bookshelf.BookshelfViewModel
import com.qreader.reader.ui.main.bookshelf.style.BaseBooksAdapter
import com.qreader.reader.ui.main.bookshelf.style.BooksAdapterGrid
import com.qreader.reader.ui.main.bookshelf.style.BooksAdapterList
import com.qreader.reader.utils.ColorUtils
import com.qreader.reader.utils.cnCompare
import com.qreader.reader.utils.setEdgeEffectColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.math.max

/**
 * 书架页 —— Compose 页面（供 HorizontalPager 使用）。
 *
 * 使用 AndroidView 桥接现有 RecyclerView + BooksAdapter，保留原有列表渲染逻辑。
 * 数据层（bookGroups / books / sorting）在 Compose 中管理，通过 adapter.updateItems() 更新。
 */
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
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as AppCompatActivity

    // ── 状态 ──
    var bookGroups by remember { mutableStateOf<List<BookGroup>>(emptyList()) }
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var groupId by remember { mutableLongStateOf(BookGroup.IdAll) }
    var enableRefresh by remember { mutableStateOf(true) }
    var onlyUpdateRead by remember { mutableStateOf(false) }
    var itemCount by remember { mutableIntStateOf(0) }

    // 可变布局状态：0=列表, 1=网格
    var bookshelfLayout by remember { mutableIntStateOf(AppConfig.bookshelfLayout) }
    val bookshelfMargin = 12

    // 排序弹框状态
    var showSortDialog by remember { mutableStateOf(false) }
    var bookshelfSort by remember { mutableIntStateOf(AppConfig.bookshelfSort) }

    // 初始化布局图标
    LaunchedEffect(Unit) {
        BookshelfMenuAction.ToggleLayout.iconRes =
            if (bookshelfLayout == 1) R.drawable.ic_chapter_list
            else R.drawable.ic_view_quilt
    }

    // 顶栏菜单所需
    val bookshelfViewModel = remember { ViewModelProvider(activity)[BookshelfViewModel::class.java] }
    var showAddUrlDialog by remember { mutableStateOf(false) }
    var addUrlText by remember { mutableStateOf("") }

    // adapter.getItems() 需要的当前列表快照
    val currentItems = remember { mutableListOf<Any>() }

    // ── Adapter CallBack ──
    val callBack = remember {
        object : BaseBooksAdapter.CallBack {
            override fun onItemClick(item: Any) {
                when (item) {
                    is Book -> onBookClick(item)
                }
            }

            override fun onItemLongClick(item: Any) {
                when (item) {
                    is Book -> onBookLongClick(item)
                    is BookGroup -> onGroupLongClick(item)
                }
            }

            override fun isUpdate(bookUrl: String): Boolean = isUpdate(bookUrl)

            override fun getItems(): List<Any> = currentItems.toList()
        }
    }

    // ── 创建 Adapter（只在 bookshelfLayout 变化时重建）──
    val adapter = remember(bookshelfLayout) {
        if (bookshelfLayout == 1) {
            BooksAdapterGrid(context, callBack)
        } else {
            BooksAdapterList(context, callBack)
        }
    }

    // RecyclerView 引用（供 gotoTop / back 使用）
    val recyclerViewRef = remember { mutableStateOf<RecyclerView?>(null) }
    val swipeRefreshRef = remember { mutableStateOf<SwipeRefreshLayout?>(null) }

    // ── 顶栏菜单项处理 ──
    fun handleMenuAction(action: BookshelfMenuAction) {
        when (action) {
            BookshelfMenuAction.UpdateToc ->
                onRefresh(books, onlyUpdateRead)
            BookshelfMenuAction.AddLocal ->
                activity.startActivity(Intent(activity, ImportBookActivity::class.java))
            BookshelfMenuAction.Remote ->
                activity.startActivity(Intent(activity, RemoteBookActivity::class.java))
            BookshelfMenuAction.AddUrl ->
                showAddUrlDialog = true
            BookshelfMenuAction.BookshelfManage ->
                activity.startActivity(
                    Intent(activity, BookshelfManageActivity::class.java).apply {
                        putExtra("groupId", groupId)
                    }
                )
            BookshelfMenuAction.Sort ->
                showSortDialog = true
            BookshelfMenuAction.ToggleLayout -> {
                bookshelfLayout = if (bookshelfLayout == 1) 0 else 1
                AppConfig.bookshelfLayout = bookshelfLayout
                BookshelfMenuAction.ToggleLayout.iconRes =
                    if (bookshelfLayout == 1) R.drawable.ic_chapter_list
                    else R.drawable.ic_view_quilt
            }
        }
    }

    // ── 注册 gotoTop / back ──
    DisposableEffect(Unit) {
        registerGotoTop {
            recyclerViewRef.value?.let { rv ->
                if (AppConfig.isEInkMode) rv.scrollToPosition(0)
                else rv.smoothScrollToPosition(0)
            }
        }
        registerBack { false }
        registerMenuAction { handleMenuAction(it) }
        onDispose {
            registerGotoTop(null)
            registerBack(null)
            registerMenuAction(null)
        }
    }

    // ── 观察 BookGroups（LiveData）──
    val groupsLiveData = remember { appDb.bookGroupDao.show }
    DisposableEffect(lifecycleOwner) {
        val observer = Observer<List<BookGroup>> { groups -> bookGroups = groups }
        groupsLiveData.observe(lifecycleOwner, observer)
        onDispose { groupsLiveData.removeObserver(observer) }
    }

    // ── 观察 Books（Flow + 排序）──
    LaunchedEffect(bookshelfSort) {
        appDb.bookDao.flowByGroup(BookGroup.IdAll)
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

    // ── 数据变化 → 更新 adapter ──
    LaunchedEffect(books) {
        currentItems.clear()
        currentItems.addAll(books)
        itemCount = currentItems.size
        adapter.updateItems(BookGroup.IdAll)
        swipeRefreshRef.value?.isEnabled = enableRefresh && itemCount > 0
    }

    // ── UI ──
    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            // key(bookshelfLayout) 强制布局切换时重建整个 AndroidView
            key(bookshelfLayout) {
                AndroidView(
                    factory = { ctx ->
                        SwipeRefreshLayout(ctx).apply {
                            swipeRefreshRef.value = this
                            setColorSchemeColors(ctx.accentColor)
                            val refreshTopPx = 110.dpToPx(ctx).toInt()
                            setProgressViewOffset(false, refreshTopPx, (150).dpToPx(ctx).toInt())
                            setOnRefreshListener {
                                isRefreshing = false
                                onRefresh(books, onlyUpdateRead)
                            }
                            addView(RecyclerView(ctx).apply {
                                recyclerViewRef.value = this
                                setEdgeEffectColor(ctx.primaryColor)
                                clipToPadding = false
                                setPadding(0, 110.dpToPx(ctx).toInt(), 0, 72.dpToPx(ctx).toInt())

                                val spanCount = if (bookshelfLayout == 1) {
                                    val screenWidthDp = ctx.resources.displayMetrics.widthPixels / ctx.resources.displayMetrics.density
                                    (screenWidthDp / 100f).toInt().coerceIn(3, 6)
                                } else 1

                                layoutManager = if (bookshelfLayout == 1) {
                                    GridLayoutManager(ctx, spanCount)
                                } else {
                                    LinearLayoutManager(ctx)
                                }
                                this.adapter = adapter
                                itemAnimator = null

                                addItemDecoration(object : RecyclerView.ItemDecoration() {
                                    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                                        val position = parent.getChildAdapterPosition(view)
                                        if (bookshelfLayout == 1) {
                                            val rowIndex = position / spanCount
                                            val totalRows = if (itemCount % spanCount == 0) itemCount / spanCount else itemCount / spanCount + 1
                                            when (rowIndex) {
                                                0 -> outRect.set(bookshelfMargin, bookshelfMargin, bookshelfMargin, bookshelfMargin)
                                                totalRows - 1 -> outRect.set(bookshelfMargin, bookshelfMargin, bookshelfMargin, bookshelfMargin + 24)
                                                else -> outRect.set(bookshelfMargin, bookshelfMargin, bookshelfMargin, bookshelfMargin)
                                            }
                                        } else {
                                            when (position) {
                                                0 -> outRect.set(0, bookshelfMargin, 0, bookshelfMargin)
                                                itemCount - 1 -> outRect.set(0, bookshelfMargin, 0, bookshelfMargin + 24)
                                                else -> outRect.set(0, bookshelfMargin, 0, bookshelfMargin)
                                            }
                                        }
                                    }
                                })
                            })
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 空状态
            if (itemCount == 0) {
                BasicText(
                    text = context.getString(R.string.empty),
                    modifier = Modifier.align(Alignment.Center),
                    style = TextStyle(color = Color.Gray, fontSize = 16.sp)
                )
            }
        }

        // 添加网址对话框
        if (showAddUrlDialog) {
            AlertDialog(
                onDismissRequest = { showAddUrlDialog = false },
                title = { BasicText(stringResource(R.string.add_url), style = TextStyle(fontSize = 18.sp)) },
                text = {
                    TextField(
                        value = addUrlText,
                        onValueChange = { addUrlText = it },
                        label = { BasicText("url") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        bookshelfViewModel.addBookByUrl(addUrlText)
                        showAddUrlDialog = false
                        addUrlText = ""
                    }) { BasicText(stringResource(R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showAddUrlDialog = false }) {
                        BasicText(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }

    // 排序弹框：使用 Material3 AlertDialog（稳定，无 backdrop 依赖）
    if (showSortDialog) {
        val sortOptions = listOf(
            R.string.bookshelf_px_0 to 0,
            R.string.bookshelf_px_1 to 1,
            R.string.bookshelf_px_2 to 2,
            R.string.bookshelf_px_3 to 3,
            R.string.bookshelf_px_4 to 4,
            R.string.bookshelf_px_5 to 5,
        )
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { BasicText(stringResource(R.string.sort), style = TextStyle(fontSize = 18.sp)) },
            text = {
                Column {
                    sortOptions.forEach { (resId, sortIndex) ->
                        val isSelected = bookshelfSort == sortIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = isSelected,
                                    onClick = {
                                        bookshelfSort = sortIndex
                                        AppConfig.bookshelfSort = sortIndex
                                        showSortDialog = false
                                    },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = Color(context.accentColor)),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicText(
                                text = stringResource(resId),
                                style = TextStyle(fontSize = 15.sp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) {
                    BasicText(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private fun Int.dpToPx(context: android.content.Context): Float {
    return this * context.resources.displayMetrics.density
}

/**
 * 书架页顶栏「更多选项」菜单项
 */
enum class BookshelfMenuAction(val titleRes: Int, var iconRes: Int) {
    UpdateToc(R.string.update_toc, R.drawable.ic_refresh_black_24dp),
    AddLocal(R.string.book_local, R.drawable.ic_add),
    Remote(R.string.add_remote_book, R.drawable.ic_add),
    AddUrl(R.string.add_url, R.drawable.ic_add_online),
    BookshelfManage(R.string.bookshelf_management, R.drawable.ic_arrange),
    Sort(R.string.sort, R.drawable.ic_sort),
    ToggleLayout(R.string.bookshelf_layout, R.drawable.ic_view_quilt),
}
