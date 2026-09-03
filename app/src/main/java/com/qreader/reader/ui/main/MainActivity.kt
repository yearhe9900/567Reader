@file:Suppress("DEPRECATION")

package com.qreader.reader.ui.main

import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.postDelayed
import androidx.lifecycle.lifecycleScope
import com.qreader.reader.BuildConfig
import com.qreader.reader.R
import com.qreader.reader.base.VMBaseActivity
import com.qreader.reader.constant.AppConst.appInfo
import com.qreader.reader.constant.EventBus
import com.qreader.reader.constant.PreferKey
import com.qreader.reader.databinding.ActivityMainBinding
import com.qreader.reader.help.AppWebDav
import com.qreader.reader.help.LifecycleHelp
import com.qreader.reader.help.book.BookHelp
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.help.config.LocalConfig
import com.qreader.reader.help.config.ThemeConfig
import com.qreader.reader.help.coroutine.Coroutine
import com.qreader.reader.help.storage.Backup
import com.qreader.reader.lib.dialogs.alert
import com.qreader.reader.service.BaseReadAloudService
import com.qreader.reader.service.WebService
import com.qreader.reader.ui.about.CrashLogsDialog
import com.qreader.reader.ui.association.ImportBookSourceDialog
import com.qreader.reader.ui.association.ImportDictRuleDialog
import com.qreader.reader.ui.association.ImportHttpTtsDialog
import com.qreader.reader.ui.association.ImportReplaceRuleDialog
import com.qreader.reader.ui.association.ImportRssSourceDialog
import com.qreader.reader.ui.association.ImportTxtTocRuleDialog
import com.qreader.reader.ui.book.bookmark.AllBookmarkActivity
import com.qreader.reader.ui.book.cache.CacheActivity
import com.qreader.reader.ui.book.info.BookInfoActivity
import com.qreader.reader.ui.book.search.SearchActivity
import com.qreader.reader.ui.book.source.edit.BookSourceEditActivity
import com.qreader.reader.ui.book.source.manage.BookSourceActivity
import com.qreader.reader.ui.book.toc.rule.TxtTocRuleActivity
import com.qreader.reader.ui.compose.GlassDemoActivity
import com.qreader.reader.ui.compose.NavBarGlassSettingsActivity
import com.qreader.reader.ui.compose.liquid.NavBarGlassConfig
import com.qreader.reader.ui.config.ConfigActivity
import com.qreader.reader.ui.config.ConfigTag
import com.qreader.reader.ui.dict.rule.DictRuleActivity
import com.qreader.reader.ui.file.FileManageActivity
import com.qreader.reader.ui.book.explore.ExploreShowActivity
import com.qreader.reader.ui.about.ReadRecordActivity
import com.qreader.reader.ui.replace.ReplaceRuleActivity
import com.qreader.reader.ui.widget.dialog.TextDialog
import com.qreader.reader.utils.clearClip
import com.qreader.reader.utils.getClipText
import com.qreader.reader.utils.getPrefString
import com.qreader.reader.utils.observeEvent
import com.qreader.reader.utils.observeEventSticky
import com.qreader.reader.utils.putPrefBoolean
import com.qreader.reader.utils.putPrefString
import com.qreader.reader.utils.showDialogFragment
import com.qreader.reader.utils.showHelp
import com.qreader.reader.utils.startActivity
import com.qreader.reader.utils.startActivityForBook
import com.qreader.reader.utils.toastOnUi
import com.qreader.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import com.qreader.reader.utils.StringUtils

/**
 * 主界面
 *
 * 整页 Compose 迁移：Activity 保留 [VMBaseActivity] 继承（主题/系统栏/语言/返回键复用），
 * UI 层改为 [MainScreen]（Compose + HorizontalPager），三个页面均为 Compose 函数，
 * 底部导航栏为 LiquidBottomTabs 玻璃态胶囊栏。
 */
