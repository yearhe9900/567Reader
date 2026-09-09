package com.qreader.reader.ui.compose.glass

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Preference 列表选择弹框的 Compose 桥（阅读页设置玻璃面板内使用）。
 *
 * View 体系的 ListPreferenceDialog 跨窗口无法采样阅读页 backdrop；
 * 由 PreferenceFragment 拦截 onDisplayPreferenceDialog 后写入本 Host，
 * 再由 MoreConfigGlassSheet 用 LiquidGlassDialog 展示。
 */
object GlassListDialogHost {

    var isOpen by mutableStateOf(false)
        private set

    var title by mutableStateOf<CharSequence>("")
        private set

    var entries by mutableStateOf<Array<CharSequence>>(emptyArray())
        private set

    var entryValues by mutableStateOf<Array<CharSequence>>(emptyArray())
        private set

    var selectedValue by mutableStateOf<String?>(null)
        private set

    var onConfirm: ((String) -> Unit)? = null
        private set

    fun show(
        title: CharSequence,
        entries: Array<CharSequence>,
        entryValues: Array<CharSequence>,
        selectedValue: String?,
        onConfirm: (String) -> Unit,
    ) {
        this.title = title
        this.entries = entries
        this.entryValues = entryValues
        this.selectedValue = selectedValue
        this.onConfirm = onConfirm
        isOpen = true
    }

    fun dismiss() {
        isOpen = false
        onConfirm = null
    }

    fun confirm(value: String) {
        onConfirm?.invoke(value)
        dismiss()
    }
}
