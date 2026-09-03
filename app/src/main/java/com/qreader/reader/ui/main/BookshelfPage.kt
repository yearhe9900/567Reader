package com.qreader.reader.ui.main

import android.content.Intent
import android.graphics.Rect
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.qreader.reader.ui.about.AppLogDialog
import com.qreader.reader.ui.book.cache.CacheActivity
import com.qreader.reader.ui.book.group.GroupManageDialog
import com.qreader.reader.ui.book.import.local.ImportBookActivity
import com.qreader.reader.ui.book.import.remote.RemoteBookActivity
import com.qreader.reader.ui.book.manage.BookshelfManageActivity
import com.qreader.reader.ui.main.bookshelf.BookshelfConfigDialog
import com.qreader.reader.ui.main.bookshelf.BookshelfViewModel
import com.qreader.reader.ui.main.bookshelf.style2.BaseBooksAdapter
import com.qreader.reader.ui.main.bookshelf.style2.BooksAdapterGrid
import com.qreader.reader.ui.main.bookshelf.style2.BooksAdapterList
import com.qreader.reader.utils.ColorUtils
import com.qreader.reader.utils.cnCompare
import com.qreader.reader.utils.setEdgeEffectColor
import com.qreader.reader.utils.showDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.File
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
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as AppCompatActivity

    // ── 状态 ──
    var bookGroups by remember { mutableStateOf<List<BookGroup>>(emptyList()) }
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var groupId by remember { mutableLongStateOf(BookGroup.IdRoot) }
    var enableRefresh by remember { mutableStateOf(true) }
    var onlyUpdateRead by remember { mutableStateOf(false) }
    var itemCount by remember { mutableIntStateOf(0) }

    val bookshelfLayout = remember { AppConfig.bookshelfLayout }
    val bookshelfMargin = remember { AppConfig.bookshelfMargin }

    // 书架分组展示样式：0=Tab，1=Folder（与 R.array.group_style 对应）
    val bookGroupStyle = remember { AppConfig.bookGroupStyle }

    // Tab 样式：根据 tabs 选择当前 groupId
    // 还原上次选中的分组（与 legado-E BookshelfFragment1.selectLastTab() 一致）
    var selectedTabIndex by remember { mutableIntStateOf(AppConfig.saveTabPosition.coerceAtLeast(0)) }

    // 顶栏菜单所需
    val bookshelfViewModel = remember { ViewModelProvider(activity)[BookshelfViewModel::class.java] }
    var showAddUrlDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var addUrlText by remember { mutableStateOf("") }
    var importText by remember { mutableStateOf("") }
    var exportTempFile by remember { mutableStateOf<File?>(null) }

    val importFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        kotlin.runCatching {
            activity.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.onSuccess { text ->
            text?.let { bookshelfViewModel.importBookshelf(it, groupId) }
        }
    }
    val exportFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        exportTempFile?.let { file ->
            kotlin.runCatching {
                activity.contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().copyTo(out)
                }
            }
        }
        exportTempFile = null
    }

    // adapter.getItems() 需要的当前列表快照
    val currentItems = remember { mutableListOf<Any>() }

    // ── Adapter CallBack ──
    val callBack = remember {
        object : BaseBooksAdapter.CallBack {
            override fun onItemClick(item: Any) {
                when (item) {
                    is Book -> onBookClick(item)
                    is BookGroup -> { groupId = item.groupId }
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
        if (bookshelfLayout >= 2) {
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
            BookshelfMenuAction.Download ->
                activity.startActivity(
                    Intent(activity, CacheActivity::class.java).apply {
                        putExtra("groupId", groupId)
                    }
                )
            BookshelfMenuAction.GroupManage ->
                activity.showDialogFragment<GroupManageDialog>()
            BookshelfMenuAction.Layout ->
                activity.showDialogFragment<BookshelfConfigDialog>()
            BookshelfMenuAction.Export ->
                bookshelfViewModel.exportBookshelf(books) { file ->
                    exportTempFile = file
                    exportFileLauncher.launch("bookshelf.json")
                }
            BookshelfMenuAction.Import ->
                showImportDialog = true
            BookshelfMenuAction.Log ->
                activity.showDialogFragment<AppLogDialog>()
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
            if (bookGroupStyle == 0 && selectedTabIndex != 0) {
                selectedTabIndex = 0
                AppConfig.saveTabPosition = 0
                val group = bookGroups.getOrNull(0)
                groupId = group?.groupId ?: BookGroup.IdRoot
                enableRefresh = group?.enableRefresh ?: true
                onlyUpdateRead = group?.onlyUpdateRead ?: false
                true
            } else if (groupId != BookGroup.IdRoot) {
                groupId = BookGroup.IdRoot
                true
            } else false
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
    LaunchedEffect(groupId) {
        appDb.bookDao.flowByGroup(groupId)
            .map { list ->
                when (AppConfig.getBookSortByGroupId(groupId)) {
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
    LaunchedEffect(bookGroups, books, groupId) {
        currentItems.clear()
        if (groupId == BookGroup.IdRoot) {
            currentItems.addAll(bookGroups)
        }
        currentItems.addAll(books)
        itemCount = currentItems.size
        adapter.updateItems(groupId)
        // 更新空状态 & 下拉刷新开关
        swipeRefreshRef.value?.isEnabled = enableRefresh && itemCount > 0
    }

    // Tab 样式：根据 tabs 选择当前 groupId（bookGroupStyle / selectedTabIndex 已在上方声明）
    LaunchedEffect(bookGroups, bookGroupStyle) {
        if (bookGroupStyle == 0 && bookGroups.isNotEmpty()) {
            val index = selectedTabIndex.coerceAtMost(bookGroups.lastIndex)
            if (index != selectedTabIndex) selectedTabIndex = index
            val group = bookGroups[index]
            groupId = group.groupId
            enableRefresh = group.enableRefresh
            onlyUpdateRead = group.onlyUpdateRead
        }
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
                            layoutManager = if (bookshelfLayout >= 2) {
                                GridLayoutManager(ctx, bookshelfLayout)
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
                                    if (bookshelfLayout >= 2) {
                                        val spanCount = bookshelfLayout
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

        // 导入书架对话框
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = {
                    BasicText(
                        stringResource(R.string.import_bookshelf),
                        style = TextStyle(fontSize = 18.sp)
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TextField(
                            value = importText,
                            onValueChange = { importText = it },
                            label = { BasicText("url/json") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TextButton(
                            onClick = {
                                importFileLauncher.launch(
                                    arrayOf("application/json", "text/plain", "*/*")
                                )
                            },
                            modifier = Modifier.padding(top = 8.dp),
                        ) { BasicText(stringResource(R.string.select_file)) }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        bookshelfViewModel.importBookshelf(importText, groupId)
                        showImportDialog = false
                        importText = ""
                    }) { BasicText(stringResource(R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        BasicText(stringResource(R.string.cancel))
                    }
                },
            )
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
    Download(R.string.cache_export, R.drawable.ic_download_line),
    GroupManage(R.string.group_manage, R.drawable.ic_groups),
    Layout(R.string.bookshelf_layout, R.drawable.ic_view_quilt),
    Export(R.string.export_bookshelf, R.drawable.ic_export),
    Import(R.string.import_bookshelf, R.drawable.ic_import),
    Log(R.string.log, R.drawable.ic_cfg_about),
}
