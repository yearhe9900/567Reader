package com.qreader.reader.ui.login

import android.os.Bundle
import androidx.activity.viewModels
import com.qreader.reader.R
import com.qreader.reader.base.VMBaseActivity
import com.qreader.reader.data.entities.BaseSource
import com.qreader.reader.databinding.ActivitySourceLoginBinding
import com.qreader.reader.utils.showDialogFragment
import com.qreader.reader.utils.viewbindingdelegate.viewBinding


class SourceLoginActivity : VMBaseActivity<ActivitySourceLoginBinding, SourceLoginViewModel>() {

    override val binding by viewBinding(ActivitySourceLoginBinding::inflate)
    override val viewModel by viewModels<SourceLoginViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.initData(intent, success = { source ->
            initView(source)
        }, error = {
            finish()
        })
    }

    private fun initView(source: BaseSource) {
        if (source.loginUi.isNullOrEmpty()) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fl_fragment, WebViewLoginFragment(), "webViewLogin")
                .commit()
        } else {
            showDialogFragment<SourceLoginDialog>()
        }
    }

}