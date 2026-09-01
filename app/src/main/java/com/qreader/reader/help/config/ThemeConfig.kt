package com.qreader.reader.help.config

import android.content.Context
import android.graphics.Color
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.toColorInt
import com.qreader.reader.R
import com.qreader.reader.constant.AppLog
import com.qreader.reader.constant.EventBus
import com.qreader.reader.constant.PreferKey
import com.qreader.reader.constant.Theme
import com.qreader.reader.help.DefaultData
import com.qreader.reader.lib.theme.ThemeStore
import com.qreader.reader.model.BookCover
import com.qreader.reader.utils.FileUtils
import com.qreader.reader.utils.GSON
import com.qreader.reader.utils.fromJsonArray
import com.qreader.reader.utils.fromJsonObject
import com.qreader.reader.utils.getCompatColor
import com.qreader.reader.utils.getPrefString
import com.qreader.reader.utils.hexString
import com.qreader.reader.utils.postEvent
import com.qreader.reader.utils.printOnDebug
import splitties.init.appCtx
import java.io.File

@Keep
object ThemeConfig {
    const val configFileName = "themeConfig.json"
    val configFilePath = FileUtils.getPath(appCtx.filesDir, configFileName)

    val configList: ArrayList<Config> by lazy {
        val cList = getConfigs() ?: DefaultData.themeConfigs
        ArrayList(cList)
    }

    fun getTheme() = when {
        AppConfig.isEInkMode -> Theme.EInk
        AppConfig.isNightTheme -> Theme.Dark
        else -> Theme.Light
    }

    fun isDarkTheme(): Boolean {
        return getTheme() == Theme.Dark
    }

    fun applyDayNight(context: Context) {
        applyTheme(context)
        initNightMode()
        BookCover.upDefaultCover()
        postEvent(EventBus.RECREATE, "")
    }

    fun applyDayNightInit(context: Context) {
        applyTheme(context)
        initNightMode()
    }

    private fun initNightMode() {
        val targetMode =
            if (AppConfig.isNightTheme) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        AppCompatDelegate.setDefaultNightMode(targetMode)
    }

    fun upConfig() {
        addConfigs(getConfigs())
    }

    fun save() {
        val json = GSON.toJson(configList)
        FileUtils.delete(configFilePath)
        FileUtils.createFileIfNotExist(configFilePath).writeText(json)
    }

    fun delConfig(index: Int) {
        configList.removeAt(index)
        save()
    }

    fun addConfig(json: String): Boolean {
        GSON.fromJsonObject<Config>(json.trim { it < ' ' }).getOrNull()
            ?.let {
                if (validateConfig(it)) {
                    addConfig(it)
                    return true
                }
            }
        return false
    }

    fun addConfig(newConfig: Config) {
        if (!validateConfig(newConfig)) {
            return
        }
        var hasTheme = false
        configList.forEachIndexed { index, config ->
            if (newConfig.themeName == config.themeName) {
                configList[index] = newConfig
                hasTheme = true
                return@forEachIndexed
            }
        }
        if (!hasTheme) {
            configList.add(newConfig)
        }
        save()
    }

    fun addConfigs(newConfigs: List<Config>?) {
        val newConfigs = newConfigs?.filter{
            validateConfig(it)
        }
        if (newConfigs.isNullOrEmpty()) {
            return
        }
        newConfigs.forEach { newConfig ->
            val existingIndex = configList.indexOfFirst { it.themeName == newConfig.themeName }
            if (existingIndex != -1) {
                configList[existingIndex] = newConfig
            } else {
                configList.add(newConfig)
            }
        }
        save()
    }

    private fun validateConfig(config: Config): Boolean {
        try {
            config.primaryColor.toColorInt()
            config.accentColor.toColorInt()
            config.backgroundColor.toColorInt()
            config.bottomBackground.toColorInt()
            return true
        } catch (_: Exception) {
            return false
        }
    }

    private fun getConfigs(): List<Config>? {
        val configFile = File(configFilePath)
        if (configFile.exists()) {
            kotlin.runCatching {
                val json = configFile.readText()
                return GSON.fromJsonArray<Config>(json).getOrThrow()
            }.onFailure {
                it.printOnDebug()
            }
        }
        return null
    }

    fun applyConfig(context: Context, config: Config) {
        try {
            AppConfig.isNightTheme = config.isNightTheme
            applyDayNight(context)
        } catch (e: Exception) {
            AppLog.put("设置主题出错\n$e", e, true)
        }
    }

