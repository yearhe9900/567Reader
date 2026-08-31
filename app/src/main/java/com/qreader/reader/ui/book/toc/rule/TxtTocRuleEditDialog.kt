package com.qreader.reader.ui.book.toc.rule

import android.app.Application
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import com.qreader.reader.R
import com.qreader.reader.base.BaseDialogFragment
import com.qreader.reader.base.BaseViewModel
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.TxtTocRule
import com.qreader.reader.databinding.DialogTocRegexEditBinding
import com.qreader.reader.exception.NoStackTraceException
import com.qreader.reader.lib.dialogs.alert
import com.qreader.reader.lib.theme.primaryColor
import com.qreader.reader.ui.widget.code.addJsPattern
import com.qreader.reader.ui.widget.code.addJsonPattern
import com.qreader.reader.utils.*
import com.qreader.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import kotlin.toString

class TxtTocRuleEditDialog() : BaseDialogFragment(R.layout.dialog_toc_regex_edit, true),
    Toolbar.OnMenuItemClickListener {

    constructor(id: Long?) : this() {
        id ?: return
        arguments = Bundle().apply {
            putLong("id", id)
        }
    }

    private val binding by viewBinding(DialogTocRegexEditBinding::bind)
    private val viewModel by viewModels<ViewModel>()
    private val callback get() = (parentFragment as? Callback) ?: activity as? Callback

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        initMenu()
        viewModel.initData(arguments?.getLong("id")) {
            upRuleView(it)
        }
    }

    private fun initMenu() {
        binding.toolBar.inflateMenu(R.menu.txt_toc_rule_edit)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_save -> {
                val tocRule = getRuleFromView()
                if (checkValid(tocRule)) {
                    callback?.saveTxtTocRule(tocRule)
                    dismissAllowingStateLoss()
                }
            }
            R.id.menu_copy_rule -> context?.sendToClip(GSON.toJson(getRuleFromView()))
            R.id.menu_paste_rule -> viewModel.pasteRule {
                upRuleView(it)
            }
        }
        return true
    }

    private fun checkValid(tocRule: TxtTocRule): Boolean {
        if (tocRule.name.isEmpty()) {
            toastOnUi("名称不能为空")
            return false
        }

        try {
            Pattern.compile(tocRule.rule, Pattern.MULTILINE)
        } catch (ex: PatternSyntaxException) {
            return false
        }

        return true
    }

    private fun upRuleView(tocRule: TxtTocRule?) {
        binding.tvRuleName.setText(tocRule?.name)
        binding.tvRuleRegex.setText(tocRule?.rule)
        binding.tvRuleReplacement.apply{
            addJsonPattern()
            addJsPattern()
            setText(tocRule?.replacement)
        }
        binding.tvRuleExample.setText(tocRule?.example)
    }

    private fun getRuleFromView(): TxtTocRule {
        val tocRule = viewModel.tocRule ?: TxtTocRule().apply {
            viewModel.tocRule = this
        }
        binding.run {
            tocRule.name = tvRuleName.text.toString()
            tocRule.rule = tvRuleRegex.text.toString()
            tocRule.replacement = tvRuleReplacement.text.toString()
            tocRule.example = tvRuleExample.text.toString()
        }
        return tocRule
    }

    private fun isSame(): Boolean{
        val tocRule = viewModel.tocRule ?: return binding.tvRuleName.text.toString().isEmpty()
        return binding.run {
            tocRule.name == tvRuleName.text.toString() &&
            tocRule.rule == tvRuleRegex.text.toString() &&
            tocRule.replacement == tvRuleReplacement.text.toString() &&
            tocRule.example == tvRuleExample.text.toString()
        }
    }

    override fun dismiss() {
        if (!isSame()) {
            alert(R.string.exit) {
                setMessage(R.string.exit_no_save)
                positiveButton(R.string.yes)
                negativeButton(R.string.no) {
                    super.dismiss()
                }
            }
        } else {
            super.dismiss()
        }
    }

    class ViewModel(application: Application) : BaseViewModel(application) {

        var tocRule: TxtTocRule? = null

        fun initData(id: Long?, finally: (tocRule: TxtTocRule?) -> Unit) {
            if (tocRule != null) return
            execute {
                if (id == null) return@execute
                tocRule = appDb.txtTocRuleDao.get(id)
            }.onFinally {
                finally.invoke(tocRule)
            }
        }

        fun pasteRule(success: (TxtTocRule) -> Unit) {
            execute(context = Dispatchers.Main) {
                val text = context.getClipText()
                if (text.isNullOrBlank()) {
                    throw NoStackTraceException("剪贴板为空")
                }
                GSON.fromJsonObject<TxtTocRule>(text).getOrNull()
                    ?: throw NoStackTraceException("格式不对")
            }.onSuccess {
                success.invoke(it)
            }.onError {
                context.toastOnUi(it.localizedMessage ?: "Error")
                it.printOnDebug()
            }
        }

    }

    interface Callback {

        fun saveTxtTocRule(txtTocRule: TxtTocRule)

    }

}