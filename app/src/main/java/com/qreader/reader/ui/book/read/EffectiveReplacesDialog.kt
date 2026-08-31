package com.qreader.reader.ui.book.read

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.qreader.reader.R
import com.qreader.reader.base.BaseDialogFragment
import com.qreader.reader.base.adapter.ItemViewHolder
import com.qreader.reader.base.adapter.RecyclerAdapter
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.ReplaceRule
import com.qreader.reader.databinding.DialogRecyclerViewBinding
import com.qreader.reader.databinding.Item1lineTextAndCloseBinding
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.lib.dialogs.alert
import com.qreader.reader.lib.theme.primaryColor
import com.qreader.reader.model.ReadBook
import com.qreader.reader.ui.replace.edit.ReplaceEditActivity
import com.qreader.reader.utils.setLayout
import com.qreader.reader.utils.viewbindingdelegate.viewBinding

/**
 * 起效的替换规则
 */
class EffectiveReplacesDialog : BaseDialogFragment(R.layout.dialog_recycler_view) {

    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val viewModel by activityViewModels<ReadBookViewModel>()
    private val adapter by lazy { ReplaceAdapter(requireContext()) }
    private val chineseConvert by lazy { ReplaceRule(0, "繁简转换") }

    private var isEdit = false

    private val editActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                isEdit = true
            }
        }

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.run {
            toolBar.setBackgroundColor(primaryColor)
            toolBar.setTitle(R.string.effective_replaces)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = adapter
        }
        val effectiveReplaceRules = ReadBook.curTextChapter?.effectiveReplaceRules ?: emptyList()
        if (AppConfig.chineseConverterType > 0) {
            adapter.setItems(effectiveReplaceRules + chineseConvert)
        } else {
            adapter.setItems(effectiveReplaceRules)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (isEdit) {
            viewModel.replaceRuleChanged()
        }
    }
    
    private fun showChineseConvertAlert() {
        alert(titleResource = R.string.chinese_converter) {
            items(resources.getStringArray(R.array.chinese_mode).toList()) { _, i ->
                if (AppConfig.chineseConverterType != i) {
                    AppConfig.chineseConverterType = i
                    isEdit = true
                }
            }
        }
    }

    private inner class ReplaceAdapter(context: Context) :
        RecyclerAdapter<ReplaceRule, Item1lineTextAndCloseBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): Item1lineTextAndCloseBinding {
            return Item1lineTextAndCloseBinding.inflate(inflater, parent, false)
        }

        override fun registerListener(holder: ItemViewHolder, binding: Item1lineTextAndCloseBinding) {
            binding.root.setOnClickListener {
                getItem(holder.layoutPosition)?.let { item ->
                    if (item == chineseConvert) {
                        showChineseConvertAlert()
                        return@let
                    }
                    editActivity.launch(ReplaceEditActivity.startIntent(requireContext(), item.id))
                }
            }
            binding.icClose.setOnClickListener {
                getItem(holder.layoutPosition)?.let { item ->
                    isEdit = true
                    removeItem(holder.layoutPosition)
                    if (item == chineseConvert) {
                        AppConfig.chineseConverterType = 0
                        return@let
                    }
                    item.isEnabled = false
                    appDb.replaceRuleDao.insert(item)
                }
            }
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: Item1lineTextAndCloseBinding,
            item: ReplaceRule,
            payloads: MutableList<Any>
        ) {
            binding.textView.text = item.name
        }

    }

}