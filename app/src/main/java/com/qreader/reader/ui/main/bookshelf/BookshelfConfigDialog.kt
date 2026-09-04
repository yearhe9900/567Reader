package com.qreader.reader.ui.main.bookshelf

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.qreader.reader.R
import com.qreader.reader.databinding.DialogBookshelfConfigBinding
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.utils.checkByIndex
import com.qreader.reader.utils.getCheckedIndex

/**
 * 书架布局 / 排序 / 开关 配置对话框。
 * 复用原版 dialog_bookshelf_config 布局，样式与原版一致（不重复造 UI）。
 * 确认后写回 AppConfig 并按需重建 Activity 以套用（与原版 postEvent(RECREATE) 等效）。
 */
class BookshelfConfigDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogBookshelfConfigBinding.inflate(layoutInflater)

        // 校正越界值（与原版 configBookshelf 一致）
        if (AppConfig.bookshelfLayout !in 0 until binding.rgLayout.childCount) {
            AppConfig.bookshelfLayout = 0
        }
        if (AppConfig.bookshelfSort !in 0 until binding.rgSort.childCount) {
            AppConfig.bookshelfSort = 0
        }

        binding.swShowUnread.isChecked = AppConfig.showUnread
        binding.swShowLastUpdateTime.isChecked = AppConfig.showLastUpdateTime
        binding.swShowWaitUpBooks.isChecked = AppConfig.showWaitUpCount
        binding.swShowBookshelfFastScroller.isChecked = AppConfig.showBookshelfFastScroller
        binding.rgLayout.checkByIndex(AppConfig.bookshelfLayout)
        binding.rgSort.checkByIndex(AppConfig.bookshelfSort)

        return AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.bookshelf_layout)
            setView(binding.root)
            setPositiveButton(R.string.ok) { _, _ -> save(binding) }
            setNegativeButton(R.string.cancel, null)
        }.create()
    }

    private fun save(binding: DialogBookshelfConfigBinding) {
        val activity = requireActivity()
        var changed = false

        if (AppConfig.showUnread != binding.swShowUnread.isChecked) {
            AppConfig.showUnread = binding.swShowUnread.isChecked
            changed = true
        }
        if (AppConfig.showLastUpdateTime != binding.swShowLastUpdateTime.isChecked) {
            AppConfig.showLastUpdateTime = binding.swShowLastUpdateTime.isChecked
            changed = true
        }
        if (AppConfig.showWaitUpCount != binding.swShowWaitUpBooks.isChecked) {
            AppConfig.showWaitUpCount = binding.swShowWaitUpBooks.isChecked
            changed = true
        }
        if (AppConfig.showBookshelfFastScroller != binding.swShowBookshelfFastScroller.isChecked) {
            AppConfig.showBookshelfFastScroller = binding.swShowBookshelfFastScroller.isChecked
            changed = true
        }
        if (AppConfig.bookshelfSort != binding.rgSort.getCheckedIndex()) {
            AppConfig.bookshelfSort = binding.rgSort.getCheckedIndex()
            changed = true
        }
        if (AppConfig.bookshelfLayout != binding.rgLayout.getCheckedIndex()) {
            AppConfig.bookshelfLayout = binding.rgLayout.getCheckedIndex()
            changed = true
        }

        // 固定值
        AppConfig.bookGroupStyle = 1   // Folder
        AppConfig.showBookname = 0     // 显示
        AppConfig.bookshelfMargin = 12 // 固定12dp

        if (changed) activity.recreate()
    }
}
