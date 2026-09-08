package com.qreader.reader.ui.book.search

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.qreader.reader.base.BaseViewModel
import com.qreader.reader.constant.AppLog
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.BookSource
import com.qreader.reader.data.entities.SearchBook
import com.qreader.reader.data.entities.SearchKeyword
import com.qreader.reader.help.book.isNotShelf
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.model.webBook.SearchModel
import com.qreader.reader.model.webBook.WebBook
import com.qreader.reader.utils.ConflateLiveData
import com.qreader.reader.utils.toastOnUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(application: Application) : BaseViewModel(application) {
    val handler = Handler(Looper.getMainLooper())
    val bookshelf: MutableSet<String> = ConcurrentHashMap.newKeySet()
    val upAdapterLiveData = MutableLiveData<String>()
    var searchBookLiveData = ConflateLiveData<List<SearchBook>>(1000)
    val searchScope: SearchScope = SearchScope(AppConfig.searchScope)
    var searchFinishLiveData = MutableLiveData<Boolean>()
    var isSearchLiveData = MutableLiveData<Boolean>()
    var searchKey: String = ""
    var hasMore = true
    private var searchID = 0L
    // 搜索结果分类补全：对 kind 为空的 SearchBook 预抓书源详情页拿到真实分类。
    // 仅在条目进入 LazyColumn 组合窗口时触发（SearchScreen 的 LaunchedEffect），
    // 并发上限 4、按 bookUrl 去重避免重复请求；条目离屏再回来时 kind 已非空会直接跳过。
    private val fillingKinds = ConcurrentHashMap.newKeySet<String>()
    private val kindSemaphore = Semaphore(4)
    private val searchModel = SearchModel(viewModelScope, object : SearchModel.CallBack {

        override fun getSearchScope(): SearchScope {
            return searchScope
        }

        override fun onSearchStart() {
            isSearchLiveData.postValue(true)
        }

        override fun onSearchSuccess(searchBooks: List<SearchBook>) {
            searchBookLiveData.postValue(searchBooks)
        }

        override fun onSearchFinish(isEmpty: Boolean, hasMore: Boolean) {
            this@SearchViewModel.hasMore = hasMore
            isSearchLiveData.postValue(false)
            searchFinishLiveData.postValue(isEmpty)
        }

        override fun onSearchCancel(exception: Throwable?) {
            isSearchLiveData.postValue(false)
            exception?.let {
                context.toastOnUi(it.localizedMessage)
            }
        }

    })

    init {
        execute {
            appDb.bookDao.flowAll().mapLatest { books ->
                val keys = arrayListOf<String>()
                books.filterNot { it.isNotShelf }
                    .forEach {
                        keys.add("${it.name}-${it.author}")
                        keys.add(it.name)
                        keys.add(it.bookUrl)
                    }
                keys
            }.catch {
                AppLog.put("搜索界面获取书籍列表失败\n${it.localizedMessage}", it)
            }.collect {
                bookshelf.clear()
                bookshelf.addAll(it)
                upAdapterLiveData.postValue("isInBookshelf")
            }
        }.onError {
            AppLog.put("加载书架数据失败", it)
        }
    }

    fun isInBookShelf(book: SearchBook): Boolean {
        val name = book.name
        val author = book.author
        val bookUrl = book.bookUrl
        val key = if (author.isNotBlank()) "$name-$author" else name
        return bookshelf.contains(key) || bookshelf.contains(bookUrl)
    }

    /**
     * 开始搜索
     */
    fun search(key: String) {
        execute {
            if ((searchKey == key) || key.isNotEmpty()) {
                searchModel.cancelSearch()
                searchID = System.currentTimeMillis()
                searchBookLiveData.postValue(emptyList())
                // 新一轮显式搜索（非加载更多）重置分类补全去重，允许重新预抓
                if (key.isNotEmpty()) fillingKinds.clear()
                searchKey = key
                hasMore = true
            }
            if (searchKey.isEmpty()) {
                return@execute
            }
            searchModel.search(searchID, searchKey)
        }
    }

    /**
     * 停止搜索
     */
    fun stop() {
        searchModel.cancelSearch()
    }

    /**
     * 为单条搜索结果补全分类：预抓书源详情页（WebBook.getBookInfoAwait）拿到真实 kind，
     * 写回当前结果列表触发重绘。由 SearchScreen 在条目进入组合窗口时调用（点击前的预抓）。
     */
    fun fillKindOne(sb: SearchBook) {
        if (!sb.kind.isNullOrBlank()) return
        val key = sb.bookUrl
        if (!fillingKinds.add(key)) return
        viewModelScope.launch(Dispatchers.IO) {
            kindSemaphore.withPermit {
                runCatching {
                    val source = appDb.bookSourceDao.getBookSource(sb.origin) ?: return@runCatching
                    val fetched = WebBook.getBookInfoAwait(source, sb.toBook())
                    val kind = fetched.kind
                    if (kind.isNullOrBlank()) {
                        fillingKinds.remove(key)
                        return@runCatching
                    }
                    val current = searchBookLiveData.value ?: return@withPermit
                    if (current.none { it.bookUrl == sb.bookUrl && it.origin == sb.origin }) {
                        return@withPermit
                    }
                    val updated = sb.copy(kind = kind, wordCount = fetched.wordCount ?: sb.wordCount)
                    searchBookLiveData.postValue(
                        current.map { b ->
                            if (b.bookUrl == sb.bookUrl && b.origin == sb.origin) updated else b
                        }
                    )
                }.onFailure {
                    fillingKinds.remove(key)
                }
            }
        }
    }

    fun pause() {
        searchModel.pause()
    }

    fun resume() {
        searchModel.resume()
    }

    /**
     * 保存搜索关键字
     */
    fun saveSearchKey(key: String) {
        execute {
            appDb.searchKeywordDao.get(key)?.let {
                it.usage += 1
                it.lastUseTime = System.currentTimeMillis()
                appDb.searchKeywordDao.update(it)
            } ?: appDb.searchKeywordDao.insert(SearchKeyword(key, 1))
        }
    }

    /**
     * 清楚搜索关键字
     */
    fun clearHistory() {
        execute {
            appDb.searchKeywordDao.deleteAll()
        }
    }

    fun deleteHistory(searchKeyword: SearchKeyword) {
        execute {
            appDb.searchKeywordDao.delete(searchKeyword)
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchModel.close()
    }

}
