package com.qreader.reader.ui.main

import android.content.Intent
import android.graphics.Rect
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
    bookshelfSort: Int,
    onRequestSort: () -> Unit,
    onRequestGroups: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as AppCompatActivity

    // ── 状态 ──
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var itemCount by remember { mutableIntStateOf(0) }

    var bookshelfLayout by remember { mutableIntStateOf(AppConfig.bookshelfLayout) }
    val bookshelfMargin = 12

    // 初始化布局图标与标题
    LaunchedEffect(Unit) {
        if (bookshelfLayout == 1) {
            BookshelfMenuAction.ToggleLayout.titleRes = R.string.list_layout
            BookshelfMenuAction.ToggleLayout.iconRes = R.drawable.ic_chapter_list
        } else {
            BookshelfMenuAction.ToggleLayout.titleRes = R.string.grid_layout
            BookshelfMenuAction.ToggleLayout.iconRes = R.drawable.ic_view_quilt
        }
    }

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

    // ── Adapter（只在 bookshelfLayout 变化时重建）──
    val adapter = remember(bookshelfLayout) {
        if (bookshelfLayout == 1) BooksAdapterGrid(context, callBack)
        else BooksAdapterList(context, callBack)
    }

    val recyclerViewRef = remember { mutableStateOf<RecyclerView?>(null) }
    val swipeRefreshRef = remember { mutableStateOf<SwipeRefreshLayout?>(null) }

    // ── 菜单处理 ──
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
            recyclerViewRef.value?.let { rv ->
                if (AppConfig.isEInkMode) rv.scrollToPosition(0) else rv.smoothScrollToPosition(0)
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

    // ── 数据 → adapter（adapter 也作为 key，切换布局时重新推送数据）──
    LaunchedEffect(books, adapter) {
        currentItems.clear()
        currentItems.addAll(books)
        itemCount = currentItems.size
        adapter.updateItems(BookGroup.IdAll)
        swipeRefreshRef.value?.isEnabled = itemCount > 0
    }

    // ── UI ──
    Box(
        modifier = modifier
            .fillMaxSize()
            // 右边缘滑出分组抽屉：从右侧 30dp 内起始、左滑超过 50dp 触发
            .pointerInput(Unit) {
                val edgePx = 30.dp.toPx()
                val thresholdPx = 50.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(false)
                    if (down.position.x < size.width - edgePx) return@awaitEachGesture
                    var totalDx = 0f
                    val upOrCancel = edgeDrag(
                        pointerId = down.id,
                        onDrag = { change ->
                            totalDx += change.positionChange().x
                        },
                    )
                    if (upOrCancel != null && totalDx < -thresholdPx) {
                        onRequestGroups()
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                key(bookshelfLayout) {
                    AndroidView(
                        factory = { ctx ->
                            SwipeRefreshLayout(ctx).apply {
                                swipeRefreshRef.value = this
                                setColorSchemeColors(ctx.accentColor)
                                val refreshTopPx = 110.dpToPx(ctx).toInt()
                                setProgressViewOffset(false, refreshTopPx, 150.dpToPx(ctx).toInt())
                                setOnRefreshListener {
                                    isRefreshing = false
                                    onRefresh(books, false)
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

                                    layoutManager = if (bookshelfLayout == 1) GridLayoutManager(ctx, spanCount)
                                    else LinearLayoutManager(ctx)
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

                if (itemCount == 0) {
                    BasicText(
                        text = context.getString(R.string.empty),
                        modifier = Modifier.align(Alignment.Center),
                        style = TextStyle(color = Color.Gray, fontSize = 16.sp)
                    )
                }
            }
        }
    }
}

private fun Int.dpToPx(context: android.content.Context): Float {
    return this * context.resources.displayMetrics.density
}

/**
 * 拖拽跟踪：从 pointerId 开始持续跟踪拖拽，直到抬手返回 PointerInputChange，
 * 取消返回 null。与 DragGestureInspector.drag() 同模式。
 */
private suspend fun AwaitPointerEventScope.edgeDrag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == pointer } ?: return null
        if (change.changedToUpIgnoreConsumed()) return change
        val moved = change.positionChange()
        if (moved.x != 0f || moved.y != 0f) onDrag(change)
        pointer = change.id
    }
}

enum class BookshelfMenuAction(var titleRes: Int, var iconRes: Int) {
    AddLocal(R.string.book_local, R.drawable.ic_add),
    Remote(R.string.add_remote_book, R.drawable.ic_add),
    BookshelfManage(R.string.bookshelf_management, R.drawable.ic_arrange),
    Sort(R.string.sort, R.drawable.ic_sort),
    ToggleLayout(R.string.bookshelf_layout, R.drawable.ic_view_quilt),
}
