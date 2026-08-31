package com.qreader.reader.ui.about

import android.os.Bundle
import android.view.View
import com.qreader.reader.R
import com.qreader.reader.base.BaseDialogFragment
import com.qreader.reader.databinding.DialogRecyclerViewBinding

/**
 * 应用日志对话框 - 已禁用
 */
class AppLogDialog : BaseDialogFragment(R.layout.dialog_recycler_view) {

    private val binding by lazy { DialogRecyclerViewBinding.bind(requireView()) }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        dismiss()
    }
}
