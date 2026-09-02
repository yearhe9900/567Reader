package com.qreader.reader.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.qreader.reader.ui.main.my.MySettingsScreen

/**
 * 设置页 —— Compose 页面（供 HorizontalPager 使用）。
 *
 * 直接包装 [MySettingsScreen]，移除 MyFragment 的 Fragment 壳。
 */
@Composable
fun SettingsPage(
    webServiceChecked: Boolean,
    webServiceSummary: String,
    themeModeIndex: Int,
    onActionClick: (String) -> Unit,
    onWebServiceToggle: (Boolean) -> Unit,
    onThemeModeSelected: (Int) -> Unit,
    themeDialogOpen: Boolean,
    onThemeDialogOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    MySettingsScreen(
        webServiceChecked = webServiceChecked,
        webServiceSummary = webServiceSummary,
        themeModeIndex = themeModeIndex,
        onActionClick = onActionClick,
        onWebServiceToggle = onWebServiceToggle,
        onThemeModeSelected = onThemeModeSelected,
        themeDialogOpen = themeDialogOpen,
        onThemeDialogOpenChange = onThemeDialogOpenChange,
        modifier = modifier,
    )
}
