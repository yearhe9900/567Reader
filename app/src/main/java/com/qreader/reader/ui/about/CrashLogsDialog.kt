package com.qreader.reader.ui.about

import android.os.Bundle
import android.view.View
import com.qreader.reader.R
import com.qreader.reader.base.BaseDialogFragment

/**
 * 崩溃日志对话框 - 已禁用
 */
class CrashLogsDialog : BaseDialogFragment(R.layout.dialog_recycler_view) {

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        dismiss()
    }
}
