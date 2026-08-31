package com.qreader.reader.ui.widget.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import com.qreader.reader.R
import com.qreader.reader.constant.AppConst
import com.qreader.reader.databinding.DialogUrlOptionEditBinding
import com.qreader.reader.model.analyzeRule.AnalyzeUrl
import com.qreader.reader.utils.GSON
import com.qreader.reader.utils.setLayout

class UrlOptionDialog(context: Context, private val success: (String) -> Unit) : Dialog(context) {

    val binding = DialogUrlOptionEditBinding.inflate(layoutInflater)

    override fun onStart() {
        super.onStart()
        setLayout(1f, ViewGroup.LayoutParams.MATCH_PARENT)
        window?.setBackgroundDrawableResource(R.color.transparent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.root.setOnClickListener { dismiss() }
        binding.vwBg.setOnClickListener(null)
        binding.editMethod.setFilterValues("POST", "GET")
        binding.editCharset.setFilterValues(AppConst.charsets)
        binding.tvOk.setOnClickListener {
            success.invoke(GSON.toJson(getUrlOption()))
            dismiss()
        }
    }

    private fun getUrlOption(): AnalyzeUrl.UrlOption {
        val urlOption = AnalyzeUrl.UrlOption()
        urlOption.useWebView(binding.cbUseWebView.isChecked)
        urlOption.setMethod(binding.editMethod.text.toString())
        urlOption.setCharset(binding.editCharset.text.toString())
        urlOption.setHeaders(binding.editHeaders.text.toString())
        urlOption.setBody(binding.editBody.text.toString())
        urlOption.setRetry(binding.editRetry.text.toString())
        urlOption.setType(binding.editType.text.toString())
        urlOption.setWebJs(binding.editWebJs.text.toString())
        urlOption.setJs(binding.editJs.text.toString())
        urlOption.setJs(binding.editBodyJs.text.toString())
        urlOption.setDnsIp(binding.editDnsIp.text.toString())
        return urlOption
    }

}