    fun getDurConfig(context: Context): Config {
        val isNight = AppConfig.isNightTheme
        val name = if (isNight) {
            context.getPrefString(PreferKey.dNThemeName) ?: ""
        } else {
            context.getPrefString(PreferKey.dThemeName) ?: ""
        }
        return if (isNight) {
            getNightTheme(name)
        } else {
            getDayTheme(name)
        }
    }

    private fun getDayTheme(name: String): Config {
        return Config(
            themeName = name,
            isNightTheme = false,
            primaryColor = "#${appCtx.getCompatColor(R.color.md_brown_500).hexString}",
            accentColor = "#${appCtx.getCompatColor(R.color.md_red_600).hexString}",
            backgroundColor = "#${appCtx.getCompatColor(R.color.md_grey_100).hexString}",
            bottomBackground = "#${appCtx.getCompatColor(R.color.md_grey_200).hexString}",
            transparentNavBar = false,
            backgroundImgPath = null,
            backgroundImgBlur = 0
        )
    }

    fun saveDayTheme(context: Context, name: String) {
        val config = getDayTheme(name)
        addConfig(config)
    }

    private fun getNightTheme(name: String): Config {
        return Config(
            themeName = name,
            isNightTheme = true,
            primaryColor = "#${appCtx.getCompatColor(R.color.md_blue_grey_600).hexString}",
            accentColor = "#${appCtx.getCompatColor(R.color.md_deep_orange_800).hexString}",
            backgroundColor = "#${appCtx.getCompatColor(R.color.md_grey_900).hexString}",
            bottomBackground = "#${appCtx.getCompatColor(R.color.md_grey_850).hexString}",
            transparentNavBar = false,
            backgroundImgPath = null,
            backgroundImgBlur = 0
        )
    }

    fun saveNightTheme(context: Context, name: String) {
        val config = getNightTheme(name)
        addConfig(config)
    }

    /**
     * 更新主题 — 固定使用默认配色
     */
    fun applyTheme(context: Context) {
        when {
            AppConfig.isEInkMode -> {
                ThemeStore.editTheme(context)
                    .primaryColor(Color.WHITE)
                    .accentColor(Color.BLACK)
                    .backgroundColor(Color.WHITE)
                    .bottomBackground(Color.WHITE)
                    .transparentNavBar(false)
                    .apply()
            }

            AppConfig.isNightTheme -> {
                ThemeStore.editTheme(context)
                    .primaryColor(context.getCompatColor(R.color.md_blue_grey_600))
                    .accentColor(context.getCompatColor(R.color.md_deep_orange_800))
                    .backgroundColor(context.getCompatColor(R.color.md_grey_900))
                    .bottomBackground(context.getCompatColor(R.color.md_grey_850))
                    .transparentNavBar(false)
                    .apply()
            }

            else -> {
                ThemeStore.editTheme(context)
                    .primaryColor(context.getCompatColor(R.color.md_brown_500))
                    .accentColor(context.getCompatColor(R.color.md_red_600))
                    .backgroundColor(context.getCompatColor(R.color.md_grey_100))
                    .bottomBackground(context.getCompatColor(R.color.md_grey_200))
                    .transparentNavBar(false)
                    .apply()
            }
        }
    }

    @Keep
    data class Config(
        var themeName: String,
        var isNightTheme: Boolean,
        var primaryColor: String,
        var accentColor: String,
        var backgroundColor: String,
        var bottomBackground: String,
        var transparentNavBar: Boolean,
        var backgroundImgPath: String?,
        var backgroundImgBlur: Int
    ) {

        override fun hashCode(): Int {
            return GSON.toJson(this).hashCode()
        }

        override fun equals(other: Any?): Boolean {
            other ?: return false
            if (other is Config) {
                return other.themeName == themeName
                        && other.isNightTheme == isNightTheme
                        && other.primaryColor == primaryColor
                        && other.accentColor == accentColor
                        && other.backgroundColor == backgroundColor
                        && other.bottomBackground == bottomBackground
                        && other.transparentNavBar == transparentNavBar
                        && other.backgroundImgPath == backgroundImgPath
                        && other.backgroundImgBlur == backgroundImgBlur
            }
            return false
        }

        fun toMap() = mapOf(
            "themeName" to themeName,
            "isNightTheme" to isNightTheme,
            "primaryColor" to primaryColor,
            "accentColor" to accentColor,
            "backgroundColor" to backgroundColor,
            "bottomBackground" to bottomBackground,
            "transparentNavBar" to transparentNavBar,
            "backgroundImgPath" to backgroundImgPath,
            "backgroundImgBlur" to backgroundImgBlur
        )

    }

}
