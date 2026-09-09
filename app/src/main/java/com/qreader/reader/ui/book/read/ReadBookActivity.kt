package com.qreader.reader.ui.book.read

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.get
import androidx.core.view.size
import androidx.lifecycle.lifecycleScope
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import com.qreader.reader.BuildConfig
import com.qreader.reader.R
import com.qreader.reader.constant.AppConst
import com.qreader.reader.constant.AppLog
import com.qreader.reader.constant.BookType
import com.qreader.reader.constant.EventBus
import com.qreader.reader.constant.PreferKey
import com.qreader.reader.constant.Status
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.Book
import com.qreader.reader.data.entities.BookChapter
import com.qreader.reader.data.entities.BookProgress
import com.qreader.reader.data.entities.BookSource
import com.qreader.reader.exception.NoStackTraceException
import com.qreader.reader.help.AppWebDav
import com.qreader.reader.help.IntentData
import com.qreader.reader.help.TTS
import com.qreader.reader.help.book.BookHelp
import com.qreader.reader.help.book.ContentProcessor
import com.qreader.reader.help.book.isAudio
import com.qreader.reader.help.book.isEpub
import com.qreader.reader.help.book.isLocal
import com.qreader.reader.help.book.isLocalTxt
import com.qreader.reader.help.book.isMobi
import com.qreader.reader.help.book.removeType
import com.qreader.reader.help.book.update
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.help.config.ReadBookConfig
import com.qreader.reader.help.config.ReadTipConfig
import com.qreader.reader.help.config.ThemeConfig
import com.qreader.reader.help.coroutine.Coroutine
import com.qreader.reader.help.source.getSourceType
import com.qreader.reader.help.storage.Backup
import com.qreader.reader.lib.dialogs.SelectItem
import com.qreader.reader.lib.dialogs.alert
import com.qreader.reader.lib.dialogs.selector
import com.qreader.reader.lib.theme.accentColor
import com.qreader.reader.model.ReadAloud
import com.qreader.reader.model.ReadBook
import com.qreader.reader.model.analyzeRule.AnalyzeRule
import com.qreader.reader.model.analyzeRule.AnalyzeRule.Companion.setChapter
import com.qreader.reader.model.analyzeRule.AnalyzeRule.Companion.setCoroutineContext
import com.qreader.reader.utils.GSON
import com.qreader.reader.utils.fromJsonObject
import com.qreader.reader.utils.isJsonObject
import com.qreader.reader.model.localBook.EpubFile
import com.qreader.reader.model.localBook.MobiFile
import com.qreader.reader.receiver.NetworkChangedListener
import com.qreader.reader.receiver.TimeBatteryReceiver
import com.qreader.reader.service.BaseReadAloudService
import com.qreader.reader.ui.book.bookmark.BookmarkDialog
import com.qreader.reader.ui.book.changesource.ChangeBookSourceDialog
import com.qreader.reader.ui.book.changesource.ChangeChapterSourceDialog
import com.qreader.reader.ui.book.info.BookInfoActivity
import com.qreader.reader.ui.book.read.config.AutoReadDialog
import com.qreader.reader.ui.book.read.config.BgTextConfigDialog.Companion.BG_COLOR
import com.qreader.reader.ui.book.read.config.BgTextConfigDialog.Companion.TEXT_ACCENT_COLOR
import com.qreader.reader.ui.book.read.config.BgTextConfigDialog.Companion.TEXT_COLOR
import com.qreader.reader.ui.book.read.config.MoreConfigDialog
import com.qreader.reader.ui.book.read.config.ReadAloudDialog
import com.qreader.reader.ui.book.read.config.ReadStyleDialog
import com.qreader.reader.ui.book.read.config.TipConfigDialog.Companion.TIP_COLOR
import com.qreader.reader.ui.book.read.config.TipConfigDialog.Companion.TIP_DIVIDER_COLOR
import com.qreader.reader.ui.book.read.page.ContentTextView
import com.qreader.reader.ui.book.read.page.ReadView
import com.qreader.reader.ui.book.read.page.delegate.ScrollPageDelegate
import com.qreader.reader.ui.book.read.page.entities.PageDirection
import com.qreader.reader.ui.book.read.page.entities.TextPage
import com.qreader.reader.ui.book.read.page.provider.ChapterProvider
import com.qreader.reader.ui.book.read.page.provider.LayoutProgressListener
import com.qreader.reader.ui.book.searchContent.SearchContentActivity
import com.qreader.reader.ui.book.searchContent.SearchResult
import com.qreader.reader.model.SourceCallBack
import com.qreader.reader.ui.book.source.edit.BookSourceEditActivity
import com.qreader.reader.ui.book.toc.TocActivityResult
import com.qreader.reader.ui.book.toc.rule.TxtTocRuleDialog
import com.qreader.reader.ui.browser.WebViewActivity
import com.qreader.reader.ui.dict.DictDialog
import com.qreader.reader.ui.file.HandleFileContract
import com.qreader.reader.ui.login.SourceLoginActivity
import com.qreader.reader.ui.replace.ReplaceRuleActivity
import com.qreader.reader.ui.replace.edit.ReplaceEditActivity
import com.qreader.reader.ui.widget.PopupAction
import com.qreader.reader.ui.widget.dialog.PhotoDialog
import com.qreader.reader.utils.ACache
import com.qreader.reader.utils.Debounce
import com.qreader.reader.utils.LogUtils
import com.qreader.reader.utils.NetworkUtils
import com.qreader.reader.utils.StartActivityContract
import com.qreader.reader.utils.applyOpenTint
import com.qreader.reader.utils.buildMainHandler
import com.qreader.reader.utils.dismissDialogFragment
import com.qreader.reader.utils.getPrefBoolean
import com.qreader.reader.utils.putPrefBoolean
import com.qreader.reader.utils.getPrefString
import com.qreader.reader.utils.hexString
import com.qreader.reader.utils.iconItemOnLongClick
import com.qreader.reader.utils.isAbsUrl
import com.qreader.reader.utils.isTrue
import com.qreader.reader.utils.launch
import com.qreader.reader.utils.navigationBarGravity
import com.qreader.reader.utils.observeEvent
import com.qreader.reader.utils.observeEventSticky
import com.qreader.reader.utils.postEvent
import com.qreader.reader.utils.showDialogFragment
import com.qreader.reader.utils.showHelp
import com.qreader.reader.utils.startActivity
import com.qreader.reader.utils.startActivityForBook
import com.qreader.reader.utils.sysScreenOffTime
import com.qreader.reader.utils.throttle
import com.qreader.reader.utils.toastOnUi
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.script.rhino.runScriptWithContext
import com.qreader.reader.model.analyzeRule.AnalyzeUrl.Companion.paramPattern
import com.qreader.reader.ui.login.SourceLoginJsExtensions

/**
 * 阅读界面
 */
