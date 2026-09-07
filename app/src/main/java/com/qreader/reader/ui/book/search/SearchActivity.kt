package com.qreader.reader.ui.book.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.qreader.reader.R
import com.qreader.reader.base.VMBaseActivity
import com.qreader.reader.constant.PreferKey
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.BookSource
import com.qreader.reader.data.entities.BookSourcePart
import com.qreader.reader.databinding.ActivityBookSearchBinding
import com.qreader.reader.lib.dialogs.alert
import com.qreader.reader.ui.book.info.BookInfoActivity
import com.qreader.reader.ui.book.source.manage.BookSourceActivity
import com.qreader.reader.utils.getPrefBoolean
import com.qreader.reader.utils.putPrefBoolean
import com.qreader.reader.utils.showDialogFragment
import com.qreader.reader.utils.startActivity
import com.qreader.reader.utils.transaction
import com.qreader.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

class SearchActivity : VMBaseActivity<ActivityBookSearchBinding, SearchViewModel>(),
    SearchScopeDialog.Callback {

    override val binding by viewBinding(ActivityBookSearchBinding::inflate)
    override val viewModel by viewModels<SearchViewModel>()

    private var menu: Menu? = null
    private var groups: List<String>? = null
    private var precisionSearchMenuItem: MenuItem? = null
    private var isManualStopSearch = false

    // Compose 状态
    private var showScopeDialog by mutableStateOf(false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initComposeView()
        initData()
        receiptIntent(intent)
    }

    private fun initComposeView() {
        binding.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.composeView.setContent {
            SearchScreen(
                viewModel = viewModel,
                onBookClick = { name, author, bookUrl ->
                    startActivity<BookInfoActivity> {
                        putExtra("name", name)
                        putExtra("author", author)
                        putExtra("bookUrl", bookUrl)
                    }
                },
                onBookshelfBookClick = { book ->
                    startActivity<BookInfoActivity> {
                        putExtra("name", book.name)
                        putExtra("author", book.author)
                        putExtra("bookUrl", book.bookUrl)
                    }
                },
                onSearchScopeClick = {
                    alertSearchScope()
                },
                onSourceManageClick = {
                    startActivity<BookSourceActivity>()
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        receiptIntent(intent)
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_search, menu)
        this.menu = menu
        precisionSearchMenuItem = menu.findItem(R.id.menu_precision_search)
        precisionSearchMenuItem?.isChecked = getPrefBoolean(PreferKey.precisionSearch)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.transaction {
            menu.removeGroup(R.id.menu_group_1)
            menu.removeGroup(R.id.menu_group_2)
            var hasChecked = false
            val searchScopeNames = viewModel.searchScope.displayNames
            if (viewModel.searchScope.isSource()) {
                menu.add(R.id.menu_group_1, Menu.NONE, Menu.NONE, searchScopeNames.first()).apply {
                    isChecked = true
                    hasChecked = true
                }
            }
            val allSourceMenu =
                menu.add(R.id.menu_group_2, R.id.menu_1, Menu.NONE, getString(R.string.all_source))
                    .apply {
                        if (searchScopeNames.isEmpty()) {
                            isChecked = true
                            hasChecked = true
                        }
                    }
            groups?.forEach {
                if (searchScopeNames.contains(it)) {
                    menu.add(R.id.menu_group_1, Menu.NONE, Menu.NONE, it).apply {
                        isChecked = true
                        hasChecked = true
                    }
                } else {
                    menu.add(R.id.menu_group_2, Menu.NONE, Menu.NONE, it)
                }
            }
            if (!hasChecked) {
                viewModel.searchScope.update("")
                allSourceMenu.isChecked = true
            }
            menu.setGroupCheckable(R.id.menu_group_1, true, false)
            menu.setGroupCheckable(R.id.menu_group_2, true, true)
        }
        return super.onMenuOpened(featureId, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_precision_search -> {
                putPrefBoolean(
                    PreferKey.precisionSearch,
                    !getPrefBoolean(PreferKey.precisionSearch)
                )
                precisionSearchMenuItem?.isChecked = getPrefBoolean(PreferKey.precisionSearch)
            }

            R.id.menu_search_scope -> alertSearchScope()
            R.id.menu_source_manage -> startActivity<BookSourceActivity>()
            R.id.menu_1 -> viewModel.searchScope.update("")
            else -> {
                if (item.groupId == R.id.menu_group_1) {
                    viewModel.searchScope.remove(item.title.toString())
                } else if (item.groupId == R.id.menu_group_2) {
                    viewModel.searchScope.update(item.title.toString())
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initData() {
        lifecycleScope.launch {
            appDb.bookSourceDao.flowEnabledGroups().collect {
                groups = it
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.resume()
                try {
                    awaitCancellation()
                } finally {
                    viewModel.pause()
                }
            }
        }
    }

    /**
     * 处理传入数据
     */
    private fun receiptIntent(intent: Intent? = null) {
        val searchScope = intent?.getStringExtra("searchScope")
        searchScope?.let {
            viewModel.searchScope.update(searchScope, postValue = false, save = false)
        }
        val key = intent?.getStringExtra("key")
        if (!key.isNullOrBlank()) {
            viewModel.saveSearchKey(key)
            viewModel.searchKey = ""
            viewModel.search(key)
        }
    }

    override fun onSearchScopeOk(searchScope: SearchScope) {
        viewModel.searchScope.update(searchScope.toString())
    }

    private fun alertSearchScope() {
        showDialogFragment<SearchScopeDialog>()
    }

    override fun finish() {
        super.finish()
    }

    companion object {

        fun start(context: Context, key: String?, searchScope: String? = null) {
            context.startActivity<SearchActivity> {
                putExtra("key", key)
                putExtra("searchScope", searchScope)
            }
        }

        fun start(context: Context, source: BookSource, key: String? = null) {
            context.startActivity<SearchActivity> {
                putExtra("key", key)
                putExtra("searchScope", SearchScope(source).toString())
            }
        }

        fun start(context: Context, source: BookSourcePart, key: String? = null) {
            context.startActivity<SearchActivity> {
                putExtra("key", key)
                putExtra("searchScope", SearchScope(source).toString())
            }
        }

    }
}
