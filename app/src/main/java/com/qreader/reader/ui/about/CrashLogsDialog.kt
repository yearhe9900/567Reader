package com.qreader.reader.ui.about

import android.os.Bundle
import com.qreader.reader.R
import com.qreader.reader.base.BaseDialogFragment
import com.qreader.reader.databinding.DialogRecyclerViewBinding

/**
 * 崩溃日志对话框 - 已禁用
 */
class CrashLogsDialog : BaseDialogFragment<DialogRecyclerViewBinding>() {

    override fun onFragmentCreated(view: android.view.View, savedInstanceState: Bundle?) {
        dismiss()
    }
}