class ReadBookActivity : BaseReadBookActivity(),
    View.OnTouchListener,
    ReadView.CallBack,
    TextActionMenu.CallBack,
    ContentTextView.CallBack,
    PopupMenu.OnMenuItemClickListener,
    ReadAloudDialog.CallBack,
    ChangeBookSourceDialog.CallBack,
    ChangeChapterSourceDialog.CallBack,
    ReadBook.CallBack,
    AutoReadDialog.CallBack,
    TxtTocRuleDialog.CallBack,
    ColorPickerDialogListener,
    LayoutProgressListener {

    /** ReadView 实例，由 Compose AndroidView 工厂创建 */
    lateinit var readView: ReadView
        private set

    /** 文字选区菜单锚点位置 */
    var textMenuPosX: Float = 0f
    var textMenuPosY: Float = 0f

    /** 光标 View（暂时保留，Phase 6 改为 Compose） */
    lateinit var cursorLeft: ImageView
        private set
    lateinit var cursorRight: ImageView
        private set

    /** 章节跳转是否已确认过（对齐原版 ReadMenu.confirmSkipToChapter） */
    private var confirmSkipToChapter = false

    private val tocActivity =
        registerForActivityResult(TocActivityResult()) {
            it?.let {
                viewModel.openChapter(it[0] as Int, it[1] as Int)
            }
        }
    private val sourceEditActivity =
        registerForActivityResult(StartActivityContract(BookSourceEditActivity::class.java)) {
            if (it.resultCode == RESULT_OK) {
                viewModel.upBookSource {
                    upMenuView()
                }
            }
        }
    private val replaceActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                viewModel.replaceRuleChanged()
            }
        }
    private val searchContentActivity =
        registerForActivityResult(StartActivityContract(SearchContentActivity::class.java)) {
            val data = it.data ?: return@registerForActivityResult
            val key = data.getLongExtra("key", System.currentTimeMillis())
            val index = data.getIntExtra("index", 0)
            val searchResult = IntentData.get<SearchResult>("searchResult$key")
            val searchResultList = IntentData.get<List<SearchResult>>("searchResultList$key")
            if (searchResult != null && searchResultList != null) {
                viewModel.searchContentQuery = searchResult.query
                readPageState.searchResults = searchResultList
                isShowingSearchResult = true
                viewModel.searchResultIndex = index
                readPageState.searchResultIndex = index
                readPageState.searchResults.getOrNull(readPageState.searchResultIndex)?.let { currentResult ->
                    ReadBook.saveCurrentBookProgress() //退出全文搜索恢复此时进度
                    skipToSearch(currentResult)
                    showActionMenu()
                }
            }
        }
    private val bookInfoActivity =
        registerForActivityResult(StartActivityContract(BookInfoActivity::class.java)) {
            if (it.resultCode == RESULT_OK) {
                setResult(RESULT_DELETED)
                super.finish()
            } else {
                ReadBook.loadOrUpContent()
            }
        }
    private val selectImageDir = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            ACache.get().put(AppConst.imagePathKey, uri.toString())
            viewModel.saveImage(it.value, uri)
        }
    }
    private var menu: Menu? = null
    private var backupJob: Job? = null
    private var tts: TTS? = null
    val textActionMenu: TextActionMenu by lazy {
        TextActionMenu(this, this)
    }
    private val popupAction: PopupAction by lazy {
        PopupAction(this)
    }
    override val isInitFinish: Boolean get() = viewModel.isInitFinish
    override val isScroll: Boolean get() = readView.isScroll
    private val isAutoPage get() = readView.isAutoPage
    var isShowingSearchResult = false
    override var isSelectingSearchResult = false
        set(value) {
            field = value && isShowingSearchResult
        }
    private val timeBatteryReceiver = TimeBatteryReceiver()
    private var screenTimeOut: Long = 0
    private var loadStates: Boolean = false
    override val pageFactory get() = readView.pageFactory
    override val pageDelegate get() = readView.pageDelegate
    override val headerHeight: Int get() = readView.curPage.headerHeight
    override val imgBgPaddingStart: Int get() = readView.curPage.imgBgPaddingStart
    private val nextPageDebounce by lazy { Debounce { keyPage(PageDirection.NEXT) } }
    private val prevPageDebounce by lazy { Debounce { keyPage(PageDirection.PREV) } }
    private var bookChanged = false
    private var pageChanged = false
    private val handler by lazy { buildMainHandler() }
    private val screenOffRunnable by lazy { Runnable { keepScreenOn(false) } }
    private val executor = ReadBook.executor
    private val upSeekBarThrottle = throttle(200) {
        runOnUiThread {
            upSeekBarProgress()
            upSeekBarState()
        }
    }

    //恢复跳转前进度对话框的交互结果
    private var confirmRestoreProcess: Boolean? = null
    private val networkChangedListener by lazy {
        NetworkChangedListener(this)
    }
    private var justInitData: Boolean = false
    private var syncDialog: AlertDialog? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // 在 Compose 工厂之前同步创建 ReadView，确保 register(this) 回调可访问
        readView = ReadView(this)
        // 初始化光标
        cursorLeft = ImageView(this).apply {
            setImageResource(R.drawable.ic_cursor_left)
            setColorFilter(accentColor)
            setOnTouchListener(this@ReadBookActivity)
            visibility = View.INVISIBLE
        }
        cursorRight = ImageView(this).apply {
            setImageResource(R.drawable.ic_cursor_right)
            setColorFilter(accentColor)
            setOnTouchListener(this@ReadBookActivity)
            visibility = View.INVISIBLE
        }
        // 设置 ComposeView
        binding.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.composeView.setContent {
            ReadBookScreen(
                state = readPageState,
                readView = readView,
                cursorLeft = cursorLeft,
                cursorRight = cursorRight,
                onBack = { finish() },
                onPrevChapter = {
                    readPageState.menuVisible = false
                    ReadBook.moveToPrevChapter(upContent = true, toLast = false)
                },
                onNextChapter = {
                    readPageState.menuVisible = false
                    ReadBook.moveToNextChapter(true)
                },
                onSeekTo = { index ->
                    when (AppConfig.progressBarBehavior) {
                        "page" -> ReadBook.skipToPage(index)
                        else -> {
                            if (confirmSkipToChapter) {
                                skipToChapter(index)
                            } else {
                                alert("章节跳转确认", "确定要跳转章节吗？") {
                                    yesButton {
                                        confirmSkipToChapter = true
                                        skipToChapter(index)
                                    }
                                    noButton { upSeekBarState() }
                                    onCancelled { upSeekBarState() }
                                }
                            }
                        }
                    }
                },
                onCatalog = {
                    readPageState.menuVisible = false
                    openChapterList()
                },
                onSearch = {
                    readPageState.menuVisible = false
                    openSearchActivity(null)
                },
                onAutoPage = {
                    readPageState.menuVisible = false
                    autoPage()
                },
                onReplaceRule = {
                    readPageState.menuVisible = false
                    openReplaceRule()
                },
                onToggleNightTheme = {
                    AppConfig.isNightTheme = !AppConfig.isNightTheme
                    readPageState.isNightTheme = AppConfig.isNightTheme
                    ThemeConfig.applyDayNight(this)
                },
                onToggleBrightnessAuto = { toggleBrightnessAuto() },
                onToggleBrightnessPos = { toggleBrightnessPos() },
                onBrightnessChange = { onBrightnessChange(it) },
                onBrightnessChangeFinished = { onBrightnessChangeFinished(it) },
                onReadAloud = {
                    readPageState.menuVisible = false
                    onClickReadAloud()
                },
                onFont = {
                    readPageState.menuVisible = false
                    showReadStyle()
                },
                onSetting = {
                    readPageState.menuVisible = false
                    showMoreSetting()
                },
                onDismissMenu = {
                    readPageState.menuVisible = false
                    onMenuHide()
                },
            )
        }
        window.setBackgroundDrawable(null)
        upScreenTimeOut()
        ReadBook.register(this)
        onBackPressedDispatcher.addCallback(this) {
            if (isShowingSearchResult) {
                exitSearchMenu()
                restoreLastBookProcess()
                return@addCallback
            }
            //拦截返回供恢复阅读进度
            if (ReadBook.lastBookProgress != null && confirmRestoreProcess != false) {
                restoreLastBookProcess()
                return@addCallback
            }
            if (BaseReadAloudService.isPlay()) {
                ReadAloud.pause(this@ReadBookActivity)
                toastOnUi(R.string.read_aloud_pause)
                return@addCallback
            }
            if (isAutoPage) {
                autoPageStop()
                return@addCallback
            }
            if (false && !menuLayoutIsVisible) {
                return@addCallback
            }
            finish()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        viewModel.initReadBookConfig(intent)
        Looper.myQueue().addIdleHandler {
            viewModel.initData(intent)
            false
        }
        justInitData = true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.initData(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        upSystemUiVisibility()
        if (hasFocus) {
            upBrightnessState()
        } else if (!menuLayoutIsVisible) {
            ReadBook.cancelPreDownloadTask()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        upSystemUiVisibility()
        readView.upStatusBar()
    }

    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        if (!isTopResumedActivity) {
            ReadBook.cancelPreDownloadTask()
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        ReadBook.readStartTime = System.currentTimeMillis()
        if (bookChanged) {
            bookChanged = false
            ReadBook.callBack = this
            viewModel.initData(intent)
            justInitData = true
        } else {
            //web端阅读时，app处于阅读界面，本地记录会覆盖web保存的进度，在此处恢复
            ReadBook.webBookProgress?.let {
                ReadBook.setProgress(it)
                ReadBook.webBookProgress = null
            }
        }
        upSystemUiVisibility()
        registerReceiver(timeBatteryReceiver, timeBatteryReceiver.filter)
        readView.upTime()
        screenOffTimerStart()
        // 网络监听，当从无网切换到网络环境时同步进度（注意注册的同时就会收到监听，因此界面激活时无需重复执行同步操作）
        networkChangedListener.register()
        networkChangedListener.onNetworkChanged = {
            // 当网络是可用状态且无需初始化时同步进度（初始化中已有同步进度逻辑）
            if (AppConfig.syncBookProgressPlus && NetworkUtils.isAvailable() && !justInitData && ReadBook.inBookshelf) {
                ReadBook.syncProgress({ progress -> sureNewProgress(progress) })
            }
        }
    }

    override fun onPause() {
        super.onPause()
        autoPageStop()
        backupJob?.cancel()
        ReadBook.saveRead()
        ReadBook.cancelPreDownloadTask()
        unregisterReceiver(timeBatteryReceiver)
        upSystemUiVisibility()
        if (!BuildConfig.DEBUG && ReadBook.inBookshelf) {
            if (AppConfig.syncBookProgressPlus) {
                ReadBook.syncProgress()
            } else {
                ReadBook.uploadProgress()
            }
        }
        if (!BuildConfig.DEBUG) {
            Backup.autoBack(this)
        }
        justInitData = false
        networkChangedListener.unRegister()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_read, menu)
        menu.iconItemOnLongClick(R.id.menu_change_source) {
            PopupMenu(this, it).apply {
                inflate(R.menu.book_read_change_source)
                this.menu.applyOpenTint(this@ReadBookActivity)
                setOnMenuItemClickListener(this@ReadBookActivity)
            }.show()
        }
        menu.iconItemOnLongClick(R.id.menu_refresh) {
            PopupMenu(this, it).apply {
                inflate(R.menu.book_read_refresh)
                this.menu.applyOpenTint(this@ReadBookActivity)
                setOnMenuItemClickListener(this@ReadBookActivity)
            }.show()
        }
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        upMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_same_title_removed)?.isChecked =
            ReadBook.curTextChapter?.sameTitleRemoved == true
        return super.onMenuOpened(featureId, menu)
    }

    /**
     * 更新菜单
     */
    private fun upMenu() {
        val menu = menu ?: return
        val book = ReadBook.book ?: return
        val onLine = !book.isLocal
        for (i in 0 until menu.size) {
            val item = menu[i]
            when (item.groupId) {
                R.id.menu_group_on_line -> item.isVisible = onLine
                R.id.menu_group_local -> item.isVisible = !onLine
                R.id.menu_group_text -> item.isVisible = book.isLocalTxt
                R.id.menu_group_epub -> item.isVisible = book.isEpub
                else -> when (item.itemId) {
                    R.id.menu_enable_replace -> item.isChecked = book.getUseReplaceRule()
                    R.id.menu_re_segment -> item.isChecked = book.getReSegment()
//                    R.id.menu_enable_review -> {
//                        item.isVisible = BuildConfig.DEBUG
//                        item.isChecked = AppConfig.enableReview
//                    }

                    R.id.menu_reverse_content -> item.isVisible = onLine
                    R.id.menu_del_ruby_tag -> item.isChecked = book.getDelTag(Book.rubyTag)
                    R.id.menu_del_h_tag -> item.isChecked = book.getDelTag(Book.hTag)
                }
            }
        }
        lifecycleScope.launch {
            val show = ReadBook.inBookshelf && withContext(IO) {
                AppWebDav.isOk
            }
            menu.findItem(R.id.menu_get_progress)?.isVisible = show
            menu.findItem(R.id.menu_cover_progress)?.isVisible = show
        }
    }

    /**
     * 菜单
     */
    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_change_source,
            R.id.menu_book_change_source -> {
                readPageState.menuVisible = false
                onMenuHide()
                ReadBook.book?.let {
                    showDialogFragment(ChangeBookSourceDialog(it.name, it.author))
                }
            }

            R.id.menu_chapter_change_source -> lifecycleScope.launch {
                val book = ReadBook.book ?: return@launch
                val chapter =
                    appDb.bookChapterDao.getChapter(book.bookUrl, ReadBook.durChapterIndex)
                        ?: return@launch
                readPageState.menuVisible = false
                onMenuHide()
                showDialogFragment(
                    ChangeChapterSourceDialog(book.name, book.author, chapter.index, chapter.title)
                )
            }

            R.id.menu_refresh,
            R.id.menu_refresh_dur -> {
                if (ReadBook.bookSource == null) {
                    upContent()
                } else {
                    ReadBook.book?.let {
                        ReadBook.curTextChapter = null
                        readView.upContent()
                        viewModel.refreshContentDur(it)
                    }
                }
            }

            R.id.menu_refresh_after -> {
                if (ReadBook.bookSource == null) {
                    upContent()
                } else {
                    ReadBook.book?.let {
                        ReadBook.clearTextChapter()
                        readView.upContent()
                        viewModel.refreshContentAfter(it)
                    }
                }
            }

            R.id.menu_refresh_all -> {
                if (ReadBook.bookSource == null) {
                    upContent()
                } else {
                    ReadBook.book?.let {
                        refreshContentAll(it)
                    }
                }
            }

            R.id.menu_download -> showDownloadDialog()
            R.id.menu_add_bookmark -> addBookmark()
            R.id.menu_simulated_reading -> showSimulatedReading()
            R.id.menu_edit_content -> showDialogFragment(ContentEditDialog())
            R.id.menu_update_toc -> ReadBook.book?.let {
                if (it.isEpub) {
                    BookHelp.clearCache(it)
                    EpubFile.clear()
                }
                if (it.isMobi) {
                    MobiFile.clear()
                }
                loadChapterList(it)
            }

            R.id.menu_enable_replace -> changeReplaceRuleState()
            R.id.menu_re_segment -> ReadBook.book?.let {
                it.setReSegment(!it.getReSegment())
                item.isChecked = it.getReSegment()
                ReadBook.loadContent(false)
            }

