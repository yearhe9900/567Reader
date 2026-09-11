package com.qreader.reader.ui.book.bookmark

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import com.qreader.reader.R
import com.qreader.reader.base.VMBaseActivity
import com.qreader.reader.constant.AppLog
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.Bookmark
import com.qreader.reader.databinding.ActivityAllBookmarkBinding
import com.qreader.reader.ui.file.HandleFileContract
import com.qreader.reader.utils.showDialogFragment
import com.qreader.reader.utils.startActivityForBook
import com.qreader.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 所有书签
 */
class AllBookmarkActivity : VMBaseActivity<ActivityAllBookmarkBinding, AllBookmarkViewModel>() {

    override val viewModel by viewModels<AllBookmarkViewModel>()
    override val binding by viewBinding(ActivityAllBookmarkBinding::inflate)

    private val exportDir = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            when (it.requestCode) {
                1 -> viewModel.exportBookmark(uri)
                2 -> viewModel.exportBookmarkMd(uri)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.composeView.setContent {
            val bookmarks by appDb.bookmarkDao.flowAll()
                .catch {
                    AppLog.put("所有书签界面获取数据失败\n${it.localizedMessage}", it)
                }
                .flowOn(IO)
                .collectAsState(initial = emptyList())

            AllBookmarkScreen(
                title = getString(R.string.all_bookmark),
                bookmarks = bookmarks,
                onBack = { finish() },
                onExportTxt = { exportDir.launch { requestCode = 1 } },
                onExportMd = { exportDir.launch { requestCode = 2 } },
                onItemClick = ::onItemClick,
                onItemLongClick = ::onItemLongClick,
            )
        }
    }

    private fun onItemClick(bookmark: Bookmark, position: Int) {
        lifecycleScope.launch {
            val book = withContext(IO) {
                appDb.bookDao.getBook(bookmark.bookName, bookmark.bookAuthor)
            }
            if (book == null) {
                showDialogFragment(BookmarkDialog(bookmark, position))
            } else {
                startActivityForBook(book) {
                    putExtra("index", bookmark.chapterIndex)
                    putExtra("chapterPos", bookmark.chapterPos)
                }
            }
        }
    }

    private fun onItemLongClick(bookmark: Bookmark, position: Int) {
        showDialogFragment(BookmarkDialog(bookmark, position))
    }

}
