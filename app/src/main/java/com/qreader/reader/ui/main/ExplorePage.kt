package com.qreader.reader.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qreader.reader.R
import com.qreader.reader.data.AppDatabase
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.BookSourcePart
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.lib.theme.accentColor
import com.qreader.reader.lib.theme.backgroundColor
import com.qreader.reader.lib.theme.primaryColor
import com.qreader.reader.ui.main.explore.ExploreAdapter
import com.qreader.reader.ui.main.explore.ExploreDiffItemCallBack
import com.qreader.reader.ui.main.explore.ExploreViewModel
import com.qreader.reader.utils.ColorUtils
import com.qreader.reader.utils.flowWithLifecycleAndDatabaseChangeFirst
import com.qreader.reader.utils.setEdgeEffectColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * 发现页 —— Compose 页面（供 HorizontalPager 使用）。
 *
 * 使用 AndroidView 桥接现有 RecyclerView + ExploreAdapter。
 * ExploreAdapter 极其复杂（675行，含 FlexboxLayout 动态 View、JS 执行），
 * 用 AndroidView 保留其完整功能。
 *
 * toTop / deleteSource 由内部 ExploreViewModel 处理，
 * openExplore / editSource / searchBook 通过回调传给 Activity。
 */
@OptIn(FlowPreview::class)
@Composable
fun ExplorePage(
    registerCompress: ((() -> Unit)?) -> Unit,
    onOpenExplore: (String, String, String?) -> Unit,
    onEditSource: (String) -> Unit,
    onSearchBook: (BookSourcePart) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val composableScope = rememberCoroutineScope()
    val exploreViewModel: ExploreViewModel = ViewModelProvider(context as androidx.lifecycle.ViewModelStoreOwner)[ExploreViewModel::class.java]

    // ── 搜索状态 ──
    var searchQuery by remember { mutableStateOf("") }

    // ── 删除确认弹窗 ──
    var showDeleteDialog by remember { mutableStateOf<BookSourcePart?>(null) }

    // RecyclerView 引用
    val recyclerViewRef = remember { mutableStateOf<RecyclerView?>(null) }

    // ── Adapter CallBack ──
    val callBack = remember {
        object : ExploreAdapter.CallBack {
            override val scope: CoroutineScope get() = composableScope

            override fun scrollTo(pos: Int) {
                (recyclerViewRef.value?.layoutManager as? LinearLayoutManager)
                    ?.scrollToPositionWithOffset(pos, 0)
            }

            override fun openExplore(sourceUrl: String, title: String, exploreUrl: String?) =
                onOpenExplore(sourceUrl, title, exploreUrl)

            override fun editSource(sourceUrl: String) = onEditSource(sourceUrl)

            override fun toTop(source: BookSourcePart) {
                exploreViewModel.topSource(source)
            }

            override fun deleteSource(source: BookSourcePart) {
                showDeleteDialog = source
            }

            override fun searchBook(bookSource: BookSourcePart) = onSearchBook(bookSource)
        }
    }

    // ── 创建 Adapter ──
    val adapter = remember { ExploreAdapter(context, callBack) }

    // ── 注册 compressExplore ──
    DisposableEffect(Unit) {
        registerCompress {
            if (!adapter.compressExplore()) {
                recyclerViewRef.value?.let { rv ->
                    if (AppConfig.isEInkMode) rv.scrollToPosition(0)
                    else rv.smoothScrollToPosition(0)
                }
            }
        }
        onDispose { registerCompress(null) }
    }

    // ── 生命周期：upResumed / onPause ──
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adapter.upResumed(true)
                Lifecycle.Event.ON_PAUSE -> {
                    adapter.upResumed(false)
                    adapter.onPause()
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── 观察 Explore 数据（Flow + 数据库变更自动刷新）──
    LaunchedEffect(searchQuery) {
        val flow = when {
            searchQuery.isBlank() -> appDb.bookSourceDao.flowExplore()
            searchQuery.startsWith("group:") -> {
                val key = searchQuery.substringAfter("group:")
                appDb.bookSourceDao.flowGroupExplore(key)
            }

            else -> appDb.bookSourceDao.flowExplore(searchQuery)
        }
        flow.flowWithLifecycleAndDatabaseChangeFirst(
            lifecycleOwner.lifecycle,
            Lifecycle.State.RESUMED,
            AppDatabase.BOOK_SOURCE_TABLE_NAME
        )
            .catch { /* AppLog.put("发现界面更新数据出错", it) */ }
            .conflate()
            .flowOn(Dispatchers.IO)
            .collect { sources ->
                adapter.setItems(sources, ExploreDiffItemCallBack())
            }
    }

    // 标题栏背景：与书架内容区（页面主题背景 backgroundColor）保持一致，文字色随背景深浅反色
    val barBg = Color(context.backgroundColor)
    val barContentColor = if (ColorUtils.isColorLight(context.backgroundColor)) Color.Black else Color.White

    // ── UI ──
    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    RecyclerView(ctx).apply {
                        recyclerViewRef.value = this
                        setEdgeEffectColor(ctx.primaryColor)
                        layoutManager = LinearLayoutManager(ctx)
                        this.adapter = adapter
                        clipToPadding = false
                        val density = ctx.resources.displayMetrics.density
                        setPadding(0, (105 * density).toInt(), 0, (72 * density).toInt())
                        adapter.registerAdapterDataObserver(object :
                            RecyclerView.AdapterDataObserver() {
                            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                                if (positionStart == 0) {
                                    scrollToPosition(0)
                                }
                            }
                        })
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // ── 删除确认弹窗 ──
    showDeleteDialog?.let { source ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(context.getString(R.string.draw)) },
            text = {
                Text(
                    "${context.getString(R.string.sure_del)}\n${source.bookSourceName}"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    exploreViewModel.deleteSource(source)
                    showDeleteDialog = null
                }) {
                    Text(context.getString(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(context.getString(android.R.string.cancel))
                }
            }
        )
    }
}

/**
 * 简易搜索栏（对应原 SearchView）
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        textStyle = TextStyle(color = contentColor, fontSize = 14.sp),
        cursorBrush = SolidColor(Color(ctx.accentColor)),
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (query.isEmpty()) {
                    androidx.compose.foundation.text.BasicText(
                        text = "搜索书源…",
                        style = TextStyle(color = contentColor.copy(0.5f), fontSize = 14.sp)
                    )
                }
                innerTextField()
            }
        }
    )
}
