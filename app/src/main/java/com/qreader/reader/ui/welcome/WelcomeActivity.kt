package com.qreader.reader.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.qreader.reader.base.BaseActivity
import com.qreader.reader.constant.PreferKey
import com.qreader.reader.constant.Theme
import com.qreader.reader.data.appDb
import com.qreader.reader.databinding.ActivityWelcomeBinding
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.help.config.ThemeConfig
import com.qreader.reader.lib.theme.backgroundColor
import com.qreader.reader.ui.book.read.ReadBookActivity
import com.qreader.reader.ui.main.MainActivity
import com.qreader.reader.utils.BitmapUtils
import com.qreader.reader.utils.fullScreen
import com.qreader.reader.utils.getPrefBoolean
import com.qreader.reader.utils.getPrefInt
import com.qreader.reader.utils.getPrefString
import com.qreader.reader.utils.setStatusBarColorAuto
import com.qreader.reader.utils.startActivity
import com.qreader.reader.utils.viewbindingdelegate.viewBinding

open class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>() {

    override val binding by viewBinding(ActivityWelcomeBinding::inflate)

    private var uiShowText by mutableStateOf(true)
    private var uiShowIcon by mutableStateOf(true)
    private var uiBackgroundPath by mutableStateOf<String?>(null)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            // 避免从桌面启动程序后，会重新实例化入口类的 activity
            finish()
        } else {
            val welcomeShowTime = getPrefInt(PreferKey.welcomeShowTime, 500)
            if (welcomeShowTime <= 0) {
                startMainActivity()
            } else {
                // 用主线程 Handler，不依赖 View.post：Compose 异常时仍能跳出欢迎页
                Handler(Looper.getMainLooper()).postDelayed(
                    { startMainActivity() },
                    welcomeShowTime.toLong(),
                )
            }
        }
        applyCustomWelcomeBackground()
        runCatching {
            binding.composeView.setContent {
                WelcomeScreen(
                    showText = uiShowText,
                    showIcon = uiShowIcon,
                    backgroundPath = uiBackgroundPath,
                )
            }
        }
    }

    override fun setupSystemBar() {
        fullScreen()
        setStatusBarColorAuto(backgroundColor, true, fullScreen)
        upNavigationBarColor()
    }

    private fun applyCustomWelcomeBackground() {
        if (!getPrefBoolean(PreferKey.customWelcome)) return
        kotlin.runCatching {
            when (ThemeConfig.getTheme()) {
                Theme.Dark -> {
                    val path = getPrefString(PreferKey.welcomeImageDark)
                    // 九宫格仍走窗口背景（Compose 难以精确拉伸 .9）
                    if (path != null && path.endsWith(".9.png")) {
                        BitmapUtils.decodeNinePatchDrawable(path)?.let {
                            window.decorView.background = it
                        }
                    } else {
                        uiBackgroundPath = path
                    }
                    uiShowText = AppConfig.welcomeShowTextDark
                    uiShowIcon = AppConfig.welcomeShowIconDark
                }
                else -> {
                    val path = getPrefString(PreferKey.welcomeImage)
                    if (path != null && path.endsWith(".9.png")) {
                        BitmapUtils.decodeNinePatchDrawable(path)?.let {
                            window.decorView.background = it
                        }
                    } else {
                        uiBackgroundPath = path
                    }
                    uiShowText = AppConfig.welcomeShowText
                    uiShowIcon = AppConfig.welcomeShowIcon
                }
            }
        }
    }

    private fun startMainActivity() {
        startActivity<MainActivity>()
        if (getPrefBoolean(PreferKey.defaultToRead) && appDb.bookDao.lastReadBook != null) {
            startActivity<ReadBookActivity>()
        }
        finish()
    }

}

class Launcher1 : WelcomeActivity()
class Launcher2 : WelcomeActivity()
class Launcher3 : WelcomeActivity()
class Launcher4 : WelcomeActivity()
class Launcher5 : WelcomeActivity()
class Launcher6 : WelcomeActivity()
class Launcher7 : WelcomeActivity()
