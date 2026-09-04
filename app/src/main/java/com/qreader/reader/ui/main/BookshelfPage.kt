package com.qreader.reader.ui.main

import android.content.Intent
import android.graphics.Rect
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog
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
 * 书架页（实验副本 BookshelfPage）—— 由 BookshelfPage 复制而来，原页面保留作兜底。
 * 后续所有分组/封面相关改动只动本文件；若改坏，删除本文件并把 MainActivity 的调用切回 BookshelfPage 即可。
 * 注意：BookshelfMenuAction 枚举沿用原 BookshelfPage.kt 中的定义（同 package，不在此重复声明）。
 */

/**
 * 书架页 —— Compose 页面（供 HorizontalPager 使用）。
 *
 * 使用 AndroidView 桥接现有 RecyclerView + BooksAdapter，保留原有列表渲染逻辑。
 * 数据层（bookGroups / books / sorting）在 Compose 中管理，通过 adapter.updateItems() 更新。
 *
 * 顶栏（搜索 + 更多选项）与原版 legado-E BookshelfFragment 的 TitleBar + R.menu.main_bookshelf 一致。
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
    backdrop: com.kyant.backdrop.Backdrop,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as AppCompatActivity

    // ── 状态 ──
    var bookGroups by remember { mutableStateOf<List<BookGroup>>(emptyList()) }
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    // 固定为全部书籍，不再支持分组导航
    var groupId by remember { mutableLongStateOf(BookGroup.IdAll) }
    var enableRefresh by remember { mutableStateOf(true) }
    var onlyUpdateRead by remember { mutableStateOf(false) }
    var itemCount by remember { mutableIntStateOf(0) }

    // 可变布局状态：0=列表, 1=网格，切换时自动重建 adapter
    var bookshelfLayout by remember { mutableIntStateOf(AppConfig.bookshelfLayout) }
    val bookshelfMargin = 12 // 固定12dp

    // 排序弹框状态
    var showSortDialog by remember { mutableStateOf(false) }
    var bookshelfSort by remember { mutableIntStateOf(AppConfig.bookshelfSort) }

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

    // ── 顶栏「更多选项」菜单项处理（与原版 onCompatOptionsItemSelected 对应）──
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
        registerBack {
            false // 不处理返回键（分组导航已移除）
        }
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
    // bookshelfSort 作为 key：排序变化时重新收集并排序
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
        // 更新空状态 & 下拉刷新开关
        swipeRefreshRef.value?.isEnabled = enableRefresh && itemCount > 0
    }

    // ── UI ──
    // 标题栏背景：与书架内容区（页面主题背景 backgroundColor）保持一致，文字色随背景深浅反色
    val barBgColor = Color(context.backgroundColor)
    val barContentColor = if (ColorUtils.isColorLight(context.backgroundColor)) Color.Black else Color.White
    val barAccentColor = Color(context.accentColor)
    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { ctx ->
                    // SwipeRefreshLayout > RecyclerView
                    SwipeRefreshLayout(ctx).apply {
                        swipeRefreshRef.value = this
                        setColorSchemeColors(ctx.accentColor)
                        // 下拉刷新圆环置于标题栏下方（110dp），与内容区 top padding 对齐
                        val refreshTopPx = 110.dpToPx(ctx).toInt()
                        setProgressViewOffset(
                            false,
                            refreshTopPx,
                            (110 + 40).dpToPx(ctx).toInt()
                        )
                        setOnRefreshListener {
                            isRefreshing = false
                            onRefresh(books, onlyUpdateRead)
                        }
                        addView(RecyclerView(ctx).apply {
                            recyclerViewRef.value = this
                            setEdgeEffectColor(ctx.primaryColor)
                            clipToPadding = false
                            setPadding(0, 110.dpToPx(ctx).toInt(), 0, 72.dpToPx(ctx).toInt())
                            // 根据屏幕宽度自动计算网格列数
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
                            // 与原 BookshelfFragment2 一致的 itemDecoration
                            addItemDecoration(object : RecyclerView.ItemDecoration() {
                                override fun getItemOffsets(
                                    outRect: Rect,
                                    view: View,
                                    parent: RecyclerView,
                                    state: RecyclerView.State
                                ) {
                                    val position = parent.getChildAdapterPosition(view)
                                    if (bookshelfLayout == 1) {
                                        val rowIndex = position / spanCount
                                        val totalRows =
                                            if (itemCount % spanCount == 0) itemCount / spanCount
                                            else itemCount / spanCount + 1
                                        when (rowIndex) {
                                            0 -> outRect.set(
                                                bookshelfMargin,
                                                bookshelfMargin,
                                                bookshelfMargin,
                                                bookshelfMargin
                                            )

                                            totalRows - 1 -> outRect.set(
                                                bookshelfMargin,
                                                bookshelfMargin,
                                                bookshelfMargin,
                                                bookshelfMargin + 24
                                            )

                                            else -> outRect.set(
                                                bookshelfMargin,
                                                bookshelfMargin,
                                                bookshelfMargin,
                                                bookshelfMargin
                                            )
                                        }
                                    } else {
                                        when (position) {
                                            0 -> outRect.set(
                                                0,
                                                bookshelfMargin,
                                                0,
                                                bookshelfMargin
                                            )

                                            itemCount - 1 -> outRect.set(
                                                0,
                                                bookshelfMargin,
                                                0,
                                                bookshelfMargin + 24
                                            )

                                            else -> outRect.set(
                                                0,
                                                bookshelfMargin,
                                                0,
                                                bookshelfMargin
                                            )
                                        }
                                    }
                                }
                            })
                        })
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 空状态
            if (itemCount == 0) {
                BasicText(
                    text = context.getString(R.string.empty),
                    modifier = Modifier.align(Alignment.Center),
                    style = TextStyle(
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                )
            }
        }

        // 添加网址对话框
        if (showAddUrlDialog) {
            AlertDialog(
                onDismissRequest = { showAddUrlDialog = false },
                title = {
                    BasicText(
                        stringResource(R.string.add_url),
                        style = TextStyle(fontSize = 18.sp)
                    )
                },
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

        // 排序玻璃态弹框
        if (showSortDialog) {
            LiquidGlassDialog(
                backdrop = backdrop,
                onDismiss = { showSortDialog = false },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
                contentPadding = PaddingValues(20.dp),
            ) { colors ->
                // 标题
                BasicText(
                    text = stringResource(R.string.sort),
                    style = TextStyle(
                        color = colors.contentColor,
                        fontSize = 18.sp,
                    ),
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                // 排序选项
                val sortOptions = listOf(
                    R.string.bookshelf_px_0 to 0,  // 按最近阅读
                    R.string.bookshelf_px_1 to 1,  // 按更新时间
                    R.string.bookshelf_px_2 to 2,  // 按书名
                    R.string.bookshelf_px_3 to 3,  // 手动排序
                    R.string.bookshelf_px_4 to 4,  // 综合排序
                    R.string.bookshelf_px_5 to 5,  // 按作者
                )
                sortOptions.forEach { (resId, sortIndex) ->
                    val isSelected = bookshelfSort == sortIndex
                    TextButton(
                        onClick = {
                            bookshelfSort = sortIndex
                            AppConfig.bookshelfSort = sortIndex
                            showSortDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        BasicText(
                            text = stringResource(resId),
                            style = TextStyle(
                                color = if (isSelected) colors.accentColor else colors.contentColor,
                                fontSize = 15.sp,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun Int.dpToPx(context: android.content.Context): Float {
    return this * context.resources.displayMetrics.density
}

/**
 * 书架页顶栏「更多选项」菜单项（与原版 R.menu.main_bookshelf 的溢出菜单一致）
 */
enum class BookshelfMenuAction(val titleRes: Int, val iconRes: Int) {
    UpdateToc(R.string.update_toc, R.drawable.ic_refresh_black_24dp),
    AddLocal(R.string.book_local, R.drawable.ic_add),
    Remote(R.string.add_remote_book, R.drawable.ic_add),
    AddUrl(R.string.add_url, R.drawable.ic_add_online),
    BookshelfManage(R.string.bookshelf_management, R.drawable.ic_arrange),
    Sort(R.string.sort, R.drawable.ic_sort),
    ToggleLayout(R.string.bookshelf_layout, R.drawable.ic_view_quilt),
}