//            R.id.menu_enable_review -> {
//                AppConfig.enableReview = !AppConfig.enableReview
//                item.isChecked = AppConfig.enableReview
//                ReadBook.loadContent(false)
//            }

            R.id.menu_del_ruby_tag -> ReadBook.book?.let {
                item.isChecked = !item.isChecked
                if (item.isChecked) {
                    it.addDelTag(Book.rubyTag)
                } else {
                    it.removeDelTag(Book.rubyTag)
                }
                refreshContentAll(it)
            }

            R.id.menu_del_h_tag -> ReadBook.book?.let {
                item.isChecked = !item.isChecked
                if (item.isChecked) {
                    it.addDelTag(Book.hTag)
                } else {
                    it.removeDelTag(Book.hTag)
                }
                refreshContentAll(it)
            }

            R.id.menu_page_anim -> showPageAnimConfig {
                readView.upPageAnim()
                ReadBook.loadContent(false)
            }

            R.id.menu_toc_regex -> showDialogFragment(
                TxtTocRuleDialog(ReadBook.book?.tocUrl)
            )

            R.id.menu_reverse_content -> ReadBook.book?.let {
                viewModel.reverseContent(it)
            }

            R.id.menu_set_charset -> showCharsetConfig()
            R.id.menu_image_style -> {
                val imgStyles =
                    arrayListOf(
                        Book.imgStyleDefault, Book.imgStyleFull, Book.imgStyleText,
                        Book.imgStyleSingle
                    )
                selector(
                    R.string.image_style,
                    imgStyles
                ) { _, index ->
                    val imageStyle = imgStyles[index]
                    ReadBook.book?.setImageStyle(imageStyle)
                    if (imageStyle == Book.imgStyleSingle) {
                        ReadBook.book?.setPageAnim(0)  // 切换图片样式single后，自动切换为覆盖
                        readView.upPageAnim()
                    }
                    ReadBook.loadContent(false)
                }
            }

            R.id.menu_get_progress -> ReadBook.book?.let {
                viewModel.syncBookProgress(it) { progress ->
                    sureSyncProgress(progress)
                }
            }

            R.id.menu_cover_progress -> ReadBook.book?.let {
                ReadBook.uploadProgress(true) { toastOnUi(R.string.upload_book_success) }
            }

            R.id.menu_same_title_removed -> {
                ReadBook.book?.let {
                    val contentProcessor = ContentProcessor.get(it)
                    val textChapter = ReadBook.curTextChapter
                    if (textChapter != null
                        && !textChapter.sameTitleRemoved
                        && !contentProcessor.removeSameTitleCache.contains(
                            textChapter.chapter.getFileName("nr")
                        )
                    ) {
                        toastOnUi("未找到可移除的重复标题")
                    }
                }
                viewModel.reverseRemoveSameTitle()
            }

            R.id.menu_effective_replaces -> showDialogFragment<EffectiveReplacesDialog>()

            R.id.menu_help -> showHelp()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun refreshContentAll(book: Book) {
        ReadBook.clearTextChapter()
        readView.upContent()
        viewModel.refreshContentAll(book)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return onCompatOptionsItemSelected(item)
    }

    /**
     * 按键拦截,显示菜单
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val action = event.action
        val isDown = action == 0

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (isDown && !readPageState.canShowMenu) {
                readPageState.isNightTheme = AppConfig.isNightTheme
                readPageState.brightnessOnRight = AppConfig.brightnessVwPos
                readPageState.showBrightnessView = getPrefBoolean(PreferKey.showBrightnessView, true)
                upBrightnessState()
                upSeekBarState()
                readPageState.menuVisible = true
                onMenuShow()
                return true
            }
            if (!isDown && !readPageState.canShowMenu) {
                readPageState.canShowMenu = true
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * 鼠标滚轮事件
     */
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (0 != (event.source and InputDevice.SOURCE_CLASS_POINTER)) {
            if (event.action == MotionEvent.ACTION_SCROLL) {
                val axisValue = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                LogUtils.d("onGenericMotionEvent", "axisValue = $axisValue")
                // 获得垂直坐标上的滚动方向
                if (axisValue < 0.0f) { // 滚轮向下滚
                    mouseWheelPage(PageDirection.NEXT, axisValue)
                } else { // 滚轮向上滚
                    mouseWheelPage(PageDirection.PREV, axisValue)
                }
                return true
            }
        }
        return super.onGenericMotionEvent(event)
    }

    /**
     * 按键事件
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (menuLayoutIsVisible) {
            return super.onKeyDown(keyCode, event)
        }
        val longPress = event.repeatCount > 0
        when {
            isPrevKey(keyCode) -> {
                handleKeyPage(PageDirection.PREV, longPress)
                return true
            }

            isNextKey(keyCode) -> {
                handleKeyPage(PageDirection.NEXT, longPress)
                return true
            }
        }
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> if (volumeKeyPage(PageDirection.PREV, longPress)) {
                return true
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> if (volumeKeyPage(PageDirection.NEXT, longPress)) {
                return true
            }

            KeyEvent.KEYCODE_PAGE_UP -> {
                handleKeyPage(PageDirection.PREV, longPress)
                return true
            }

            KeyEvent.KEYCODE_PAGE_DOWN -> {
                handleKeyPage(PageDirection.NEXT, longPress)
                return true
            }

            KeyEvent.KEYCODE_SPACE -> {
                handleKeyPage(PageDirection.NEXT, longPress)
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    /**
     * 松开按键事件
     */
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (volumeKeyPage(PageDirection.NONE, false)) {
                    return true
                }
            }

        }
        return super.onKeyUp(keyCode, event)
    }

    /**
     * view触摸,文字选择
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (!readView.isTextSelected) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> textActionMenu.dismiss()
            MotionEvent.ACTION_MOVE -> {
                when {
                    v === cursorLeft -> if (!readView.curPage.getReverseStartCursor()) {
                        readView.curPage.selectStartMove(
                            event.rawX + cursorLeft.width,
                            event.rawY - cursorLeft.height
                        )
                    } else {
                        readView.curPage.selectEndMove(
                            event.rawX - cursorRight.width,
                            event.rawY - cursorRight.height
                        )
                    }

                    v === cursorRight -> if (readView.curPage.getReverseEndCursor()) {
                        readView.curPage.selectStartMove(
                            event.rawX + cursorLeft.width,
                            event.rawY - cursorLeft.height
                        )
                    } else {
                        readView.curPage.selectEndMove(
                            event.rawX - cursorRight.width,
                            event.rawY - cursorRight.height
                        )
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                readView.curPage.resetReverseCursor()
                showTextActionMenu()
            }
        }
        return true
    }

    /**
     * 更新文字选择开始位置
     */
    override fun upSelectedStart(x: Float, y: Float, top: Float) {
        cursorLeft.x = x - cursorLeft.width
        cursorLeft.y = y
        cursorLeft.visibility = View.VISIBLE
        textMenuPosX = x
        textMenuPosY = top
    }

    /**
     * 更新文字选择结束位置
     */
    override fun upSelectedEnd(x: Float, y: Float) {
        cursorRight.x = x
        cursorRight.y = y
        cursorRight.visibility = View.VISIBLE
    }

    /**
     * 取消文字选择
     */
    override fun onCancelSelect() {
        cursorLeft.visibility = View.INVISIBLE
        cursorRight.visibility = View.INVISIBLE
        textActionMenu.dismiss()
    }

    override fun onLongScreenshotTouchEvent(event: MotionEvent): Boolean {
        return readView.onTouchEvent(event)
    }

    /**
     * 显示文本操作菜单
     */
    override fun showTextActionMenu() {
        val navigationBarHeight = 0
        textActionMenu.show(
            cursorLeft,
            window.decorView.height + navigationBarHeight,
            textMenuPosX.toInt(),
            textMenuPosY.toInt(),
            cursorLeft.y.toInt() + cursorLeft.height,
            cursorRight.x.toInt(),
            cursorRight.y.toInt() + cursorRight.height
        )
    }

    /**
     * 当前选择的文本
     */
    override val selectedText: String get() = readView.getSelectText()

    /**
     * 文本选择菜单操作
     */
    override fun onMenuItemSelected(itemId: Int): Boolean {
        when (itemId) {
            R.id.menu_aloud -> when (AppConfig.contentSelectSpeakMod) {
                1 -> lifecycleScope.launch {
                    readView.aloudStartSelect()
                }

                else -> speak(readView.getSelectText())
            }

            R.id.menu_bookmark -> readView.curPage.let {
                val bookmark = it.createBookmark()
                if (bookmark == null) {
                    toastOnUi(R.string.create_bookmark_error)
                } else {
                    showDialogFragment(BookmarkDialog(bookmark))
                }
                return true
            }

            R.id.menu_replace -> {
                val scopes = arrayListOf<String>()
                ReadBook.book?.name?.let {
                    scopes.add(it)
                }
                ReadBook.bookSource?.bookSourceUrl?.let {
                    scopes.add(it)
                }
                val text = selectedText.lineSequence().joinToString("\n") { it.trim() }
                replaceActivity.launch(
                    ReplaceEditActivity.startIntent(
                        this,
                        pattern = text,
                        scope = scopes.joinToString(";")
                    )
                )
                return true
            }

            R.id.menu_search_content -> {
                viewModel.searchContentQuery = selectedText
                openSearchActivity(selectedText)
                return true
            }

            R.id.menu_dict -> {
                showDialogFragment(DictDialog(selectedText))
                return true
            }
        }
        return false
    }

    /**
     * 文本选择菜单操作完成
     */
    override fun onMenuActionFinally() {
        textActionMenu.dismiss()
        readView.cancelSelect()
    }

    private fun speak(text: String) {
        if (tts == null) {
            tts = TTS()
        }
        tts?.speak(text)
    }

    /**
     * 鼠标滚轮翻页
     */
    private fun mouseWheelPage(direction: PageDirection, distance: Float) {
        if (menuLayoutIsVisible || !AppConfig.mouseWheelPage) {
            return
        }
        if (readView.isScroll) {
            // 滚动视图时滚动,否则翻页
            (readView.pageDelegate as? ScrollPageDelegate)?.curPage?.scroll((distance * 50).toInt())
        } else {
            keyPageDebounce(direction, mouseWheel = true, longPress = false)
        }
    }

    /**
     * 音量键翻页
     */
    private fun volumeKeyPage(direction: PageDirection, longPress: Boolean): Boolean {
        if (!AppConfig.volumeKeyPage) {
            return false
        }
        if (!AppConfig.volumeKeyPageOnPlay && BaseReadAloudService.isPlay()) {
            return false
        }
        handleKeyPage(direction, longPress)
        return true
    }

    private fun handleKeyPage(direction: PageDirection, longPress: Boolean) {
        if (AppConfig.keyPageOnLongPress || direction == PageDirection.NONE) {
            keyPage(direction)
        } else {
            keyPageDebounce(direction, longPress = longPress)
        }
    }

    private fun keyPageDebounce(
        direction: PageDirection,
        mouseWheel: Boolean = false,
        longPress: Boolean
    ) {
        if (longPress) {
            return
        }
        nextPageDebounce.apply {
            wait = if (mouseWheel) 200L else 600L
            leading = !mouseWheel
            trailing = mouseWheel
        }
        prevPageDebounce.apply {
            wait = if (mouseWheel) 200L else 600L
            leading = !mouseWheel
            trailing = mouseWheel
        }
        when (direction) {
            PageDirection.NEXT -> nextPageDebounce.invoke()
            PageDirection.PREV -> prevPageDebounce.invoke()
            else -> {}
        }
    }

    private fun keyPage(direction: PageDirection) {
        readView.cancelSelect()
        readView.pageDelegate?.isCancel = false
        readView.pageDelegate?.keyTurnPage(direction)
    }

    override fun upMenuView() {
        handler.post {
            upMenu()
            upBookViewState()
        }
    }

    override fun loadChapterList(book: Book) {
        ReadBook.upMsg(getString(R.string.toc_updateing))
        viewModel.loadChapterList(book)
    }

    /**
     * 内容加载完成
     */
    override fun contentLoadFinish() {
        if (intent.getBooleanExtra("readAloud", false)) {
            intent.removeExtra("readAloud")
            ReadBook.readAloud()
        }
        loadStates = true
    }

    /**
     * 更新内容
     */
    override fun upContent(
        relativePosition: Int,
        resetPageOffset: Boolean,
        success: (() -> Unit)?
    ) {
        lifecycleScope.launch {
            readView.upContent(relativePosition, resetPageOffset)
            if (relativePosition == 0) {
                upSeekBarProgress()
            }
            loadStates = false
            success?.invoke()
        }
    }

    override suspend fun upContentAwait(
        relativePosition: Int,
        resetPageOffset: Boolean,
        success: (() -> Unit)?
    ) = withContext(Main.immediate) {
        readView.upContent(relativePosition, resetPageOffset)
        if (relativePosition == 0) {
            upSeekBarProgress()
        }
        loadStates = false
    }

    override fun upPageAnim(upRecorder: Boolean) {
        lifecycleScope.launch {
            readView.upPageAnim(upRecorder)
        }
    }

    override fun notifyBookChanged() {
        bookChanged = true
        if (!ReadBook.inBookshelf) {
            viewModel.removeFromBookshelf { super.finish() }
        }
    }

    override fun cancelSelect() {
        runOnUiThread {
            readView.cancelSelect()
        }
    }

    /**
     * 页面改变
     */
    override fun pageChanged() {
        pageChanged = true
        readView.onPageChange()
        handler.post {
            upSeekBarProgress()
        }
        executor.execute {
            startBackupJob()
        }
    }

    /**
     * 更新进度条位置
     */
    private fun upSeekBarProgress() {
        val progress = when (AppConfig.progressBarBehavior) {
            "page" -> ReadBook.durPageIndex
            else /* chapter */ -> ReadBook.durChapterIndex
        }
        readPageState.seekPageText = progress.toString()
    }

    private fun upSeekBarState() {
        if (readPageState.isDraggingSeek) return
        when (AppConfig.progressBarBehavior) {
            "page" -> {
                val chapter = ReadBook.curTextChapter ?: return
                readPageState.seekMax = chapter.pageSize - 1
                readPageState.seekProgress = ReadBook.durPageIndex
            }
            else -> {
                readPageState.seekMax = ReadBook.simulatedChapterSize - 1
                readPageState.seekProgress = ReadBook.durChapterIndex
            }
        }
    }

    private fun upBookViewState() {
        val book = ReadBook.book ?: return
        readPageState.chapterName = book.durChapterTitle ?: ""
        readPageState.isLocalBook = book.isLocal
        upSeekBarState()
    }

    private fun upBrightnessState() {
        readPageState.brightness = AppConfig.readBrightness.toFloat()
        readPageState.brightnessAuto = brightnessAuto()
        setScreenBrightness(AppConfig.readBrightness.toFloat())
    }

    private fun brightnessAuto(): Boolean =
        getPrefBoolean("brightnessAuto", false)

    fun toggleBrightnessAuto() {
        putPrefBoolean("brightnessAuto", !brightnessAuto())
        upBrightnessState()
    }

    fun toggleBrightnessPos() {
        AppConfig.brightnessVwPos = !AppConfig.brightnessVwPos
        readPageState.brightnessOnRight = AppConfig.brightnessVwPos
    }

    fun onBrightnessChange(value: Float) {
        setScreenBrightness(value)
    }

    fun onBrightnessChangeFinished(value: Float) {
        AppConfig.readBrightness = value.toInt().coerceIn(0, 255)
        readPageState.brightness = AppConfig.readBrightness.toFloat()
    }

    /**
     * 系统亮度监听，在高阳光亮度时启用
     */
    private var contentObserver: ContentObserver? = null

    /**
     * 设置屏幕亮度（自原版 ReadMenu 迁入）
     */
    fun setScreenBrightness(value: Float) {
        fun setBrightness(v: Float) {
            val params = window.attributes
            params.screenBrightness = v
            window.attributes = params
        }
        val autoBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        if (brightnessAuto() || value == autoBrightness) {
            setBrightness(autoBrightness)
            return
        }
        val brightness = if (value < 1f) 0.004f else value / 255f
        var isSunMax = false
        if (brightness == 1f) {
            val sysBrightness = getCurrentBrightness()
            if (sysBrightness == 255) {
                isSunMax = true
            }
        }
        if (isSunMax) {
            contentObserver = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    if (contentObserver == null) return
                    if (uri == Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS)) {
                        val sysBrightness = getCurrentBrightness()
                        if (sysBrightness < 200) {
                            setBrightness(brightness)
                            contentObserver?.let {
                                contentResolver.unregisterContentObserver(it)
                            }
                            contentObserver = null
                        } else if (sysBrightness < 255) {
                            setBrightness(brightness)
                        } else {
                            setBrightness(autoBrightness)
                        }
                    }
                }
            }
            val brightnessUri = Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS)
            contentResolver.registerContentObserver(brightnessUri, false, contentObserver!!)
            setBrightness(autoBrightness)
        } else {
            setBrightness(brightness)
        }
    }

    private fun getCurrentBrightness(): Int {
        return try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (_: Settings.SettingNotFoundException) {
            -1
        }
    }

    /**
     * 显示菜单
     */
    override fun showMenuBar() {
        readPageState.isNightTheme = AppConfig.isNightTheme
        readPageState.brightnessOnRight = AppConfig.brightnessVwPos
        readPageState.showBrightnessView = getPrefBoolean(PreferKey.showBrightnessView, true)
        upBrightnessState()
        upSeekBarState()
        readPageState.menuVisible = true
        onMenuShow()
    }

    override val oldBook: Book?
        get() = ReadBook.book

    override fun changeTo(source: BookSource, book: Book, toc: List<BookChapter>) {
        if (!book.isAudio) {
            viewModel.changeTo(book, toc)
        } else {
            ReadAloud.stop(this)
            lifecycleScope.launch {
                withContext(IO) {
                    ReadBook.book?.migrateTo(book, toc)
                    book.removeType(BookType.updateError)
                    ReadBook.book?.delete()
                    appDb.bookDao.insert(book)
                }
                startActivityForBook(book)
                finish()
            }
        }
    }

    override fun replaceContent(content: String) {
        ReadBook.book?.let {
            viewModel.saveContent(it, content)
        }
    }

    override fun showActionMenu() {
        when {
            BaseReadAloudService.isRun -> showReadAloudDialog()
            isAutoPage -> showDialogFragment<AutoReadDialog>()
            isShowingSearchResult -> {
                readPageState.searchMenuVisible = true
                onMenuShow()
            }
            else -> {
                readPageState.isNightTheme = AppConfig.isNightTheme
                readPageState.brightnessOnRight = AppConfig.brightnessVwPos
                readPageState.showBrightnessView = getPrefBoolean(PreferKey.showBrightnessView, true)
                upBrightnessState()
                upSeekBarState()
                readPageState.menuVisible = true
                onMenuShow()
            }
        }
    }

    /**
     * 显示朗读菜单
     */
    fun showReadAloudDialog() {
        showDialogFragment<ReadAloudDialog>()
    }

    /**
     * 自动翻页
     */
    fun autoPage() {
        ReadAloud.stop(this)
        if (isAutoPage) {
            autoPageStop()
        } else {
            readView.autoPager.start()
            readPageState.autoPage = true
            screenTimeOut = -1L
            screenOffTimerStart()
        }
    }

    override fun autoPageStop() {
        if (isAutoPage) {
            readView.autoPager.stop()
            readPageState.autoPage = false
            dismissDialogFragment<AutoReadDialog>()
            upScreenTimeOut()
        }
    }

    fun openSourceEditActivity() {
        ReadBook.bookSource?.let {
            sourceEditActivity.launch {
                putExtra("sourceUrl", it.bookSourceUrl)
            }
        }
    }

    fun openBookInfoActivity() {
        ReadBook.book?.let {
            bookInfoActivity.launch {
                putExtra("name", it.name)
                putExtra("author", it.author)
            }
        }
    }

    /**
     * 替换
     */
    fun openReplaceRule() {
        replaceActivity.launch(Intent(this, ReplaceRuleActivity::class.java))
    }

    /**
     * 打开目录
     */
    override fun openChapterList() {
        ReadBook.book?.let {
            tocActivity.launch(it.bookUrl)
        }
    }

    /**
     * 打开搜索界面
     */
    override fun openSearchActivity(searchWord: String?) {
        val book = ReadBook.book ?: return
        searchContentActivity.launch {
            putExtra("bookUrl", book.bookUrl)
            putExtra("searchWord", searchWord ?: viewModel.searchContentQuery)
            putExtra("searchResultIndex", viewModel.searchResultIndex)
            viewModel.searchResultList?.first()?.let {
                if (it.query == viewModel.searchContentQuery) {
                    IntentData.put("searchResultList", viewModel.searchResultList)
                }
            }
        }
    }

    /**
     * 禁用书源
     */
    fun disableSource() {
        viewModel.disableSource()
    }

    /**
     * 显示阅读样式配置
     */
    fun showReadStyle() {
        showDialogFragment<ReadStyleDialog>()
    }

    /**
     * 显示更多设置
     */
    fun showMoreSetting() {
        showDialogFragment<MoreConfigDialog>()
    }

    fun showSearchSetting() {
        showDialogFragment<MoreConfigDialog>()
    }

    /**
     * 更新状态栏,导航栏
     */
    override fun upSystemUiVisibility() {
        upSystemUiVisibility(isInMultiWindow, !menuLayoutIsVisible, bottomDialog > 0)
        upNavigationBarColor()
    }

    // 退出全文搜索
    fun exitSearchMenu() {
        if (isShowingSearchResult) {
            isShowingSearchResult = false
            readPageState.searchMenuVisible = false
            ReadBook.clearSearchResult()
            readView.cancelSelect(true)
        }
    }

    /* 恢复到 全文搜索/进度条跳转前的位置 */
    private fun restoreLastBookProcess() {
        if (confirmRestoreProcess == true) {
            ReadBook.restoreLastBookProgress()
        } else if (confirmRestoreProcess == null) {
            alert(R.string.draw) {
                setMessage(R.string.restore_last_book_process)
                yesButton {
                    confirmRestoreProcess = true
                    ReadBook.restoreLastBookProgress() //恢复启动全文搜索前的进度
                }
                noButton {
                    ReadBook.lastBookProgress = null
                    confirmRestoreProcess = false
                }
                onCancelled {
                    ReadBook.lastBookProgress = null
                    confirmRestoreProcess = false
                }
            }
        }
    }

    fun showLogin() {
        ReadBook.bookSource?.let {
            startActivity<SourceLoginActivity> {
                putExtra("bookType", BookType.text)
            }
        }
    }

    fun payAction() {
        val book = ReadBook.book ?: return
        if (book.isLocal) return
        val chapter = appDb.bookChapterDao.getChapter(book.bookUrl, ReadBook.durChapterIndex)
        if (chapter == null) {
            toastOnUi("no chapter")
            return
        }
        alert(R.string.chapter_pay) {
            setMessage(chapter.title)
            yesButton {
                Coroutine.async(lifecycleScope) {
                    val source =
                        ReadBook.bookSource ?: throw NoStackTraceException("no book source")
                    val payAction = source.getContentRule().payAction
                    if (payAction.isNullOrBlank()) {
                        throw NoStackTraceException("no pay action")
                    }
                    val java = SourceLoginJsExtensions(this@ReadBookActivity, source, BookType.text)
                    runScriptWithContext {
                        source.evalJS(payAction) {
                            put("java", java)
                            put("book", book)
                            put("chapter", chapter)
                            put("title", chapter.title)
                            put("baseUrl", chapter.url)
                            put("result", null)
                            put("src", null)
                        }.toString()
                    }
                }.onSuccess(IO) {
                    if (it.isAbsUrl()) {
                        startActivity<WebViewActivity> {
                            val bookSource = ReadBook.bookSource
                            putExtra("title", getString(R.string.chapter_pay))
                            putExtra("url", it)
                            putExtra("sourceOrigin", bookSource?.bookSourceUrl)
                            putExtra("sourceName", bookSource?.bookSourceName)
                            putExtra("sourceType", bookSource?.getSourceType())
                        }
                    } else if (it.isTrue()) {
                        //购买成功后刷新目录
                        ReadBook.book?.let {
                            ReadBook.curTextChapter = null
                            BookHelp.delContent(book, chapter)
                            loadChapterList(book)
                        }
                    }
                }.onError {
                    AppLog.put("执行购买操作出错\n${it.localizedMessage}", it, true)
                }
            }
            noButton()
        }
    }

    /**
     * 点击图片
     */
    override fun oldClickImg(src: String): Boolean {
        val urlMatcher = paramPattern.matcher(src)
        if (urlMatcher.find()) {
            val urlOptionStr = src.substring(urlMatcher.end())
            val urlOptionMap = GSON.fromJsonObject<Map<String, String>>(urlOptionStr).getOrNull()
            val click = urlOptionMap?.get("click")
            if (click != null) {
                Coroutine.async(lifecycleScope,IO) {
                    val source = ReadBook.bookSource ?: return@async
                    val java = SourceLoginJsExtensions(this@ReadBookActivity, source, BookType.text)
                    val book = ReadBook.book ?: return@async
                    val chapter = appDb.bookChapterDao.getChapter(book.bookUrl, ReadBook.durChapterIndex) ?: throw Exception("no find chapter")
                    runScriptWithContext {
                        source.evalJS(click) {
                            put("java", java)
                            put("book", book)
                            put("chapter", chapter)
                            put("result", src)
                        }
                    }
                }.onError {
                    AppLog.put("执行图片链接click键值出错\n${it.localizedMessage}", it, true)
                }
                return true
            }
            val jsStr = urlOptionMap?.get("js") ?: return false
            Coroutine.async(lifecycleScope, IO) {
                val source = ReadBook.bookSource ?: return@async
                val book = ReadBook.book ?: return@async
                val chapter = appDb.bookChapterDao.getChapter(book.bookUrl, ReadBook.durChapterIndex) ?: throw Exception("no find chapter")
                val urlNoOption = src.take(urlMatcher.start())
                AnalyzeRule(book, source).apply {
                    setCoroutineContext(coroutineContext)
                    setBaseUrl(chapter.url)
                    setChapter(chapter)
                    evalJS(jsStr, urlNoOption)
                }
            }.onError {
                AppLog.put("执行图片链接js键值出错\n${it.localizedMessage}", it, true)
            }
            return true
        }
        return false
    }

    override fun clickImg(click: String, src: String) {
        Coroutine.async(lifecycleScope,IO) {
            val source = ReadBook.bookSource ?: return@async
            val java = SourceLoginJsExtensions(this@ReadBookActivity, source, BookType.text)
            val book = ReadBook.book ?: return@async
            val chapter = appDb.bookChapterDao.getChapter(book.bookUrl, ReadBook.durChapterIndex) ?: throw Exception("no find chapter")
            runScriptWithContext {
                source.evalJS(click) {
                    put("java", java)
                    put("book", book)
                    put("chapter", chapter)
                    put("result", src)
                }
            }
        }.onError {
            AppLog.put("执行图片链接click键值出错\n${it.localizedMessage}", it, true)
        }
    }


    /**
     * 朗读按钮
     */
    override fun onClickReadAloud() {
        autoPageStop()
        when {
            !BaseReadAloudService.isRun -> {
                ReadAloud.upReadAloudClass()
                val scrollPageAnim = ReadBook.pageAnim() == 3
                if (scrollPageAnim) {
                    val pos = readView.getReadAloudPos()
                    if (pos != null) {
                        val (index, line) = pos
                        if (ReadBook.durChapterIndex != index) {
                            ReadBook.openChapter(index, line.chapterPosition, false) {
                                ReadBook.readAloud(startPos = line.pagePosition)
                            }
                        } else {
                            ReadBook.durChapterPos = line.chapterPosition
                            ReadBook.readAloud(startPos = line.pagePosition)
                        }
                    } else {
                        ReadBook.readAloud()
                    }
                } else {
                    ReadBook.readAloud()
                }
            }

            BaseReadAloudService.pause -> {
                val scrollPageAnim = ReadBook.pageAnim() == 3
                if (scrollPageAnim && pageChanged) {
                    pageChanged = false
                    val pos = readView.getReadAloudPos()
                    if (pos != null) {
                        val (index, line) = pos
                        if (ReadBook.durChapterIndex != index) {
                            ReadBook.openChapter(index, line.chapterPosition, false) {
                                ReadBook.readAloud(startPos = line.pagePosition)
                            }
                        } else {
                            ReadBook.durChapterPos = line.chapterPosition
                            ReadBook.readAloud(startPos = line.pagePosition)
                        }
                    } else {
                        ReadBook.readAloud()
                    }
                } else {
                    ReadAloud.resume(this)
                }
            }

            else -> ReadAloud.pause(this)
        }
    }

    fun showHelp() {
        showHelp("readMenuHelp")
    }

    /**
     * 长按图片
     */
    @SuppressLint("RtlHardcoded")
    override fun onImageLongPress(x: Float, y: Float, src: String) {
        popupAction.setItems(
            listOf(
                SelectItem(getString(R.string.show), "show"),
                SelectItem(getString(R.string.refresh), "refresh"),
                SelectItem(getString(R.string.action_save), "save"),
                SelectItem(getString(R.string.menu), "menu"),
                SelectItem(getString(R.string.select_folder), "selectFolder")
            )
        )
        popupAction.onActionClick = {
            when (it) {
                "show" -> showDialogFragment(PhotoDialog(src, isBook = true))
                "refresh" -> viewModel.refreshImage(src)
                "save" -> {
                    val path = ACache.get().getAsString(AppConst.imagePathKey)
                    if (path.isNullOrEmpty()) {
                        selectImageDir.launch {
                            value = src
                        }
                    } else {
                        viewModel.saveImage(src, path.toUri())
                    }
                }

                "menu" -> showActionMenu()
                "selectFolder" -> selectImageDir.launch()
            }
            popupAction.dismiss()
        }
        val navigationBarHeight = 0
        popupAction.showAtLocation(
            readView, Gravity.BOTTOM or Gravity.LEFT, x.toInt(),
            window.decorView.height + navigationBarHeight - y.toInt()
        )
    }

    /**
     * colorSelectDialog
     */
    override fun onColorSelected(dialogId: Int, color: Int) = ReadBookConfig.durConfig.run {
        when (dialogId) {
            TEXT_COLOR -> {
                setCurTextColor(color)
                postEvent(EventBus.UP_CONFIG, arrayListOf(2, 6, 9, 11))
                if (AppConfig.readBarStyleFollowPage) {
                    postEvent(EventBus.UPDATE_READ_ACTION_BAR, true)
                }
            }

            TEXT_ACCENT_COLOR -> {
                setCurTextAccentColor(color)
                postEvent(EventBus.UP_CONFIG, arrayListOf(2, 6, 9, 11))
                if (AppConfig.readBarStyleFollowPage) {
                    postEvent(EventBus.UPDATE_READ_ACTION_BAR, true)
                }
            }

            BG_COLOR -> {
                setCurBg(0, "#${color.hexString}")
                postEvent(EventBus.UP_CONFIG, arrayListOf(1))
                if (AppConfig.readBarStyleFollowPage) {
                    postEvent(EventBus.UPDATE_READ_ACTION_BAR, true)
                }
            }

            TIP_COLOR -> {
                ReadTipConfig.tipColor = color
                postEvent(EventBus.TIP_COLOR, "")
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }

            TIP_DIVIDER_COLOR -> {
                ReadTipConfig.tipDividerColor = color
                postEvent(EventBus.TIP_COLOR, "")
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }
        }
    }

    /**
     * colorSelectDialog
     */
    override fun onDialogDismissed(dialogId: Int) = Unit

    override fun onTocRegexDialogResult(tocRegex: String) {
        ReadBook.book?.let {
            it.tocUrl = tocRegex
            loadChapterList(it)
        }
    }

    private fun sureSyncProgress(progress: BookProgress) {
        alert(R.string.get_book_progress) {
            setMessage(R.string.current_progress_exceeds_cloud)
            okButton {
                ReadBook.setProgress(progress)
            }
            noButton()
        }
    }

    /* 进度条跳转到指定章节 */
    fun skipToChapter(index: Int) {
        ReadBook.saveCurrentBookProgress() //退出章节跳转恢复此时进度
        viewModel.openChapter(index)
    }

    /* 全文搜索跳转 */
    fun navigateToSearch(searchResult: SearchResult, index: Int) {
        viewModel.searchResultIndex = index
        skipToSearch(searchResult)
    }

    override fun onMenuShow() {
        readView.autoPager.pause()
    }

    override fun onMenuHide() {
        readView.autoPager.resume()
    }

    override fun onLayoutPageCompleted(index: Int, page: TextPage) {
        upSeekBarThrottle.invoke()
        readView.onLayoutPageCompleted(index, page)
    }

    /* 全文搜索跳转 */
    private fun skipToSearch(searchResult: SearchResult) {
        if (searchResult.chapterIndex != ReadBook.durChapterIndex) {
            viewModel.openChapter(searchResult.chapterIndex) {
                jumpToPosition(searchResult)
            }
        } else {
            jumpToPosition(searchResult)
        }
    }

    private fun jumpToPosition(searchResult: SearchResult) {
        val curTextChapter = ReadBook.curTextChapter ?: return
        val searchResultPositions =
            viewModel.searchResultPositions(curTextChapter, searchResult)
        val (pageIndex, lineIndex, charIndex, addLine, charIndex2) = searchResultPositions
        ReadBook.skipToPage(pageIndex) {
            isSelectingSearchResult = true
            readView.curPage.selectStartMoveIndex(0, lineIndex, charIndex)
            when (addLine) {
                0 -> readView.curPage.selectEndMoveIndex(
                    0,
                    lineIndex,
                    charIndex + searchResultPositions[5] - 1
                )

                1 -> readView.curPage.selectEndMoveIndex(
                    0, lineIndex + 1, charIndex2
                )
                //consider change page, jump to scroll position
                -1 -> readView.curPage.selectEndMoveIndex(1, 0, charIndex2)
            }
            readView.isTextSelected = true
            isSelectingSearchResult = false
        }
    }

    override fun addBookmark() {
        val book = ReadBook.book
        val page = ReadBook.curTextChapter?.getPage(ReadBook.durPageIndex)
        if (book != null && page != null) {
            val bookmark = book.createBookMark().apply {
                chapterIndex = ReadBook.durChapterIndex
                chapterPos = ReadBook.durChapterPos
                chapterName = page.title
                bookText = page.text.trim()
            }
            showDialogFragment(BookmarkDialog(bookmark))
        }
    }

    override fun changeReplaceRuleState() {
        ReadBook.book?.let {
            it.setUseReplaceRule(!it.getUseReplaceRule())
            ReadBook.saveRead()
            menu?.findItem(R.id.menu_enable_replace)?.isChecked = it.getUseReplaceRule()
            viewModel.replaceRuleChanged()
        }
    }

    private fun startBackupJob() {
        backupJob?.cancel()
        backupJob = lifecycleScope.launch(IO) {
            delay(300000)
            ReadBook.book?.let {
                AppWebDav.uploadBookProgress(it)
                ensureActive()
                it.update()
                Backup.autoBack(this@ReadBookActivity)
            }
        }
    }

    override fun sureNewProgress(progress: BookProgress) {
        syncDialog?.dismiss()
        syncDialog = alert(R.string.get_book_progress) {
            setMessage(R.string.cloud_progress_exceeds_current)
            okButton {
                ReadBook.setProgress(progress)
            }
            noButton()
        }
    }

    override fun finish() {
        val book = ReadBook.book ?: return super.finish()
        if (ReadBook.inBookshelf) {
            callBackBookEnd()
            return super.finish()
        }
        if (!AppConfig.showAddToShelfAlert) {
            callBackBookEnd()
            viewModel.removeFromBookshelf { super.finish() }
        } else {
            alert(title = getString(R.string.add_to_bookshelf)) {
                setMessage(getString(R.string.check_add_bookshelf, book.name))
                okButton {
                    ReadBook.book?.removeType(BookType.notShelf)
                    ReadBook.book?.save()
                    SourceCallBack.callBackBook(SourceCallBack.ADD_BOOK_SHELF, ReadBook.bookSource, ReadBook.book)
                    ReadBook.inBookshelf = true
                    setResult(RESULT_OK)
                }
                noButton {
                    callBackBookEnd()
                    viewModel.removeFromBookshelf { super.finish() }
                }
            }
        }
    }

    private fun callBackBookEnd() {
        SourceCallBack.callBackBook(SourceCallBack.END_READ, ReadBook.bookSource, ReadBook.book, ReadBook.curTextChapter?.chapter)
    }

    override fun onDestroy() {
        super.onDestroy()
        contentObserver?.let {
            contentResolver.unregisterContentObserver(it)
            contentObserver = null
        }
        tts?.clearTts()
        textActionMenu.dismiss()
        popupAction.dismiss()
        if (::readView.isInitialized) readView.onDestroy()
        ReadBook.unregister(this)
        handler.removeCallbacksAndMessages(null) // 清理Handler消息
        if (!ReadBook.inBookshelf && !isChangingConfigurations) {
            viewModel.removeFromBookshelf(null)
        }
        if (!BuildConfig.DEBUG) {
            Backup.autoBack(this)
        }
    }

    override fun observeLiveBus() {
        observeEvent<String>(EventBus.TIME_CHANGED) { readView.upTime() }
        observeEvent<Int>(EventBus.BATTERY_CHANGED) { readView.upBattery(it) }
        observeEvent<Boolean>(EventBus.MEDIA_BUTTON) {
            if (it) {
                onClickReadAloud()
            } else {
                ReadBook.readAloud(!BaseReadAloudService.pause)
            }
        }
        observeEvent<ArrayList<Int>>(EventBus.UP_CONFIG) {
            it.forEach { value ->
                when (value) {
                    0 -> upSystemUiVisibility()
                    1 -> readView.upBg()
                    2 -> readView.upStyle()
                    3 -> readView.upBgAlpha()
                    4 -> readView.upPageSlopSquare()
                    5 -> if (isInitFinish) ReadBook.loadContent(resetPageOffset = false)
                    6 -> readView.upContent(resetPageOffset = false)
                    8 -> ChapterProvider.upStyle()
                    9 -> readView.invalidateTextPage()
                    10 -> ChapterProvider.upLayout()
                    11 -> readView.submitRenderTask()
                    12 -> readView.upPageTouchClick()
                    13 -> upPageAnim()
                }
            }
        }
        observeEvent<Int>(EventBus.ALOUD_STATE) {
            if (it == Status.STOP || it == Status.PAUSE) {
                ReadBook.curTextChapter?.let { textChapter ->
                    val page = textChapter.getPageByReadPos(ReadBook.durChapterPos)
                    if (page != null) {
                        page.removePageAloudSpan()
                        readView.upContent(resetPageOffset = false)
                    }
                }
            }
        }
        observeEventSticky<Int>(EventBus.TTS_PROGRESS) { chapterStart ->
            lifecycleScope.launch(IO) {
                if (BaseReadAloudService.isPlay()) {
                    ReadBook.curTextChapter?.let { textChapter ->
                        ReadBook.durChapterPos = chapterStart
                        val pageIndex = ReadBook.durPageIndex
                        val aloudSpanStart = chapterStart - textChapter.getReadLength(pageIndex)
                        textChapter.getPage(pageIndex)
                            ?.upPageAloudSpan(aloudSpanStart)
                        upContent()
                    }
                }
            }
        }
        observeEvent<Boolean>(PreferKey.keepLight) {
            upScreenTimeOut()
        }
        observeEvent<Boolean>(PreferKey.textSelectAble) {
            readView.curPage.upSelectAble(it)
        }
        observeEvent<String>(PreferKey.showBrightnessView) {
            upBrightnessState()
        }
        observeEvent<List<SearchResult>>(EventBus.SEARCH_RESULT) {
            viewModel.searchResultList = it
        }
        observeEvent<Boolean>(EventBus.UPDATE_READ_ACTION_BAR) {
            upMenuView()
        }
        observeEvent<Boolean>(EventBus.UP_SEEK_BAR) {
            upSeekBarState()
        }
        observeEvent<Boolean>(EventBus.REFRESH_BOOK_CONTENT) { //书源js函数触发刷新
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                ReadBook.book?.let {
                    ReadBook.curTextChapter = null
                    readView.upContent()
                    viewModel.refreshContentDur(it)
                }
            }
        }
        observeEvent<Boolean>(EventBus.REFRESH_BOOK_TOC) { //书源js函数触发刷新
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                ReadBook.book?.let {
                    loadChapterList(it)
                }
            }
        }
    }

    private fun upScreenTimeOut() {
        // 屏幕超时固定默认（跟随系统），设置入口已移除；自动翻页仍可通过 screenTimeOut=-1 保持常亮
        screenTimeOut = 0L
        screenOffTimerStart()
    }

    /**
     * 重置黑屏时间
     */
    override fun screenOffTimerStart() {
        handler.post {
            if (screenTimeOut < 0) {
                keepScreenOn(true)
                return@post
            }
            val t = screenTimeOut - sysScreenOffTime
            if (t > 0) {
                keepScreenOn(true)
                handler.removeCallbacks(screenOffRunnable)
                handler.postDelayed(screenOffRunnable, screenTimeOut)
            } else {
                keepScreenOn(false)
            }
        }
    }

    companion object {
        const val RESULT_DELETED = 100
    }

}
