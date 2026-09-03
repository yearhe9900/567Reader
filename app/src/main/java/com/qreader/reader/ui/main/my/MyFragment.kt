package com.qreader.reader.ui.main.my

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.qreader.reader.R
import com.qreader.reader.base.BaseFragment
import com.qreader.reader.constant.EventBus
import com.qreader.reader.constant.PreferKey
import com.qreader.reader.databinding.FragmentMyConfigBinding
import com.qreader.reader.help.config.ThemeConfig
import com.qreader.reader.service.WebService
import com.qreader.reader.ui.about.ReadRecordActivity
import com.qreader.reader.ui.book.bookmark.AllBookmarkActivity
import com.qreader.reader.ui.book.source.manage.BookSourceActivity
import com.qreader.reader.ui.book.toc.rule.TxtTocRuleActivity
import com.qreader.reader.ui.compose.GlassDemoActivity
import com.qreader.reader.ui.config.ConfigActivity
import com.qreader.reader.ui.config.ConfigTag
import com.qreader.reader.ui.dict.rule.DictRuleActivity
import com.qreader.reader.ui.file.FileManageActivity
import com.qreader.reader.ui.main.MainFragmentInterface
import com.qreader.reader.ui.replace.ReplaceRuleActivity
import com.qreader.reader.utils.getPrefString
import com.qreader.reader.utils.observeEventSticky
import com.qreader.reader.utils.putPrefBoolean
import com.qreader.reader.utils.putPrefString
import com.qreader.reader.utils.showHelp
import com.qreader.reader.utils.startActivity
import com.qreader.reader.utils.toastOnUi
import com.qreader.reader.utils.viewbindingdelegate.viewBinding

class MyFragment() : BaseFragment(R.layout.fragment_my_config), MainFragmentInterface {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    override val position: Int? get() = arguments?.getInt("position")

    private val binding by viewBinding(FragmentMyConfigBinding::bind)

    private var webServiceChecked by mutableStateOf(WebService.isRun)
    private var webServiceSummary by mutableStateOf("")
    private var themeModeIndex by mutableStateOf(0)
    private var composeInitialized = false

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)

        val ctx = requireContext()
        themeModeIndex = ctx.getPrefString(PreferKey.themeMode, "0")?.toIntOrNull() ?: 0
        webServiceSummary = if (WebService.isRun) {
            WebService.hostAddress
        } else {
            getString(R.string.web_service_desc)
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

    /**
     * 懒加载 Compose：仅在 Fragment 真正可见（resume）时才 setContent。
     * 原因：MainActivity 的 ViewPager 使用 offscreenPageLimit=3 会预加载本页，
     * 若在离屏阶段就渲染 Liquid Glass 的 backdrop（GPU/RenderEffect），
     * 会在启动早期触发 Surface 渲染异常导致黑屏。
     */
    override fun onResume() {
        super.onResume()
        if (composeInitialized) return
        composeInitialized = true
        binding.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.composeView.setContent {
            MySettingsScreen(
                webServiceChecked = webServiceChecked,
                webServiceSummary = webServiceSummary,
                themeModeIndex = themeModeIndex,
                onActionClick = { key -> handleSettingAction(key) },
                onWebServiceToggle = { checked -> handleWebServiceToggle(checked) },
                onThemeModeSelected = { value -> handleThemeModeSelected(value) },
                themeDialogOpen = false,
                onThemeDialogOpenChange = {}
            )
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_my, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_help -> showHelp("appHelp")
        }
    }

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
            "appVersion" -> showAppVersion()
            "exit" -> activity?.finish()
        }
    }

    private fun handleWebServiceToggle(checked: Boolean) {
        val ctx = requireContext()
        ctx.putPrefBoolean(PreferKey.webService, checked)
        if (checked) {
            WebService.start(ctx)
        } else {
            WebService.stop(ctx)
        }
        webServiceChecked = WebService.isRun
        webServiceSummary = if (WebService.isRun) {
            WebService.hostAddress
        } else {
            getString(R.string.web_service_desc)
        }
    }

    private fun handleThemeModeSelected(value: Int) {
        val ctx = requireContext()
        themeModeIndex = value
        ctx.putPrefString(PreferKey.themeMode, value.toString())
        view?.post { ThemeConfig.applyDayNight(ctx) }
    }

    private fun showAppVersion() {
        try {
            val ctx = requireContext()
            val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            toastOnUi("当前版本: ${packageInfo.versionName}")
        } catch (e: Exception) {
            toastOnUi("获取版本信息失败")
        }
    }
}
