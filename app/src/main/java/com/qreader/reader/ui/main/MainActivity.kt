@file:Suppress("DEPRECATION")

package com.qreader.reader.ui.main

import android.os.Bundle
import android.text.format.DateUtils
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
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
import com.qreader.reader.help.coroutine.Coroutine
import com.qreader.reader.help.storage.Backup
import com.qreader.reader.lib.dialogs.alert
import com.qreader.reader.lib.theme.primaryColor
import com.qreader.reader.service.BaseReadAloudService
import com.qreader.reader.ui.about.CrashLogsDialog
import com.qreader.reader.ui.association.ImportBookSourceDialog
import com.qreader.reader.ui.association.ImportReplaceRuleDialog
import com.qreader.reader.ui.association.ImportRssSourceDialog
import com.qreader.reader.ui.main.bookshelf.BaseBookshelfFragment
import com.qreader.reader.ui.main.bookshelf.style1.BookshelfFragment1
import com.qreader.reader.ui.main.bookshelf.style2.BookshelfFragment2
import com.qreader.reader.ui.main.explore.ExploreFragment
import com.qreader.reader.ui.main.my.MyFragment
import com.qreader.reader.ui.widget.dialog.TextDialog
import com.qreader.reader.utils.isCreated
import com.qreader.reader.utils.observeEvent
import com.qreader.reader.utils.setEdgeEffectColor
import com.qreader.reader.utils.showDialogFragment
import com.qreader.reader.utils.toastOnUi
import com.qreader.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import com.qreader.reader.ui.association.ImportDictRuleDialog
import com.qreader.reader.ui.association.ImportHttpTtsDialog
import com.qreader.reader.ui.association.ImportTxtTocRuleDialog
import com.qreader.reader.ui.compose.liquid.NavBarGlassConfig
import com.qreader.reader.utils.StringUtils
import com.qreader.reader.utils.clearClip
import com.qreader.reader.utils.getClipText

/**
 * 主界面
 *
 * 整页迁移 Compose：Activity 保留 [VMBaseActivity] 继承（主题/系统栏/语言/返回键复用），
 * UI 层改为 [MainScreen]（Compose），内容区 ViewPager + 三个 Fragment 暂保留 View 体系，
 * 底部导航栏替换为 LiquidBottomTabs 玻璃态胶囊栏。
 */
