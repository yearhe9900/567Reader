package com.qreader.reader.ui.config

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.qreader.reader.R
import com.qreader.reader.base.VMBaseActivity
import com.qreader.reader.constant.EventBus
import com.qreader.reader.databinding.ActivityConfigBinding
import com.qreader.reader.ui.compose.glass.GlassTopBarReservedHeight
import com.qreader.reader.utils.observeEvent
import com.qreader.reader.utils.viewbindingdelegate.viewBinding

class ConfigActivity : VMBaseActivity<ActivityConfigBinding, ConfigViewModel>() {

    override val binding by viewBinding(ActivityConfigBinding::inflate)
    override val viewModel by viewModels<ConfigViewModel>()

    /**
     * Fragment 挂载点。Compose 化后不再由 XML 提供，改由本 Activity 创建并交给
     * [ConfigScreen] 通过 AndroidView 承载（fragment-compose 未引入，见 ConfigScreen 注释）。
     */
    private lateinit var configContainer: FrameLayout

    /**
     * 顶栏标题。子 Fragment 通过 `activity?.setTitle(R.string.xxx)` 驱动，
     * 而 Compose 需要可观察状态才能重组，故用该字段桥接（见 [setTitle]）。
     */
    private var uiTitle by mutableStateOf("")

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        configContainer = FrameLayout(this).apply {
            id = R.id.configFrameLayout
            // 为悬浮玻璃顶栏留出空间 + 状态栏高度，否则内容被顶栏盖住。
            setPadding(0, topBarReservedPx(), 0, 0)
            clipToPadding = false
        }

        binding.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.composeView.setContent {
            ConfigScreen(
                title = uiTitle,
                fragmentHost = configContainer,
                onBack = { finish() },
            )
        }

        // Fragment 事务必须等容器真正挂进视图树之后再提交。
        // setContent 只是登记组合内容，此刻 ComposeView 尚未 attach、AndroidView 也还没
        // 把 configContainer 加进层级；此时 commit 会因找不到容器 id 而失败
        // （FragmentManager 抛 "No view found for id"）。
        // 用 post 排到下一个消息循环，此时首帧已布局完成，容器一定已 attach。
        binding.composeView.post { showConfigFragment() }
    }

    private fun showConfigFragment() {
        when (val configTag = intent.getStringExtra("configTag")) {
            ConfigTag.OTHER_CONFIG -> replaceFragment<OtherConfigFragment>(configTag)
            ConfigTag.THEME_CONFIG -> replaceFragment<ThemeConfigFragment>(configTag)
            ConfigTag.BACKUP_CONFIG -> replaceFragment<BackupConfigFragment>(configTag)
            ConfigTag.COVER_CONFIG -> replaceFragment<CoverConfigFragment>(configTag)
            ConfigTag.WELCOME_CONFIG -> replaceFragment<WelcomeConfigFragment>(configTag)
            else -> finish()
        }
    }

    /**
     * 顶栏预留高度（像素）：纯标题栏高度 + 状态栏高度。
     *
     * Fragment 内容走的是 View 体系（Preference 列表），不吃 Compose 的 `statusBarsPadding`，
     * 所以这里要把两部分都换算成 padding 手动加回去。
     */
    private fun topBarReservedPx(): Int {
        val statusBarHeight = resources.getDimensionPixelSize(
            resources.getIdentifier("status_bar_height", "dimen", "android")
        )
        val titleBarHeight = (GlassTopBarReservedHeight.value * resources.displayMetrics.density).toInt()
        return titleBarHeight + statusBarHeight
    }

    /**
     * 子 Fragment 用 `activity?.setTitle(R.string.xxx)` 设置标题（全部 5 个 Fragment 都只走这个重载）。
     *
     * 这里转发给 Compose 状态（[uiTitle]）驱动玻璃顶栏，同时**不再**调用
     * `binding.titleBar.setTitle(...)`——View 版 TitleBar 已随布局一起移除。
     *
     * 注意：**不要**顺手再重写 `setTitle(CharSequence)`。AppCompat 在 Activity
     * 初始化时会用清单里的 `android:label` 走那个重载调一次，若一并接管，
     * 会把 Fragment 刚设好的标题覆盖成「设置」这类默认值。
     */
    override fun setTitle(resId: Int) {
        super.setTitle(resId)
        uiTitle = getString(resId)
    }

    inline fun <reified T : Fragment> replaceFragment(configTag: String) {
        intent.putExtra("configTag", configTag)
        @Suppress("DEPRECATION")
        val configFragment = supportFragmentManager.findFragmentByTag(configTag)
            ?: T::class.java.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.configFrameLayout, configFragment, configTag)
            .commit()
    }

    override fun observeLiveBus() {
        super.observeLiveBus()
        observeEvent<String>(EventBus.RECREATE) {
            recreate()
        }
    }

}
