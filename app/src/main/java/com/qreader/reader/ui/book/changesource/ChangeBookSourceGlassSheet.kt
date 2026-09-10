package com.qreader.reader.ui.book.changesource

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kyant.backdrop.Backdrop
import com.qreader.reader.R
import com.qreader.reader.constant.EventBus
import com.qreader.reader.data.entities.Book
import com.qreader.reader.data.entities.BookChapter
import com.qreader.reader.data.entities.BookSource
import com.qreader.reader.data.entities.SearchBook
import com.qreader.reader.databinding.DialogBookChangeSourceBinding
import com.qreader.reader.help.book.isWebFile
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.lib.dialogs.alert
import com.qreader.reader.ui.book.read.ReadBookActivity
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog
import com.qreader.reader.ui.widget.dialog.WaitDialog
import com.qreader.reader.ui.widget.recycler.VerticalDivider
import com.qreader.reader.utils.observeEvent
import com.qreader.reader.utils.startActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 换源玻璃面板（阅读页 in-tree）。
 *
 * 复用 [ChangeBookSourceViewModel] + [ChangeBookSourceAdapter]；
 * 与 [ChangeBookSourceDialog] 功能对齐，宿主为 [LiquidGlassDialog]。
 */
@SuppressLint("CommitTransaction")
@Composable
fun ChangeBookSourceGlassSheet(
    backdrop: Backdrop,
    oldBook: Book?,
    onDismiss: () -> Unit,
    onChangeTo: (BookSource, Book, List<BookChapter>) -> Unit,
) {
    val activity = LocalContext.current as? ReadBookActivity ?: return
    val lifecycleOwner = activity as LifecycleOwner

    LiquidGlassDialog(
        backdrop = backdrop,
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .navigationBarsPadding()
            .padding(horizontal = GlassConfig.sheetHorizontalPadding),
        cardRadius = GlassConfig.sheetCornerRadius,
        alignment = Alignment.BottomCenter,
        showScrim = false,
    ) {
        AndroidView(
            factory = { ctx ->
                ChangeBookSourceHostView(
                    activity = activity,
                    oldBook = oldBook,
                    onDismiss = onDismiss,
                    onChangeTo = onChangeTo,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        )
    }

    DisposableEffect(Unit) {
        onDispose { }
    }
}

/**
 * 换源宿主 View：在 Activity 的 Fragment 容器里挂逻辑。
 * 使用独立 ViewModel（同一 Activity scope，与原 Dialog 一致）。
 */
private fun ChangeBookSourceHostView(
    activity: ReadBookActivity,
    oldBook: Book?,
    onDismiss: () -> Unit,
    onChangeTo: (BookSource, Book, List<BookChapter>) -> Unit,
): android.view.View {
    val ctx = activity
    val binding = DialogBookChangeSourceBinding.inflate(LayoutInflater.from(ctx))
    val viewModel = ViewModelProvider(activity)[ChangeBookSourceViewModel::class.java]
    val waitDialog = WaitDialog(ctx)
    val adapter = ChangeBookSourceAdapter(
        ctx,
        viewModel,
        object : ChangeBookSourceAdapter.CallBack {
            override val oldBookUrl: String? get() = oldBook?.bookUrl
            override fun changeTo(searchBook: SearchBook) {
                val oldType = oldBook?.type ?: 0
                if (searchBook.sameBookTypeLocal(oldType)) {
                    changeSourceInternal(viewModel, waitDialog, searchBook, oldBook, onChangeTo, onDismiss)
                } else {
                    ctx.alert(
                        titleResource = R.string.book_type_different,
                        messageResource = R.string.soure_change_source,
                    ) {
                        okButton {
                            changeSourceInternal(
                                viewModel, waitDialog, searchBook, oldBook, onChangeTo, onDismiss
                            )
                        }
                        cancelButton()
                    }
                }
            }
            override fun topSource(searchBook: SearchBook) {
                viewModel.topSource(searchBook)
            }
            override fun bottomSource(searchBook: SearchBook) {
                viewModel.bottomSource(searchBook)
            }
            override fun editSource(searchBook: SearchBook) {
                // 简化：跳转书源编辑
                ctx.startActivity<com.qreader.reader.ui.book.source.edit.BookSourceEditActivity> {
                    putExtra("sourceUrl", searchBook.origin)
                }
            }
            override fun disableSource(searchBook: SearchBook) {
                viewModel.disableSource(searchBook)
            }
            override fun deleteSource(searchBook: SearchBook) {
                viewModel.del(searchBook)
                if (oldBook?.bookUrl == searchBook.bookUrl) {
                    viewModel.autoChangeSource(oldBook.type) { book, toc, source ->
                        onChangeTo(source, book, toc)
                        onDismiss()
                    }
                }
            }
            override fun setBookScore(searchBook: SearchBook, score: Int) {
                viewModel.setBookScore(searchBook, score)
            }
            override fun getBookScore(searchBook: SearchBook): Int {
                return viewModel.getBookScore(searchBook)
            }
        },
    )

    binding.recyclerView.addItemDecoration(VerticalDivider(ctx))
    binding.recyclerView.adapter = adapter
    binding.recyclerView.layoutManager = LinearLayoutManager(ctx)
    binding.tvDur.text = oldBook?.originName
    binding.ivTop.setOnClickListener {
        binding.recyclerView.scrollToPosition(0)
    }
    binding.ivBottom.setOnClickListener {
        binding.recyclerView.scrollToPosition(adapter.itemCount - 1)
    }
    binding.tvDur.setOnClickListener {
        adapter.getItems().forEachIndexed { index, searchBook ->
            if (searchBook.bookUrl == oldBook?.bookUrl) {
                (binding.recyclerView.layoutManager as LinearLayoutManager)
                    .scrollToPositionWithOffset(index, 60)
                return@setOnClickListener
            }
        }
    }

    viewModel.initData(bundleOf("name" to oldBook?.name, "author" to oldBook?.author), oldBook, true)
    viewModel.startSearch()

    activity.lifecycleScope.launch {
        activity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.searchDataFlow.conflate().collect {
                adapter.setItems(it)
                delay(1000)
            }
        }
    }
    activity.lifecycleScope.launch {
        activity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.searchStateData.observe(activity) {
                binding.refreshProgressBar.isAutoLoading = it
            }
        }
    }

    // 包一层，便于后续扩展
    return FrameLayout(ctx).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        addView(binding.root)
    }
}

private fun changeSourceInternal(
    viewModel: ChangeBookSourceViewModel,
    waitDialog: WaitDialog,
    searchBook: SearchBook,
    oldBook: Book?,
    onChangeTo: (BookSource, Book, List<BookChapter>) -> Unit,
    onDismiss: () -> Unit,
) {
    val ctx = waitDialog.context
    waitDialog.setText(R.string.load_toc)
    waitDialog.show()
    val book = viewModel.bookMap[searchBook.primaryStr()] ?: searchBook.toBook()
    if (book.isWebFile) {
        val source = com.qreader.reader.data.appDb.bookSourceDao.getBookSource(book.origin)
        waitDialog.dismiss()
        if (source != null) {
            onChangeTo(source, book, emptyList())
            onDismiss()
        }
        return
    }
    val coroutine = viewModel.getToc(book, { toc, source ->
        waitDialog.dismiss()
        onChangeTo(source, book, toc)
        onDismiss()
    }, {
        waitDialog.dismiss()
        com.qreader.reader.constant.AppLog.put("换源获取目录出错\n$it", it, true)
    })
    waitDialog.setOnCancelListener {
        coroutine.cancel()
    }
}
