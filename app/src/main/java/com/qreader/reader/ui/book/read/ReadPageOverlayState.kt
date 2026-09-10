package com.qreader.reader.ui.book.read

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.help.config.ReadBookConfig
import com.qreader.reader.utils.ColorUtils

/**
 * 阅读页所有浮层（菜单 / 搜索栏 / 弹窗）的 Compose 状态容器。
 *
 * 由 [ReadBookActivity] 持有实例，传入 [ReadBookScreen] 供 Compose UI 读写；
 * Activity 业务逻辑通过写这些 state 驱动 UI 重组，替代原先 binding.readMenu.xxx() 调用。
 */
class ReadPageOverlayState {

    // ── ReadMenu 状态 ──

    /** 顶/底菜单是否可见 */
    var menuVisible by mutableStateOf(false)

    /** 菜单能否显示（触摸未进行中才可） */
    var canShowMenu by mutableStateOf(true)

    /** 当前章节名 */
    var chapterName by mutableStateOf("")

    /** 当前章节 URL（仅网络书源显示） */
    var chapterUrl by mutableStateOf<String?>(null)

    /** 是否为本地书 */
    var isLocalBook by mutableStateOf(false)

    /** 是否有自定义按钮 */
    var showCustomBtn by mutableStateOf(false)

    /** 进度条最大值（章节数 - 1） */
    var seekMax by mutableIntStateOf(0)

    /** 进度条当前值（章节索引） */
    var seekProgress by mutableIntStateOf(0)

    /** 进度条显示页码文本 */
    var seekPageText by mutableStateOf("")

    /** 用户正在拖动进度条（拖动期间禁止 Activity 回写 seekProgress，避免抢手势） */
    var isDraggingSeek by mutableStateOf(false)

    /** 自动翻页是否开启 */
    var autoPage by mutableStateOf(false)

    /** 上一章按钮是否可用 */
    var prevEnabled by mutableStateOf(true)

    /** 下一章按钮是否可用 */
    var nextEnabled by mutableStateOf(true)

    /** 是否正在朗读 */
    var isReadAloud by mutableStateOf(false)

    /** 是否夜间模式 */
    var isNightTheme by mutableStateOf(false)

    /** 是否有搜索结果 */
    var isShowingSearchResult by mutableStateOf(false)

    // ── 亮度 ──

    /** 亮度值 0f–1f（-1 = 跟随系统） */
    var brightness by mutableFloatStateOf(AppConfig.readBrightness.toFloat())

    /** 是否自动亮度 */
    var brightnessAuto by mutableStateOf(AppConfig.readBrightness == -1)

    /** 亮度条是否靠右 */
    var brightnessOnRight by mutableStateOf(AppConfig.brightnessVwPos)

    /** 是否显示亮度条（由 Activity 在 onResume 时同步） */
    var showBrightnessView by mutableStateOf(true)

    // ── SearchMenu 状态 ──

    var searchMenuVisible by mutableStateOf(false)

    /** 搜索结果列表（供 SearchMenu 显示） */
    var searchResults by mutableStateOf<List<com.qreader.reader.ui.book.searchContent.SearchResult>>(emptyList())

    /** 当前搜索结果索引 */
    var searchResultIndex by mutableIntStateOf(0)

    /** 当前搜索关键词 */
    var searchQuery by mutableStateOf("")

    // ── 弹窗开关 ──

    var showReadStyleDialog by mutableStateOf(false)
    var showMoreConfigDialog by mutableStateOf(false)
    var showAutoReadDialog by mutableStateOf(false)
    var showClickActionDialog by mutableStateOf(false)
    var showPaddingConfigDialog by mutableStateOf(false)
    var showReadAloudDialog by mutableStateOf(false)
    var showBgTextConfigDialog by mutableStateOf(false)
    var showTipConfigDialog by mutableStateOf(false)
    var showPageKeyDialog by mutableStateOf(false)
    var showReadAloudConfigDialog by mutableStateOf(false)

    /** 阅读页书签编辑（玻璃面板）；null = 关闭 */
    var pendingBookmark by mutableStateOf<com.qreader.reader.data.entities.Bookmark?>(null)
    var pendingBookmarkEditPos by mutableIntStateOf(-1)

    /** 换源玻璃面板是否打开 */
    var showChangeSourceDialog by mutableStateOf(false)

    /** 目录玻璃面板是否打开 */
    var showTocDialog by mutableStateOf(false)

    // ── 目录抽屉 ──
    var tocDrawerOpen by mutableStateOf(false)

    // ── 计数器（替代原 bottomDialog 计数）──
    var bottomDialogCount by mutableIntStateOf(0)

    // ── 辅助方法 ──

    /** 菜单是否处于显示状态（搜索/界面/设置/自动翻页/点击区域等玻璃面板） */
    val menuLayoutIsVisible: Boolean
        get() = bottomDialogCount > 0 || menuVisible || searchMenuVisible ||
            showMoreConfigDialog || showReadStyleDialog ||
            showAutoReadDialog || showClickActionDialog ||
            showPaddingConfigDialog || showTipConfigDialog ||
            showBgTextConfigDialog || showReadAloudConfigDialog ||
            showReadAloudDialog || showChangeSourceDialog ||
            showTocDialog || pendingBookmark != null

    /** 阅读页明暗判定（基于书页背景色，非 App 主题） */
    val isLightPage: Boolean
        get() = ColorUtils.isColorLight(ReadBookConfig.bgMeanColor)
}
