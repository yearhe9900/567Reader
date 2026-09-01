package com.qreader.reader.ui.welcome

import android.content.Intent
import android.os.Bundle
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.postDelayed
import com.qreader.reader.base.BaseActivity
import com.qreader.reader.constant.PreferKey
import com.qreader.reader.constant.Theme
import com.qreader.reader.data.appDb
import com.qreader.reader.databinding.ActivityWelcomeBinding
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.help.config.ThemeConfig
import com.qreader.reader.lib.theme.accentColor
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
import com.qreader.reader.utils.visible
import com.qreader.reader.utils.windowSize

open class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>() {

    override val binding by viewBinding(ActivityWelcomeBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            // 避免从桌面启动程序后，会重新实例化入口类的activity
            finish()
        } else {
            val welcomeShowTime = getPrefInt(PreferKey.welcomeShowTime, 500)
            if (welcomeShowTime == 0) {
                startMainActivity()
            } else {
                binding.root.postDelayed(welcomeShowTime.toLong()) { startMainActivity() }
            }
        }
        binding.ivBook.setColorFilter(accentColor)
        binding.vwTitleLine.setBackgroundColor(accentColor)
        applyCustomWelcomeBackground()
    }

    override fun setupSystemBar() {
        fullScreen()
        setStatusBarColorAuto(backgroundColor, true, fullScreen)
        upNavigationBarColor()
    }

    private fun applyCustomWelcomeBackground() {
        if (getPrefBoolean(PreferKey.customWelcome)) {
            kotlin.runCatching {
                when (ThemeConfig.getTheme()) {
                    Theme.Dark -> {
                        getPrefString(PreferKey.welcomeImageDark)?.let { path ->
                            if (path.endsWith(".9.png")) {
                                BitmapUtils.decodeNinePatchDrawable(path)?.let {
                                    window.decorView.background = it
                                }
                            } else {
                                val size = windowManager.windowSize
                                BitmapUtils.decodeBitmap(path, size.widthPixels, size.heightPixels)?.let {
                                    window.decorView.background = it.toDrawable(resources)
                                }
                            }
                        }
                        binding.tvLegado.visible(AppConfig.welcomeShowTextDark)
                        binding.ivBook.visible(AppConfig.welcomeShowIconDark)
                        binding.tvGzh.visible(AppConfig.welcomeShowTextDark)
                        return
                    }
                    else -> {
                        getPrefString(PreferKey.welcomeImage)?.let { path ->
                            if (path.endsWith(".9.png")) {
                                BitmapUtils.decodeNinePatchDrawable(path)?.let {
                                    window.decorView.background = it
                                }
                            } else {
                                val size = windowManager.windowSize
                                BitmapUtils.decodeBitmap(path, size.widthPixels, size.heightPixels)?.let {
                                    window.decorView.background = it.toDrawable(resources)
                                }
                            }
                        }
                        binding.tvLegado.visible(AppConfig.welcomeShowText)
                        binding.ivBook.visible(AppConfig.welcomeShowIcon)
                        binding.tvGzh.visible(AppConfig.welcomeShowText)
                        return
                    }
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