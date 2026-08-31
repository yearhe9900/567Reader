package com.qreader.reader.ui.rss.source.debug

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.qreader.reader.R
import com.qreader.reader.base.VMBaseActivity
import com.qreader.reader.databinding.ActivityRssSourceDebugBinding
import com.qreader.reader.help.source.sortUrls
import com.qreader.reader.lib.dialogs.selector
import com.qreader.reader.lib.theme.accentColor
import com.qreader.reader.lib.theme.primaryColor
import com.qreader.reader.ui.widget.dialog.TextDialog
import com.qreader.reader.utils.applyNavigationBarPadding
import com.qreader.reader.utils.setEdgeEffectColor
import com.qreader.reader.utils.showDialogFragment
import com.qreader.reader.utils.toastOnUi
import com.qreader.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.launch
import splitties.views.onClick
import splitties.views.onLongClick


class RssSourceDebugActivity : VMBaseActivity<ActivityRssSourceDebugBinding, RssSourceDebugModel>() {

    override val binding by viewBinding(ActivityRssSourceDebugBinding::inflate)
    override val viewModel by viewModels<RssSourceDebugModel>()

    private val adapter by lazy { RssSourceDebugAdapter(this) }
    private val searchView: androidx.appcompat.widget.SearchView by lazy {
        binding.titleBar.findViewById(R.id.search_view)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        initSearchView()
        viewModel.initData(intent.getStringExtra("key")) {
            initHelpView()
        }
        viewModel.observe { state, msg ->
            lifecycleScope.launch {
                adapter.addItem(msg)
                if (state == -1 || state == 1000) {
                    binding.rotateLoading.gone()
                }
            }
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.rss_source_debug, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_list_src -> showDialogFragment(TextDialog("Html", viewModel.listSrc))
            R.id.menu_content_src -> showDialogFragment(TextDialog("Html", viewModel.contentSrc))
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initRecyclerView() {
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.applyNavigationBarPadding()
        binding.rotateLoading.loadingColor = accentColor
    }

    private fun initSearchView() {
        openOrCloseHelp(true)
        searchView.onActionViewExpanded()
        searchView.isSubmitButtonEnabled = true
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                openOrCloseHelp(false)
                startSearch(query ?: "我的")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            openOrCloseHelp(hasFocus)
        }
    }
    @SuppressLint("SetTextI18n")
    private fun initHelpView() {
        binding.textMy.onClick {
            searchView.setQuery(binding.textMy.text, true)
        }
        binding.textXt.onClick {
            searchView.setQuery(binding.textXt.text, true)
        }
        binding.textFl.onClick {
            if (!binding.textFl.text.startsWith("ERROR:")) {
                searchView.setQuery(binding.textFl.text, true)
            }
        }
        binding.textContent.onClick {
            if (!searchView.query.isNullOrBlank()) {
                searchView.setQuery(searchView.query, true)
            }
        }
        initSortKinds()
    }

    private fun initSortKinds() {
        lifecycleScope.launch {
            val sortKinds = viewModel.rssSource?.sortUrls()?.filter {
                it.second.isNotBlank()
            }
            sortKinds?.firstOrNull()?.let {
                binding.textFl.text = "${it.first}::${it.second}"
                if (it.first.startsWith("ERROR:")) {
                    adapter.addItem("获取发现出错\n${it.second}")
                    openOrCloseHelp(false)
                    searchView.clearFocus()
                    return@launch
                }
            }
            @Suppress("USELESS_ELVIS")
            sortKinds?.map { it.first ?: "" }?.let { sortKindTitles ->
                binding.textFl.onLongClick {
                    selector("选择分类", sortKindTitles) { _, index ->
                        val sort = sortKinds[index]
                        binding.textFl.text = "${sort.first}::${sort.second}"
                        searchView.setQuery(binding.textFl.text, true)
                    }
                }
            }
        }
    }

    /**
     * 打开关闭辅助面板
     */
    private fun openOrCloseHelp(open: Boolean) {
        if (open) {
            binding.help.visibility = View.VISIBLE
        } else {
            binding.help.visibility = View.GONE
        }
    }
    private fun startSearch(key: String) {
        adapter.clearItems()
        viewModel.startDebug(key, {
            binding.rotateLoading.visible()
        }, {
            toastOnUi("未获取到书源")
        })
    }
}