@Suppress("PrivatePropertyName")
class MainActivity : VMBaseActivity<ActivityMainBinding, MainViewModel>(),
    MainViewModel.CallBack {

    override val binding by viewBinding(ActivityMainBinding::inflate)
    override val viewModel by viewModels<MainViewModel>()
    private val idBookshelf = 0
    private val idBookshelf1 = 11
    private val idBookshelf2 = 12
    private val idExplore = 1
    private val idMy = 2
    private var exitTime: Long = 0
    private var bookshelfReselected: Long = 0
    private var exploreReselected: Long = 0
    private var pagePosition = 0
    private val fragmentMap = hashMapOf<Int, Fragment>()
    private var bottomMenuCount = 3
    private val EXIT_INTERVAL = 2000L
    private val realPositions = arrayOf(idBookshelf, idExplore, idMy)
    private val adapter by lazy {
        TabFragmentPageAdapter(supportFragmentManager)
    }

    // Compose 状态（由 MainScreen 消费，ViewPager 联动驱动）
    private var selectedTab by mutableIntStateOf(0)
    private var showDiscovery by mutableStateOf(false)
    private var badgeCount by mutableIntStateOf(0)
    private var glassConfig by mutableStateOf(NavBarGlassConfig())
    private lateinit var viewPager: ViewPager

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        // 延迟到 Activity attach 之后再读 SharedPreferences（字段初始化阶段 mBase 尚为 null 会 NPE）
        glassConfig = NavBarGlassConfig.load(this)
        upBottomMenu()
        initView()
        upHomePage()
        onBackPressedDispatcher.addCallback(this) {
            if (pagePosition != 0) {
                viewPager.currentItem = 0
                return@addCallback
            }
            (fragmentMap[getFragmentId(0)] as? BookshelfFragment2)?.let {
                if (it.back()) {
                    return@addCallback
                }
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
            viewPager.postDelayed(1000) {
                viewModel.ruleSubsUp()
            }
            readShibboleth(1500)
            //自动更新书籍
            val isAutoRefreshedBook = savedInstanceState?.getBoolean("isAutoRefreshedBook") ?: false
            if (AppConfig.autoRefreshBook && !isAutoRefreshedBook) {
                viewPager.postDelayed(2000) {
                    viewModel.upAllBookToc()
                }
            }
            viewPager.postDelayed(3000) {
                viewModel.postLoad()
            }
        }
    }

    private fun initView() {
        viewPager = ViewPager(this).apply {
            id = R.id.view_pager_main
            setEdgeEffectColor(primaryColor)
            offscreenPageLimit = 3
            adapter = this@MainActivity.adapter
            addOnPageChangeListener(PageChangeCallback())
        }
        binding.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.composeView.setContent {
            MainScreen(
                viewPager = viewPager,
                selectedTabIndex = { selectedTab },
                onTabSelected = { position -> selectTab(position) },
                onTabReselected = { position -> reselectTab(position) },
                badgeCount = badgeCount,
                showDiscovery = showDiscovery,
                isEInkMode = AppConfig.isEInkMode,
                glassConfig = glassConfig,
            )
        }
    }

    /**
     * 切换 tab（对应原 onNavigationItemSelected）
     */
    private fun selectTab(position: Int) {
        viewPager.setCurrentItem(position, false)
    }

    /**
     * 重选当前 tab（对应原 onNavigationItemReselected）：书架回顶 / 发现压缩
     */
    private fun reselectTab(position: Int) {
        when (getFragmentId(position)) {
            idBookshelf1, idBookshelf2 -> {
                if (System.currentTimeMillis() - bookshelfReselected > 300) {
                    bookshelfReselected = System.currentTimeMillis()
                } else {
                    (fragmentMap[getFragmentId(0)] as? BaseBookshelfFragment)?.gotoTop()
                }
            }

            idExplore -> {
                if (System.currentTimeMillis() - exploreReselected > 300) {
                    exploreReselected = System.currentTimeMillis()
                } else {
                    (fragmentMap[idExplore] as? ExploreFragment)?.compressExplore()
                }
            }
        }
    }

    /**
     * 用户隐私与协议
     */
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

    /**
     * 版本更新日志
     */
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

    /**
     * 备份同步
     */
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

    /**
     * 如果重启太快fragment不会重建,这里更新一下书架的排序
     */
    override fun recreate() {
        (fragmentMap[getFragmentId(0)] as? BaseBookshelfFragment)?.run {
            upSort()
        }
        super.recreate()
    }

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
                viewPager.setCurrentItem(bottomMenuCount - 1, false)
            }
        }
        observeEvent<String>(PreferKey.threadCount) {
            viewModel.upPool()
        }
    }

    private fun upBottomMenu() {
        showDiscovery = AppConfig.showDiscovery
        var index = 0
        if (showDiscovery) {
            index++
            realPositions[index] = idExplore
        }
        index++
        realPositions[index] = idMy
        bottomMenuCount = index + 1
        adapter.notifyDataSetChanged()
    }

    private fun upHomePage() {
        when (AppConfig.defaultHomePage) {
            "bookshelf" -> {}
            "explore" -> if (AppConfig.showDiscovery) {
                viewPager.setCurrentItem(realPositions.indexOf(idExplore), false)
            }

            "my" -> viewPager.setCurrentItem(realPositions.indexOf(idMy), false)
        }
    }

    private fun getFragmentId(position: Int): Int {
        val id = realPositions[position]
        if (id == idBookshelf) {
            return if (AppConfig.bookGroupStyle == 1) idBookshelf2 else idBookshelf1
        }
        return id
    }

    private inner class PageChangeCallback : ViewPager.SimpleOnPageChangeListener() {

        override fun onPageSelected(position: Int) {
            pagePosition = position
            selectedTab = position
        }

    }

    @Suppress("DEPRECATION")
    private inner class TabFragmentPageAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        private fun getId(position: Int): Int {
            return getFragmentId(position)
        }

        override fun getItemPosition(any: Any): Int {
            val position = (any as MainFragmentInterface).position
                ?: return POSITION_NONE
            val fragmentId = getId(position)
            if ((fragmentId == idBookshelf1 && any is BookshelfFragment1)
                || (fragmentId == idBookshelf2 && any is BookshelfFragment2)
                || (fragmentId == idExplore && any is ExploreFragment)
                || (fragmentId == idMy && any is MyFragment)
            ) {
                return POSITION_UNCHANGED
            }
            return POSITION_NONE
        }

        override fun getItem(position: Int): Fragment {
            return when (getId(position)) {
                idBookshelf1 -> BookshelfFragment1(position)
                idBookshelf2 -> BookshelfFragment2(position)
                idExplore -> ExploreFragment(position)
                else -> MyFragment(position)
            }
        }

        override fun getCount(): Int {
            return bottomMenuCount
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            var fragment = super.instantiateItem(container, position) as Fragment
            if (fragment.isCreated && getItemPosition(fragment) == POSITION_NONE) {
                destroyItem(container, position, fragment)
                fragment = super.instantiateItem(container, position) as Fragment
            }
            fragmentMap[getId(position)] = fragment
            return fragment
        }

    }

    override fun openImportUi(type:Int, source: String) {
        when (type) {
            0 -> showDialogFragment(
                ImportBookSourceDialog(source)
            )
            1 -> showDialogFragment(
                ImportRssSourceDialog(source)
            )
            2 -> showDialogFragment(
                ImportReplaceRuleDialog(source)
            )
        }
    }

    /**
     * 读取导入口令
     */
    fun readShibboleth(delay: Long) {
        viewPager.postDelayed(delay) {
            val text = this@MainActivity.getClipText()
            if (!text.isNullOrBlank()) {
                if ("#L:" in text) {
                    this@MainActivity.clearClip() //清理一下防重复
                    val (url, type, customWord) = StringUtils.unShibboleth(text)
                    when(type) {
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

    override fun onResume() {
        super.onResume()
        // 从设置页返回后刷新玻璃态配置，使底部导航栏即时生效
        glassConfig = NavBarGlassConfig.load(this)
        if (LifecycleHelp.activitySize() == 1) {
            readShibboleth(500)
        }
    }

}