@Suppress("PrivatePropertyName")
class MainActivity : VMBaseActivity<ActivityMainBinding, MainViewModel>(),
    MainViewModel.CallBack {

    override val binding by viewBinding(ActivityMainBinding::inflate)
    override val viewModel by viewModels<MainViewModel>()
    private var exitTime: Long = 0
    private val EXIT_INTERVAL = 2000L

    // ── Compose 状态 ──
    private var selectedTab by mutableIntStateOf(0)
    private var showDiscovery by mutableStateOf(false)
    private var badgeCount by mutableIntStateOf(0)
    private var glassConfig by mutableStateOf(NavBarGlassConfig())

    // ── 设置页状态（原 MyFragment）──
    private var webServiceChecked by mutableStateOf(WebService.isRun)
    private var webServiceSummary by mutableStateOf("")
    private var themeModeIndex by mutableStateOf(0)

    // ── 书架返回键回调（由 BookshelfPage 注册）──
    private var bookshelfBack: (() -> Boolean)? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        // 延迟到 Activity attach 之后再读 SharedPreferences（字段初始化阶段 mBase 尚为 null 会 NPE）
        glassConfig = NavBarGlassConfig.load(this)
        themeModeIndex = getPrefString(PreferKey.themeMode, "0")?.toIntOrNull() ?: 0
        webServiceSummary = if (WebService.isRun) {
            WebService.hostAddress
        } else {
            getString(R.string.web_service_desc)
        }
        upBottomMenu()
        upHomePage()
        initView()
        onBackPressedDispatcher.addCallback(this) {
            if (selectedTab != 0) {
                selectedTab = 0
                return@addCallback
            }
            if (bookshelfBack?.invoke() == true) {
                return@addCallback
            }
            if (System.currentTimeMillis() - exitTime > EXIT_INTERVAL) {
                toastOnUi(R.string.double_click_exit)
                exitTime = System.currentTimeMillis()
            } else {
                if (BaseReadAloudService.pause) {
                    finish()
                } else {
                    moveTaskToBack(true)
                }
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        lifecycleScope.launch {
            //隐私协议
            if (!privacyPolicy()) return@launch
            //版本更新
            upVersion()
            notifyAppCrash()
            //备份同步
            backupSync()
            //设置回调
            viewModel.setActivityCallback(this@MainActivity)
            //自动更新书源
            binding.root.postDelayed(1000) {
                viewModel.ruleSubsUp()
            }
            readShibboleth(1500)
            //自动更新书籍
            val isAutoRefreshedBook = savedInstanceState?.getBoolean("isAutoRefreshedBook") ?: false
            if (AppConfig.autoRefreshBook && !isAutoRefreshedBook) {
                binding.root.postDelayed(2000) {
                    viewModel.upAllBookToc()
                }
            }
            binding.root.postDelayed(3000) {
                viewModel.postLoad()
            }
        }
    }

    private fun initView() {
        binding.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.composeView.setContent {
            var themeDialogOpen by remember { mutableStateOf(false) }
            MainScreen(
                selectedTabIndex = { selectedTab },
                onTabSelected = { position -> selectedTab = position },
                badgeCount = badgeCount,
                showDiscovery = showDiscovery,
                isEInkMode = AppConfig.isEInkMode,
                glassConfig = glassConfig,
                bookshelfPage = { registerGotoTop, registerBack, registerMenuAction, onRequestGroupEdit ->
                    BookshelfPageNew(
                        registerGotoTop = registerGotoTop,
                        registerBack = registerBack,
                        registerMenuAction = registerMenuAction,
                        onBookClick = { startActivityForBook(it) },
                        onBookLongClick = {
                            startActivity<BookInfoActivity> {
                                putExtra("name", it.name)
                                putExtra("author", it.author)
                            }
                        },
                        onGroupLongClick = onRequestGroupEdit,
                        onRefresh = { books, onlyUpdateRead ->
                            viewModel.upToc(books, onlyUpdateRead)
                        },
                        isUpdate = { viewModel.isUpdate(it) },
                    )
                },
                explorePage = { registerCompress, searchQuery, onSearchQueryChange, backdrop ->
                    ExplorePage(
                        registerCompress = registerCompress,
                        onOpenExplore = { sourceUrl, title, exploreUrl ->
                            if (exploreUrl.isNullOrBlank()) return@ExplorePage
                            startActivity<ExploreShowActivity> {
                                putExtra("exploreName", title)
                                putExtra("sourceUrl", sourceUrl)
                                putExtra("exploreUrl", exploreUrl)
                            }
                        },
                        onEditSource = {
                            startActivity<BookSourceEditActivity> {
                                putExtra("sourceUrl", it)
                            }
                        },
                        onSearchBook = { SearchActivity.start(this, it) },
                        searchQuery = searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        backdrop = backdrop,
                    )
                },
                settingsPage = {
                    SettingsPage(
                        webServiceChecked = webServiceChecked,
                        webServiceSummary = webServiceSummary,
                        themeModeIndex = themeModeIndex,
                        onActionClick = { handleSettingAction(it) },
                        onWebServiceToggle = { handleWebServiceToggle(it) },
                        onThemeModeSelected = { value -> handleThemeModeSelected(value) },
                        themeDialogOpen = themeDialogOpen,
                        onThemeDialogOpenChange = { themeDialogOpen = it },
                    )
                },
                themeDialogOpen = themeDialogOpen,
                onThemeDialogOpenChange = { themeDialogOpen = it },
                registerBookshelfBack = { bookshelfBack = it },
            )
        }
    }

    private fun upBottomMenu() {
        showDiscovery = AppConfig.showDiscovery
    }

    private fun upHomePage() {
        when (AppConfig.defaultHomePage) {
            "bookshelf" -> {}
            "explore" -> if (showDiscovery) {
                selectedTab = 1
            }
            "my" -> selectedTab = if (showDiscovery) 2 else 1
        }
    }

    // ── 设置页回调（原 MyFragment）──

    private fun handleSettingAction(key: String) {
        when (key) {
            "bookSourceManage" -> startActivity<BookSourceActivity>()
            "replaceManage" -> startActivity<ReplaceRuleActivity>()
            "dictRuleManage" -> startActivity<DictRuleActivity>()
            "txtTocRuleManage" -> startActivity<TxtTocRuleActivity>()
            "bookmark" -> startActivity<AllBookmarkActivity>()
            "setting" -> startActivity<ConfigActivity> {
                putExtra("configTag", ConfigTag.OTHER_CONFIG)
            }
            "web_dav_setting" -> startActivity<ConfigActivity> {
                putExtra("configTag", ConfigTag.BACKUP_CONFIG)
            }
            "theme_setting" -> startActivity<ConfigActivity> {
                putExtra("configTag", ConfigTag.THEME_CONFIG)
            }
            "fileManage" -> startActivity<FileManageActivity>()
            "readRecord" -> startActivity<ReadRecordActivity>()
            "glassDemo" -> startActivity<GlassDemoActivity>()
            "navBarGlass" -> startActivity<NavBarGlassSettingsActivity>()
            "appVersion" -> showAppVersion()
            "exit" -> finish()
        }
    }

    private fun handleWebServiceToggle(checked: Boolean) {
        putPrefBoolean(PreferKey.webService, checked)
        if (checked) {
            WebService.start(this)
        } else {
            WebService.stop(this)
        }
        webServiceChecked = WebService.isRun
        webServiceSummary = if (WebService.isRun) {
            WebService.hostAddress
        } else {
            getString(R.string.web_service_desc)
        }
    }

    private fun handleThemeModeSelected(value: Int) {
        themeModeIndex = value
        putPrefString(PreferKey.themeMode, value.toString())
        binding.root.post { ThemeConfig.applyDayNight(this) }
    }

    private fun showAppVersion() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            toastOnUi("当前版本: ${packageInfo.versionName}")
        } catch (e: Exception) {
            toastOnUi("获取版本信息失败")
        }
    }

    // ── 事件总线 ──

    override fun observeLiveBus() {
        viewModel.onUpBooksLiveData.observe(this) {
            badgeCount = it ?: 0
        }
        observeEvent<String>(EventBus.RECREATE) {
            recreate()
        }
        observeEvent<Boolean>(EventBus.NOTIFY_MAIN) {
            upBottomMenu()
            if (it) {
                selectedTab = if (showDiscovery) 2 else 1
            }
        }
        observeEvent<String>(PreferKey.threadCount) {
            viewModel.upPool()
        }
        observeEventSticky<String>(EventBus.WEB_SERVICE) {
            webServiceChecked = WebService.isRun
            webServiceSummary = if (WebService.isRun) {
                WebService.hostAddress
            } else {
                getString(R.string.web_service_desc)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 从设置页返回后刷新玻璃态配置，使底部导航栏即时生效
        glassConfig = NavBarGlassConfig.load(this)
        if (LifecycleHelp.activitySize() == 1) {
            readShibboleth(500)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (AppConfig.autoRefreshBook) {
            outState.putBoolean("isAutoRefreshedBook", true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Coroutine.async {
            BookHelp.clearInvalidCache()
        }
        if (!BuildConfig.DEBUG) {
            Backup.autoBack(this)
        }
    }

    override fun recreate() {
        super.recreate()
    }

    // ── 隐私协议 ──

    private suspend fun privacyPolicy(): Boolean = suspendCancellableCoroutine sc@{ block ->
        if (LocalConfig.privacyPolicyOk) {
            block.resume(true)
            return@sc
        }
        val privacyPolicy = String(assets.open("privacyPolicy.md").readBytes())
        alert(getString(R.string.privacy_policy), privacyPolicy) {
            positiveButton(R.string.agree) {
                LocalConfig.privacyPolicyOk = true
                block.resume(true)
            }
            negativeButton(R.string.refuse) {
                finish()
                block.resume(false)
            }
        }
    }

    // ── 版本更新日志 ──

    private suspend fun upVersion() = suspendCancellableCoroutine sc@{ block ->
        if (LocalConfig.versionCode == appInfo.versionCode) {
            block.resume(null)
            return@sc
        }
        LocalConfig.versionCode = appInfo.versionCode
        if (LocalConfig.isFirstOpenApp) {
            val help = String(assets.open("web/help/md/appHelp.md").readBytes())
            val dialog = TextDialog(getString(R.string.help), help, TextDialog.Mode.MD)
            dialog.setOnDismissListener {
                block.resume(null)
            }
            showDialogFragment(dialog)
        } else if (!BuildConfig.DEBUG) {
            val log = String(assets.open("updateLog.md").readBytes())
            val dialog = TextDialog(getString(R.string.update_log), log, TextDialog.Mode.MD)
            dialog.setOnDismissListener {
                block.resume(null)
            }
            showDialogFragment(dialog)
        } else {
            block.resume(null)
        }
    }

    private fun notifyAppCrash() {
        if (!LocalConfig.appCrash || BuildConfig.DEBUG) {
            return
        }
        LocalConfig.appCrash = false
        alert(getString(R.string.draw), "检测到阅读发生了崩溃，是否打开崩溃日志以便报告问题？") {
            yesButton {
                showDialogFragment<CrashLogsDialog>()
            }
            noButton()
        }
    }

    // ── 备份同步 ──

    private fun backupSync() {
        if (!AppConfig.autoCheckNewBackup) {
            return
        }
        lifecycleScope.launch {
            val lastBackupFile =
                withContext(IO) { AppWebDav.lastBackUp().getOrNull() } ?: return@launch
            if (lastBackupFile.lastModify - LocalConfig.lastBackup > DateUtils.MINUTE_IN_MILLIS) {
                LocalConfig.lastBackup = lastBackupFile.lastModify
                alert(R.string.restore, R.string.webdav_after_local_restore_confirm) {
                    cancelButton()
                    okButton {
                        viewModel.restoreWebDav(lastBackupFile.displayName)
                    }
                }
            }
        }
    }

    // ── 导入 ──

    override fun openImportUi(type: Int, source: String) {
        when (type) {
            0 -> showDialogFragment(ImportBookSourceDialog(source))
            1 -> showDialogFragment(ImportRssSourceDialog(source))
            2 -> showDialogFragment(ImportReplaceRuleDialog(source))
        }
    }

    // ── 读取导入口令 ──

    fun readShibboleth(delay: Long) {
        binding.root.postDelayed(delay) {
            val text = this@MainActivity.getClipText()
            if (!text.isNullOrBlank()) {
                if ("#L:" in text) {
                    this@MainActivity.clearClip() //清理一下防重复
                    val (url, type, customWord) = StringUtils.unShibboleth(text)
                    when (type) {
                        StringUtils.BOOK_SOURCE ->
                            showDialogFragment(ImportBookSourceDialog(url))
                        StringUtils.RSS_SOURCE ->
                            showDialogFragment(ImportRssSourceDialog(url))
                        StringUtils.DICT_RULE ->
                            showDialogFragment(ImportDictRuleDialog(url))
                        StringUtils.REPLACE_RULE ->
                            showDialogFragment(ImportReplaceRuleDialog(url))
                        StringUtils.TOC_RULE ->
                            showDialogFragment(ImportTxtTocRuleDialog(url))
                        StringUtils.TTS_RULE ->
                            showDialogFragment(ImportHttpTtsDialog(url))
                        else -> showDialogFragment(ImportHttpTtsDialog(url))
                    }
                }
            }
        }
    }
}
