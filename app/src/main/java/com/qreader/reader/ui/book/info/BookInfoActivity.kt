package com.qreader.reader.ui.book.info

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.textclassifier.TextClassifier
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.qreader.reader.R
import com.qreader.reader.base.VMBaseActivity
import com.qreader.reader.constant.BookType
import com.qreader.reader.constant.EventBus
import com.qreader.reader.constant.Theme
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.BaseSource
import com.qreader.reader.data.entities.Book
import com.qreader.reader.data.entities.BookChapter
import com.qreader.reader.data.entities.BookSource
import com.qreader.reader.databinding.ActivityBookInfoComposeBinding
import com.qreader.reader.exception.NoStackTraceException
import com.qreader.reader.help.AppWebDav
import com.qreader.reader.help.GlideImageGetter
import com.qreader.reader.help.TextViewTagHandler
import com.qreader.reader.help.WebCacheManager
import com.qreader.reader.help.book.addType
import com.qreader.reader.help.book.getRemoteUrl
import com.qreader.reader.help.book.isAudio
import com.qreader.reader.help.book.isImage
import com.qreader.reader.help.book.isLocal
import com.qreader.reader.help.book.isLocalTxt
import com.qreader.reader.help.book.isWebFile
import com.qreader.reader.help.book.removeType
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.help.config.LocalConfig
import com.qreader.reader.help.webView.PooledWebView
import com.qreader.reader.help.webView.WebJsExtensions
import com.qreader.reader.help.webView.WebJsExtensions.Companion.getInjectionString
import com.qreader.reader.help.webView.WebJsExtensions.Companion.nameCache
import com.qreader.reader.help.webView.WebJsExtensions.Companion.nameJava
import com.qreader.reader.help.webView.WebJsExtensions.Companion.nameSource
import com.qreader.reader.help.webView.WebViewPool
import com.qreader.reader.lib.dialogs.alert
import com.qreader.reader.lib.dialogs.selector
import com.qreader.reader.model.remote.RemoteBookWebDav
import com.qreader.reader.ui.book.audio.AudioPlayActivity
import com.qreader.reader.ui.book.changecover.ChangeCoverDialog
import com.qreader.reader.ui.book.changesource.ChangeBookSourceDialog
import com.qreader.reader.ui.book.group.GroupSelectDialog
import com.qreader.reader.ui.book.info.edit.BookInfoEditActivity
import com.qreader.reader.ui.book.manga.ReadMangaActivity
import com.qreader.reader.ui.book.read.ReadBookActivity
import com.qreader.reader.ui.book.read.ReadBookActivity.Companion.RESULT_DELETED
import com.qreader.reader.ui.book.search.SearchActivity
import com.qreader.reader.model.SourceCallBack
import com.qreader.reader.ui.association.OnLineImportActivity
import com.qreader.reader.ui.book.source.edit.BookSourceEditActivity
import com.qreader.reader.ui.book.toc.TocActivityResult
import com.qreader.reader.ui.file.HandleFileContract
import com.qreader.reader.ui.login.SourceLoginActivity
import com.qreader.reader.ui.widget.dialog.PhotoDialog
import com.qreader.reader.ui.widget.dialog.VariableDialog
import com.qreader.reader.ui.widget.dialog.WaitDialog
import com.qreader.reader.ui.compose.GlassAlertDialog
import com.qreader.reader.utils.ConvertUtils
import com.qreader.reader.utils.FileDoc
import com.qreader.reader.utils.GSON
import com.qreader.reader.utils.StartActivityContract
import com.qreader.reader.utils.dpToPx
import com.qreader.reader.utils.longSnackbar
import com.qreader.reader.utils.longToastOnUi
import com.qreader.reader.utils.observeEvent
import com.qreader.reader.utils.openFileUri
import com.qreader.reader.utils.openUrl
import com.qreader.reader.utils.sendToClip
import com.qreader.reader.utils.setHtml
import com.qreader.reader.utils.setMarkdown
import com.qreader.reader.utils.showDialogFragment
import com.qreader.reader.utils.startActivity
import com.qreader.reader.utils.toastOnUi
import com.qreader.reader.utils.viewbindingdelegate.viewBinding
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookInfoActivity :
    VMBaseActivity<ActivityBookInfoComposeBinding, BookInfoViewModel>(
        toolBarTheme = Theme.Dark,
        showOpenMenuIcon = false
    ),
    GroupSelectDialog.CallBack,
    ChangeBookSourceDialog.CallBack,
    ChangeCoverDialog.CallBack,
    VariableDialog.Callback {

    override val binding by viewBinding(ActivityBookInfoComposeBinding::inflate)
    override val viewModel by viewModels<BookInfoViewModel>()

    // ── Compose 状态变量 ──
    private var uiBookName by mutableStateOf("")
    private var uiAuthor by mutableStateOf("")
    private var uiOrigin by mutableStateOf("")
    private var uiLatestChapter by mutableStateOf("")
    private var uiGroupText by mutableStateOf("")
    private var uiTocText by mutableStateOf("")
    private var uiTocVisible by mutableStateOf(true)
    private var uiShelfText by mutableStateOf("")
    private val uiKinds = mutableStateListOf<String>()

    // ── 简介容器（由 Activity 管理，Compose 通过 AndroidView 展示）──
    private lateinit var introContainer: FrameLayout
    private var initIntroView = false
    private var pooledWebView: PooledWebView? = null

    private val introTextView by lazy {
        initIntroView = true
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.view_book_intro, introContainer, false)
            as com.qreader.reader.ui.widget.text.ScrollTextView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            view.revealOnFocusHint = false
        }
        view
    }

    private val imgAvailableWidth by lazy {
        val textView = introTextView
        textView.width - textView.paddingLeft - textView.paddingRight - 8.dpToPx()
    }
    private var initGetter = false
    private val glideImageGetter by lazy {
        initGetter = true
        GlideImageGetter(
            this,
            introTextView,
            lifecycle,
            imgAvailableWidth,
            viewModel.bookSource?.bookSourceUrl
        )
    }

    private val textViewTagHandler by lazy {
        TextViewTagHandler(object : TextViewTagHandler.OnButtonClickListener {
            override fun onButtonClick(name: String, click: String) {
                viewModel.onButtonClick(this@BookInfoActivity, "info button $name", click)
            }
        })
    }

    // ── Activity Result Contracts ──
    private val tocActivityResult = registerForActivityResult(TocActivityResult()) {
        it?.let {
            viewModel.getBook(false)?.let { book ->
                lifecycleScope.launch {
                    withContext(IO) {
                        val durChapterIndex = it[0] as Int
                        val durChapterPos = it[1] as Int
                        val durVolumeIndex = it[3] as Int
                        val chapterInVolumeIndex = it[4] as Int
                        book.durChapterIndex = durChapterIndex
                        book.durChapterPos = durChapterPos
                        chapterChanged = it[2] as Boolean
                        book.durVolumeIndex = durVolumeIndex
                        book.chapterInVolumeIndex = chapterInVolumeIndex
                        appDb.bookDao.update(book)
                    }
                    startReadActivity(book)
                }
            }
        } ?: let {
            if (!viewModel.inBookshelf) {
                viewModel.delBook()
            }
        }
    }
    private val localBookTreeSelect = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { treeUri ->
            AppConfig.defaultBookTreeUri = treeUri.toString()
        }
    }
    private val readBookResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.upBook(intent)
        when (it.resultCode) {
            RESULT_OK -> {
                viewModel.inBookshelf = true
                upTvBookshelf()
            }
            RESULT_DELETED -> {
                setResult(RESULT_OK)
                finish()
            }
        }
    }
    private val infoEditResult = registerForActivityResult(
        StartActivityContract(BookInfoEditActivity::class.java)
    ) {
        if (it.resultCode == RESULT_OK) {
            viewModel.upEditBook()
        }
    }
    private val editSourceResult = registerForActivityResult(
        StartActivityContract(BookSourceEditActivity::class.java)
    ) {
        if (it.resultCode == RESULT_CANCELED) {
            return@registerForActivityResult
        }
        book?.let { book ->
            viewModel.bookSource = appDb.bookSourceDao.getBookSource(book.origin)?.also { source ->
                viewModel.hasCustomBtn = source.customButton
            }
            viewModel.refreshBook(book)
        }
    }
    private var chapterChanged = false
    private val waitDialog by lazy { WaitDialog(this) }
    private var editMenuItem: MenuItem? = null
    private var menuCustomBtn: MenuItem? = null
    private val book get() = viewModel.getBook(false)

    @SuppressLint("PrivateResource")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        // 创建简介容器
        introContainer = FrameLayout(this)

        // 设置 Compose 内容
        binding.composeView.setContent {
            BookInfoScreen(
                book = viewModel.bookData.value,
                bookName = uiBookName,
                author = uiAuthor,
                origin = uiOrigin,
                latestChapter = uiLatestChapter,
                groupText = uiGroupText,
                tocText = uiTocText,
                tocVisible = uiTocVisible,
                shelfText = uiShelfText,
                introContainer = introContainer,
                kinds = uiKinds.toList(),
                onCoverClick = {
                    viewModel.getBook()?.let {
                        showDialogFragment(ChangeCoverDialog(it.name, it.author))
                    }
                },
                onCoverLongClick = {
                    viewModel.getBook()?.getDisplayCover()?.let { path ->
                        showDialogFragment(PhotoDialog(path, isBook = true))
                    }
                },
                onReadClick = {
                    viewModel.getBook()?.let { book ->
                        if (book.isWebFile) {
                            showWebFileDownloadAlert { readBook(it) }
                        } else {
                            readBook(book)
                        }
                    }
                },
                onShelfClick = {
                    viewModel.getBook()?.let { book ->
                        if (viewModel.inBookshelf) {
                            deleteBook()
                        } else {
                            if (book.isWebFile) {
                                showWebFileDownloadAlert()
                            } else {
                                viewModel.addToBookshelf { upTvBookshelf() }
                            }
                        }
                    }
                },
                onOriginClick = {
                    viewModel.getBook()?.let { book ->
                        if (book.isLocal) return@let
                        if (!appDb.bookSourceDao.has(book.origin)) {
                            toastOnUi(R.string.error_no_source)
                            return@let
                        }
                        editSourceResult.launch {
                            putExtra("sourceUrl", book.origin)
                        }
                    }
                },
                onChangeSource = {
                    viewModel.getBook()?.let { book ->
                        showDialogFragment(ChangeBookSourceDialog(book.name, book.author))
                    }
                },
                onTocClick = {
                    if (viewModel.chapterListData.value.isNullOrEmpty()) {
                        toastOnUi(R.string.chapter_list_empty)
                        return@BookInfoScreen
                    }
                    viewModel.getBook()?.let { book ->
                        if (!viewModel.inBookshelf) {
                            viewModel.saveBook(book) {
                                viewModel.saveChapterList { openChapterList() }
                            }
                        } else {
                            openChapterList()
                        }
                    }
                },
                onGroupChange = {
                    viewModel.getBook()?.let {
                        showDialogFragment(GroupSelectDialog(it.group))
                    }
                },
                onNameClick = {
                    viewModel.getBook(false)?.let { book ->
                        SourceCallBack.callBackBtn(
                            this@BookInfoActivity,
                            SourceCallBack.CLICK_BOOK_NAME,
                            viewModel.bookSource,
                            book,
                            null,
                            result = book.name
                        ) {
                            SearchActivity.start(this@BookInfoActivity, book.name)
                        }
                    }
                },
                onNameLongClick = {
                    viewModel.getBook(false)?.let { book ->
                        SourceCallBack.callBackBtn(
                            this@BookInfoActivity,
                            SourceCallBack.LONG_CLICK_BOOK_NAME,
                            viewModel.bookSource,
                            book,
                            null,
                            result = book.name
                        ) {
                            SearchActivity.start(this@BookInfoActivity, book.name)
                        }
                    }
                },
                onAuthorClick = {
                    viewModel.getBook(false)?.let { book ->
                        SourceCallBack.callBackBtn(
                            this@BookInfoActivity,
                            SourceCallBack.CLICK_AUTHOR,
                            viewModel.bookSource,
                            book,
                            null,
                            result = book.author
                        ) {
                            SearchActivity.start(this@BookInfoActivity, book.author)
                        }
                    }
                },
                onAuthorLongClick = {
                    viewModel.getBook(false)?.let { book ->
                        SourceCallBack.callBackBtn(
                            this@BookInfoActivity,
                            SourceCallBack.LONG_CLICK_AUTHOR,
                            viewModel.bookSource,
                            book,
                            null,
                            result = book.author
                        ) {
                            SearchActivity.start(this@BookInfoActivity, book.author)
                        }
                    }
                },
                onRefresh = { refreshBook() },
            )
        }

        // 观察 ViewModel
        viewModel.bookData.observe(this) { showBook(it) }
        viewModel.chapterListData.observe(this) { upLoading(false, it) }
        viewModel.waitDialogData.observe(this) { upWaitDialogStatus(it) }
        viewModel.initData(intent)
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_info, menu)
        editMenuItem = menu.findItem(R.id.menu_edit)
        menuCustomBtn = menu.findItem(R.id.menu_custom_btn).also {
            it.isVisible = viewModel.hasCustomBtn
        }
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_can_update)?.isChecked =
            viewModel.bookData.value?.canUpdate ?: true
        menu.findItem(R.id.menu_split_long_chapter)?.isChecked =
            viewModel.bookData.value?.getSplitLongChapter() ?: true
        menu.findItem(R.id.menu_login)?.isVisible =
            !viewModel.bookSource?.loginUrl.isNullOrBlank()
        menu.findItem(R.id.menu_set_source_variable)?.isVisible =
            viewModel.bookSource != null
        menu.findItem(R.id.menu_set_book_variable)?.isVisible =
            viewModel.bookSource != null
        menu.findItem(R.id.menu_can_update)?.isVisible =
            viewModel.bookSource != null
        menu.findItem(R.id.menu_split_long_chapter)?.isVisible =
            viewModel.bookData.value?.isLocalTxt ?: false
        menu.findItem(R.id.menu_upload)?.isVisible =
            viewModel.bookData.value?.isLocal ?: false
        menu.findItem(R.id.menu_delete_alert)?.isChecked =
            LocalConfig.bookInfoDeleteAlert
        return super.onMenuOpened(featureId, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_custom_btn -> {
                viewModel.bookSource?.customButton?.let {
                    viewModel.getBook()?.let { book ->
                        SourceCallBack.callBackBtn(
                            this,
                            SourceCallBack.CLICK_CUSTOM_BUTTON,
                            viewModel.bookSource,
                            book,
                            null
                        )
                    }
                }
            }
            R.id.menu_edit -> {
                viewModel.getBook()?.let {
                    infoEditResult.launch {
                        putExtra("bookUrl", it.bookUrl)
                    }
                }
            }
            R.id.menu_share_it -> {
                viewModel.getBook()?.let {
                    val bookJson = GSON.toJson(it)
                    val shareStr = "${it.bookUrl}#$bookJson"
                    SourceCallBack.callBackBtn(
                        this,
                        SourceCallBack.CLICK_SHARE_BOOK,
                        viewModel.bookSource,
                        it,
                        null,
                        result = shareStr
                    ) {
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.putExtra(Intent.EXTRA_TEXT, shareStr)
                        intent.type = "text/plain"
                        startActivity(Intent.createChooser(intent, it.name))
                    }
                }
            }
            R.id.menu_refresh -> refreshBook()
            R.id.menu_login -> viewModel.bookSource?.let {
                startActivity<SourceLoginActivity> {
                    putExtra("type", "bookSource")
                    putExtra("key", it.bookSourceUrl)
                    putExtra("bookUrl", book?.bookUrl)
                }
            }
            R.id.menu_top -> viewModel.topBook()
            R.id.menu_set_source_variable -> setSourceVariable()
            R.id.menu_set_book_variable -> setBookVariable()
            R.id.menu_copy_book_url -> viewModel.getBook()?.let {
                SourceCallBack.callBackBtn(
                    this,
                    SourceCallBack.CLICK_COPY_BOOK_URL,
                    viewModel.bookSource,
                    it,
                    null,
                    result = it.bookUrl
                ) { sendToClip(it.bookUrl) }
            }
            R.id.menu_copy_toc_url -> viewModel.getBook()?.let {
                SourceCallBack.callBackBtn(
                    this,
                    SourceCallBack.CLICK_COPY_TOC_URL,
                    viewModel.bookSource,
                    it,
                    null,
                    result = it.tocUrl
                ) { sendToClip(it.tocUrl) }
            }
            R.id.menu_can_update -> {
                viewModel.getBook()?.let {
                    it.canUpdate = !it.canUpdate
                    if (viewModel.inBookshelf) {
                        if (!it.canUpdate) {
                            it.removeType(BookType.updateError)
                        }
                        viewModel.saveBook(it)
                    }
                }
            }
            R.id.menu_clear_cache -> viewModel.getBook()?.let {
                SourceCallBack.callBackBtn(
                    this,
                    SourceCallBack.CLICK_CLEAR_CACHE,
                    viewModel.bookSource,
                    it,
                    null
                ) { viewModel.clearCache(it) }
            }
            R.id.menu_split_long_chapter -> {
                upLoading(true)
                viewModel.getBook()?.let {
                    it.setSplitLongChapter(!item.isChecked)
                    viewModel.loadBookInfo(it, false)
                }
                item.isChecked = !item.isChecked
                if (!item.isChecked) longToastOnUi(R.string.need_more_time_load_content)
            }
            R.id.menu_delete_alert -> LocalConfig.bookInfoDeleteAlert = !item.isChecked
            R.id.menu_upload -> {
                viewModel.getBook()?.let { book ->
                    book.getRemoteUrl()?.let {
                        alert(R.string.draw, R.string.sure_upload) {
                            okButton { upLoadBook(book) }
                            cancelButton()
                        }
                    } ?: upLoadBook(book)
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun observeLiveBus() {
        viewModel.actionLive.observe(this) {
            when (it) {
                "selectBooksDir" -> localBookTreeSelect.launch {
                    title = getString(R.string.select_book_folder)
                }
            }
        }
        observeEvent<Boolean>(EventBus.REFRESH_BOOK_INFO) {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                refreshBook()
            }
        }
        observeEvent<Boolean>(EventBus.REFRESH_BOOK_TOC) {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                refreshToc()
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (initIntroView && ev.action == MotionEvent.ACTION_DOWN) {
            currentFocus?.let {
                if (it === introTextView && introTextView.hasSelection()) {
                    it.clearFocus()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun refreshBook() {
        upLoading(true)
        viewModel.getBook()?.let { viewModel.refreshBook(it) }
    }

    private fun refreshToc() {
        upLoading(true)
        viewModel.getBook()?.let { viewModel.loadChapter(it, true, isFromBookInfo = true) }
    }

    private fun upLoadBook(
        book: Book,
        bookWebDav: RemoteBookWebDav? = AppWebDav.defaultBookWebDav,
    ) {
        lifecycleScope.launch {
            waitDialog.setText("上传中.....")
            waitDialog.show()
            try {
                bookWebDav?.upload(book) ?: throw NoStackTraceException("未配置webDav")
                book.lastCheckTime = System.currentTimeMillis()
                viewModel.saveBook(book)
            } catch (e: Exception) {
                toastOnUi(e.localizedMessage)
            } finally {
                waitDialog.dismiss()
            }
        }
    }

    private fun showBook(book: Book) {
        uiBookName = book.name
        uiAuthor = getString(R.string.author_show, book.getRealAuthor())
        uiOrigin = getString(R.string.origin_show, book.originName)
        uiLatestChapter = getString(R.string.lasted_show, book.latestChapterTitle)
        showBookIntro(book)
        if (book.isWebFile) {
            uiTocVisible = false
            uiLatestChapter = getString(R.string.lasted_show, "下载中...")
        } else {
            uiTocVisible = true
        }
        menuCustomBtn?.isVisible = viewModel.hasCustomBtn
        upTvBookshelf()
        upKinds(book)
        upGroup(book.group)
    }

    inner class CustomWebViewClient : WebViewClient() {
        private val jsStr = getInjectionString
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            request?.let {
                val uri = it.url
                return when (uri.scheme) {
                    "http", "https" -> false
                    "legado", "yuedu" -> {
                        startActivity<OnLineImportActivity> { data = uri }
                        true
                    }
                    else -> {
                        window.decorView.longSnackbar(R.string.jump_to_another_app, R.string.confirm) {
                            openUrl(uri)
                        }
                        true
                    }
                }
            }
            return true
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            view?.evaluateJavascript(jsStr, null)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            view?.post { introContainer.requestLayout() }
        }
    }

    private fun showBookIntro(book: Book) {
        val intro = book.getDisplayIntro()
        if (intro?.startsWith("<useweb>") == true) {
            val lastIndex = intro.lastIndexOf("<")
            if (lastIndex < 8) {
                introTextView.text = intro
                return
            }
            val html = intro.substring(8, lastIndex)
            val pooledWebView = this.pooledWebView ?: let {
                val pooledWebView = WebViewPool.acquire(this)
                val webView = pooledWebView.realWebView
                webView.onResume()
                webView.webViewClient = CustomWebViewClient()
                webView.addJavascriptInterface(WebCacheManager, nameCache)
                viewModel.bookSource?.let {
                    webView.addJavascriptInterface(it as BaseSource, nameSource)
                    val webJsExtensions = WebJsExtensions(it, null, webView)
                    webView.addJavascriptInterface(webJsExtensions, nameJava)
                }
                pooledWebView
            }
            val webView = pooledWebView.realWebView
            if (initIntroView || this.pooledWebView == null) {
                initIntroView = false
                this.pooledWebView = pooledWebView
                introContainer.removeAllViews()
                introContainer.addView(webView)
            }
            val bookUrl = viewModel.getBook()?.bookUrl
                ?.takeIf { it.startsWith("http", true) }
                ?.substringBefore(",")
            webView.loadDataWithBaseURL(bookUrl, html, "text/html", "utf-8", bookUrl)
            return
        }
        if (!initIntroView || pooledWebView != null) {
            destroyWeb()
            introContainer.removeAllViews()
            introContainer.addView(introTextView)
        }
        if (intro.isNullOrBlank()) {
            return
        }
        val tvIntro = introTextView
        if (intro.startsWith("<usehtml>")) {
            val lastIndex = intro.lastIndexOf("<")
            if (lastIndex < 9) {
                tvIntro.text = intro
                return
            }
            val html = intro.substring(9, lastIndex)
            tvIntro.setHtml(
                html,
                glideImageGetter,
                textViewTagHandler,
                imgOnLongClickListener = {
                    showDialogFragment(PhotoDialog(it, viewModel.bookSource?.bookSourceUrl))
                },
                imgOnClickListener = {
                    viewModel.onButtonClick(this@BookInfoActivity, "info image", it)
                }
            )
        } else if (intro.startsWith("<md>")) {
            val lastIndex = intro.lastIndexOf("<")
            if (lastIndex < 4) {
                tvIntro.text = intro
                return
            }
            val mark = intro.substring(4, lastIndex)
            lifecycleScope.launch {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    tvIntro.setTextClassifier(TextClassifier.NO_OP)
                }
                val context = this@BookInfoActivity
                val markwon: Markwon
                val markdown = withContext(IO) {
                    markwon = Markwon.builder(context)
                        .usePlugin(
                            GlideImagesPlugin.create(
                                Glide.with(context)
                                    .applyDefaultRequestOptions(
                                        RequestOptions()
                                            .override(imgAvailableWidth)
                                            .encodeQuality(88)
                                    )
                            )
                        )
                        .usePlugin(HtmlPlugin.create())
                        .usePlugin(TablePlugin.create(context))
                        .build()
                    markwon.toMarkdown(mark)
                }
                tvIntro.setMarkdown(
                    markwon,
                    markdown,
                    imgOnLongClickListener = { source ->
                        showDialogFragment(PhotoDialog(source, viewModel.bookSource?.bookSourceUrl))
                    }
                )
            }
        } else {
            tvIntro.text = intro
        }
    }

    private fun upKinds(book: Book) {
        lifecycleScope.launch {
            var kinds = book.getKindList()
            if (book.isLocal) {
                withContext(IO) {
                    val size = FileDoc.fromFile(book.bookUrl).size
                    if (size > 0) {
                        kinds = kinds.toMutableList()
                        kinds.add(ConvertUtils.formatFileSize(size))
                    }
                }
            }
            uiKinds.clear()
            uiKinds.addAll(kinds)
        }
    }

    private fun upLoading(isLoading: Boolean, chapterList: List<BookChapter>? = null) {
        when {
            isLoading -> {
                uiTocText = getString(R.string.toc_s, getString(R.string.loading))
            }
            chapterList.isNullOrEmpty() -> {
                uiTocText = getString(R.string.toc_s, getString(R.string.error_load_toc))
                uiLatestChapter = getString(R.string.lasted_show, book?.latestChapterTitle)
            }
            else -> {
                book?.let {
                    uiTocText = getString(R.string.toc_s, it.durChapterTitle)
                    uiLatestChapter = getString(R.string.lasted_show, it.latestChapterTitle)
                }
            }
        }
    }

    private fun upTvBookshelf() {
        uiShelfText = if (viewModel.inBookshelf) {
            getString(R.string.remove_from_bookshelf)
        } else {
            getString(R.string.add_to_bookshelf)
        }
        editMenuItem?.isVisible = viewModel.inBookshelf
    }

    private fun upGroup(groupId: Long) {
        viewModel.loadGroup(groupId) {
            uiGroupText = if (it.isNullOrEmpty()) {
                if (book?.isLocal == true) {
                    getString(R.string.group_s, getString(R.string.local_no_group))
                } else {
                    getString(R.string.group_s, getString(R.string.no_group))
                }
            } else {
                getString(R.string.group_s, it)
            }
        }
    }

    private fun setSourceVariable() {
        lifecycleScope.launch {
            val source = viewModel.bookSource
            if (source == null) {
                toastOnUi("书源不存在")
                return@launch
            }
            val comment =
                source.getDisplayVariableComment("源变量可在js中通过source.getVariable()获取")
            val variable = withContext(IO) { source.getVariable() }
            showDialogFragment(
                VariableDialog(
                    getString(R.string.set_source_variable),
                    source.getKey(),
                    variable,
                    comment
                )
            )
        }
    }

    private fun setBookVariable() {
        lifecycleScope.launch {
            val source = viewModel.bookSource
            if (source == null) {
                toastOnUi("书源不存在")
                return@launch
            }
            val book = viewModel.getBook() ?: return@launch
            val variable = withContext(IO) { book.getCustomVariable() }
            val comment = source.getDisplayVariableComment(
                """书籍变量可在js中通过book.getVariable("custom")获取"""
            )
            showDialogFragment(
                VariableDialog(
                    getString(R.string.set_book_variable),
                    book.bookUrl,
                    variable,
                    comment
                )
            )
        }
    }

    override fun setVariable(key: String, variable: String?) {
        when (key) {
            viewModel.bookSource?.getKey() -> viewModel.bookSource?.setVariable(variable)
            viewModel.bookData.value?.bookUrl -> viewModel.bookData.value?.let {
                it.putCustomVariable(variable)
                if (viewModel.inBookshelf) {
                    viewModel.saveBook(it)
                }
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun deleteBook() {
        viewModel.getBook()?.let { book ->
            if (LocalConfig.bookInfoDeleteAlert) {
                val pageBitmap = try {
                    val rootView = window.decorView
                    if (rootView.width > 0 && rootView.height > 0) {
                        Bitmap.createBitmap(
                            rootView.width,
                            rootView.height,
                            Bitmap.Config.ARGB_8888
                        ).also { rootView.draw(Canvas(it)) }
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
                val dialog = GlassAlertDialog.newInstance(
                    title = getString(R.string.draw),
                    message = getString(R.string.sure_del),
                    confirmText = getString(R.string.ok),
                    cancelText = getString(R.string.cancel),
                    showCheckBox = book.isLocal,
                    checkBoxText = if (book.isLocal) getString(R.string.delete_book_file) else "",
                    checkBoxChecked = LocalConfig.deleteBookOriginal,
                    backdropBitmap = pageBitmap,
                    onConfirm = { checkBoxChecked ->
                        if (book.isLocal) {
                            LocalConfig.deleteBookOriginal = checkBoxChecked
                        }
                        SourceCallBack.callBackBook(
                            SourceCallBack.DEL_BOOK_SHELF,
                            viewModel.bookSource,
                            book
                        )
                        viewModel.delBook(LocalConfig.deleteBookOriginal) {
                            setResult(RESULT_OK)
                            finish()
                        }
                    },
                    onCancel = {}
                )
                dialog.show(supportFragmentManager, "glass_delete_dialog")
            } else {
                SourceCallBack.callBackBook(SourceCallBack.DEL_BOOK_SHELF, viewModel.bookSource, book)
                viewModel.delBook(LocalConfig.deleteBookOriginal) {
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
    }

    private fun openChapterList() {
        viewModel.getBook()?.let { tocActivityResult.launch(it.bookUrl) }
    }

    private fun showWebFileDownloadAlert(
        onClick: ((Book) -> Unit)? = null,
    ) {
        val webFiles = viewModel.webFiles
        if (webFiles.isEmpty()) {
            toastOnUi("Unexpected webFileData")
            return
        }
        selector(R.string.download_and_import_file, webFiles) { _, webFile, _ ->
            if (webFile.isSupported) {
                viewModel.importOrDownloadWebFile<Book>(webFile) { onClick?.invoke(it) }
            } else if (webFile.isSupportDecompress) {
                viewModel.importOrDownloadWebFile<Uri>(webFile) { uri ->
                    viewModel.getArchiveFilesName(uri) { fileNames ->
                        if (fileNames.size == 1) {
                            viewModel.importArchiveBook(uri, fileNames[0]) { onClick?.invoke(it) }
                        } else {
                            showDecompressFileImportAlert(uri, fileNames, onClick)
                        }
                    }
                }
            } else {
                alert(
                    title = getString(R.string.draw),
                    message = getString(R.string.file_not_supported, webFile.name)
                ) {
                    neutralButton(R.string.open_fun) {
                        viewModel.importOrDownloadWebFile<Uri>(webFile) { openFileUri(it, "*/*") }
                    }
                    noButton()
                }
            }
        }
    }

    private fun showDecompressFileImportAlert(
        archiveFileUri: Uri,
        fileNames: List<String>,
        success: ((Book) -> Unit)? = null,
    ) {
        if (fileNames.isEmpty()) {
            toastOnUi(R.string.unsupport_archivefile_entry)
            return
        }
        selector(R.string.import_select_book, fileNames) { _, name, _ ->
            viewModel.importArchiveBook(archiveFileUri, name) { success?.invoke(it) }
        }
    }

    private fun readBook(book: Book) {
        if (!viewModel.inBookshelf) {
            book.addType(BookType.notShelf)
            viewModel.saveBook(book) {
                viewModel.saveChapterList { startReadActivity(book) }
            }
        } else {
            viewModel.saveBook(book) { startReadActivity(book) }
        }
    }

    private fun startReadActivity(book: Book) {
        when {
            book.isAudio -> readBookResult.launch(
                Intent(this, AudioPlayActivity::class.java)
                    .putExtra("bookUrl", book.bookUrl)
                    .putExtra("inBookshelf", viewModel.inBookshelf)
            )
            else -> readBookResult.launch(
                Intent(
                    this,
                    if (!book.isLocal && book.isImage && AppConfig.showMangaUi) ReadMangaActivity::class.java
                    else ReadBookActivity::class.java
                )
                    .putExtra("bookUrl", book.bookUrl)
                    .putExtra("inBookshelf", viewModel.inBookshelf)
                    .putExtra("chapterChanged", chapterChanged)
            )
        }
    }

    override val oldBook: Book?
        get() = viewModel.bookData.value

    override fun changeTo(source: BookSource, book: Book, toc: List<BookChapter>) {
        viewModel.changeTo(source, book, toc)
    }

    override fun coverChangeTo(coverUrl: String) {
        viewModel.bookData.value?.let { book ->
            book.customCoverUrl = coverUrl
            // 触发 Compose 重新加载封面
            viewModel.bookData.postValue(book)
            if (viewModel.inBookshelf) {
                viewModel.saveBook(book)
            }
        }
    }

    override fun upGroup(requestCode: Int, groupId: Long) {
        upGroup(groupId)
        viewModel.getBook()?.let { book ->
            book.group = groupId
            if (viewModel.inBookshelf) {
                viewModel.saveBook(book)
            } else if (groupId > 0) {
                viewModel.addToBookshelf { upTvBookshelf() }
            }
        }
    }

    private fun upWaitDialogStatus(isShow: Boolean) {
        val showText = "Loading....."
        if (isShow) {
            waitDialog.run {
                setText(showText)
                show()
            }
        } else {
            waitDialog.dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        if (initGetter) {
            glideImageGetter.start()
        }
    }

    override fun onStop() {
        super.onStop()
        if (initGetter) {
            glideImageGetter.stop()
        }
    }

    override fun onDestroy() {
        destroyWeb()
        super.onDestroy()
        if (initGetter) {
            glideImageGetter.clear()
        }
    }

    private fun destroyWeb() {
        pooledWebView?.let { WebViewPool.release(it) }
        pooledWebView = null
    }
}
