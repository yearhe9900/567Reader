package com.qreader.reader.ui.rss.read

import android.webkit.JavascriptInterface
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.script.rhino.runScriptWithContext
import com.qreader.reader.constant.BookType
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.BaseSource
import com.qreader.reader.data.entities.Book
import com.qreader.reader.data.entities.BookChapter
import com.qreader.reader.data.entities.BookSource
import com.qreader.reader.data.entities.RssReadRecord
import com.qreader.reader.data.entities.RssSource
import com.qreader.reader.help.JsExtensions
import com.qreader.reader.model.AudioPlay
import com.qreader.reader.model.ReadBook
import com.qreader.reader.model.analyzeRule.AnalyzeRule
import com.qreader.reader.model.analyzeRule.AnalyzeRule.Companion.setChapter
import com.qreader.reader.ui.association.AddToBookshelfDialog
import com.qreader.reader.ui.book.explore.ExploreShowActivity
import com.qreader.reader.ui.book.search.SearchActivity
import com.qreader.reader.ui.login.SourceLoginActivity
import com.qreader.reader.ui.widget.dialog.PhotoDialog
import com.qreader.reader.utils.isJsonObject
import com.qreader.reader.utils.openUrl
import com.qreader.reader.utils.showDialogFragment
import com.qreader.reader.utils.startActivity
import com.qreader.reader.utils.toastOnUi
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.net.URL


@Suppress("unused")
open class RssJsExtensions(
    activity: AppCompatActivity?,
    source: BaseSource?,
    val bookType: Int = 0
) : JsExtensions {

    val activityRef: WeakReference<AppCompatActivity> = WeakReference(activity)
    val sourceRef: WeakReference<BaseSource?> = WeakReference(source)

    override fun getSource(): BaseSource? {
        return sourceRef.get()
    }

    override fun getTag(): String? {
        return getSource()?.getTag()
    }

    @JavascriptInterface
    fun put(key: String, value: String): String {
        getSource()?.put(key, value)
        return value
    }

    @JavascriptInterface
    fun get(key: String): String {
        return getSource()?.get(key) ?: ""
    }

    @JavascriptInterface
    @JvmOverloads
    fun searchBook(key: String, searchScope: String? = null) {
        activityRef.get()?.let {
            SearchActivity.start(it, key, searchScope)
        }
    }

    fun searchBook(key: String, source: BookSource) {
        activityRef.get()?.let {
            SearchActivity.start(it, source, key)
        }
    }

    @JavascriptInterface
    fun addBook(bookUrl: String) {
        activityRef.get()?.showDialogFragment(AddToBookshelfDialog(bookUrl))
    }

    @JavascriptInterface
    fun showPhoto(src: String) {
        activityRef.get()?.showDialogFragment(PhotoDialog(src, getSource()?.getKey()))
    }


    @JavascriptInterface
    @JvmOverloads
    fun open(name: String, url: String? = null, title: String? = null, origin: String? = null) {
        val activity = activityRef.get() ?: return
        activity.lifecycleScope.launch(IO) {
            val source = getSource() ?: return@launch
            when (name) {
                "login" -> {
                    if (activity is SourceLoginActivity) {
                        activity.toastOnUi("已在登录界面")
                        return@launch
                    }
                    val toSource = origin?.let { o ->
                        appDb.bookSourceDao.getBookSource(o)
                    } ?: source
                    if (toSource.loginUrl.isNullOrBlank()) {
                        activity.toastOnUi("源未配置登录")
                        return@launch
                    }
                    when (toSource) {
                        is BookSource -> {
                            withContext(Main) {
                                activity.startActivity<SourceLoginActivity> {
                                    putExtra("bookType", bookType)
                                    putExtra("type", "bookSource")
                                    putExtra("key", toSource.bookSourceUrl)
                                }
                            }
                        }

                        is RssSource -> {
                            withContext(Main) {
                                activity.startActivity<SourceLoginActivity> {
                                    putExtra("type", "rssSource")
                                    putExtra("key", toSource.sourceUrl)
                                }
                            }
                        }
                    }
                }

                "search" -> {
                    title?.let {
                        origin?.let { o  ->
                            appDb.bookSourceDao.getBookSource(o)?.let { s ->
                                searchBook(it, s)
                                return@launch
                            }
                        }
                        searchBook(it)
                    }
                }

                "explore" -> {
                    val toSource = origin?.let { o ->
                        appDb.bookSourceDao.getBookSource(o)
                    } ?: (source as? BookSource) ?: return@launch
                    val sourceUrl = toSource.bookSourceUrl
                    withContext(Main) {
                        activity.startActivity<ExploreShowActivity> {
                            putExtra("exploreName", title)
                            putExtra("sourceUrl", sourceUrl)
                            putExtra("exploreUrl", url)
                        }
                    }
                }
            }
        }
    }

    /** AnalyzeRule实现 **/
    private val bookAndChapter by lazy {
        var book: Book? = null
        var chapter: BookChapter? = null
        when (bookType) {
            BookType.text -> {
                book = ReadBook.book?.also {
                    chapter = appDb.bookChapterDao.getChapter(
                        it.bookUrl,
                        ReadBook.durChapterIndex
                    )
                }
            }

            BookType.audio -> {
                book = AudioPlay.book
                chapter = AudioPlay.durChapter
            }

            BookType.video -> {
                book = null
                chapter = null
            }
        }
        Pair(book, chapter)
    }
    private val book: Book? get() = bookAndChapter.first
    private val chapter: BookChapter? get() = bookAndChapter.second

    val analyzeRule by lazy {
        AnalyzeRule(book, source = getSource()).setChapter(chapter)
    }

    @JavascriptInterface
    @JvmOverloads
    fun setContent(content: Any?, baseUrl: String? = null): AnalyzeRule {
        return analyzeRule.setContent(content, baseUrl)
    }

    @JavascriptInterface
    fun setBaseUrl(baseUrl: String?): AnalyzeRule {
        return analyzeRule.setBaseUrl(baseUrl)
    }

    @JavascriptInterface
    fun setRedirectUrl(url: String): URL? {
        return analyzeRule.setRedirectUrl(url)
    }

    @JvmOverloads
    fun getStringList(rule: String?, mContent: Any? = null, isUrl: Boolean = false): List<String>? {
        return analyzeRule.getStringList(rule, mContent, isUrl)
    }

    @JvmOverloads
    fun getString(ruleStr: String?, mContent: Any? = null, isUrl: Boolean = false): String {
        return analyzeRule.getString(ruleStr, mContent, isUrl)
    }

    @JavascriptInterface
    fun getString(ruleStr: String?, unescape: Boolean): String {
        return analyzeRule.getString(ruleStr, unescape)
    }

    fun getElement(ruleStr: String): Any? {
        return analyzeRule.getElement(ruleStr)
    }

    fun getElements(ruleStr: String): List<Any> {
        return analyzeRule.getElements(ruleStr)
    }

}
