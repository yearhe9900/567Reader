package com.qreader.reader.ui.association

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.qreader.reader.R
import com.qreader.reader.base.BaseDialogFragment
import com.qreader.reader.base.adapter.ItemViewHolder
import com.qreader.reader.base.adapter.RecyclerAdapter
import com.qreader.reader.constant.PreferKey
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.ReplaceRule
import com.qreader.reader.databinding.DialogCustomGroupBinding
import com.qreader.reader.databinding.DialogRecyclerViewBinding
import com.qreader.reader.databinding.ItemSourceImportBinding
import com.qreader.reader.lib.dialogs.alert
import com.qreader.reader.lib.theme.primaryColor
import com.qreader.reader.ui.widget.dialog.CodeDialog
import com.qreader.reader.ui.widget.dialog.WaitDialog
import com.qreader.reader.utils.GSON
import com.qreader.reader.utils.dpToPx
import com.qreader.reader.utils.fromJsonObject
import com.qreader.reader.utils.putPrefBoolean
import com.qreader.reader.utils.setLayout
import com.qreader.reader.utils.showDialogFragment
import com.qreader.reader.utils.viewbindingdelegate.viewBinding
import com.qreader.reader.utils.visible
import splitties.views.onClick

class ImportReplaceRuleDialog() : BaseDialogFragment(R.layout.dialog_recycler_view),
    Toolbar.OnMenuItemClickListener,
    CodeDialog.Callback {

    constructor(source: String, finishOnDismiss: Boolean = false) : this() {
        arguments = Bundle().apply {
            putString("source", source)
            putBoolean("finishOnDismiss", finishOnDismiss)
        }
    }

    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val viewModel by viewModels<ImportReplaceRuleViewModel>()
    private val adapter by lazy { SourcesAdapter(requireContext()) }

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (arguments?.getBoolean("finishOnDismiss") == true) {
            activity?.finish()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.setTitle(R.string.import_replace_rule)
        binding.rotateLoading.visible()
        initMenu()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.tvCancel.visible()
        binding.tvCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.tvOk.visible()
        binding.tvOk.setOnClickListener {
            val waitDialog = WaitDialog(requireContext())
            waitDialog.show()
            viewModel.importSelect {
                waitDialog.dismiss()
                dismissAllowingStateLoss()
            }
        }
        binding.tvFooterLeft.visible()
        binding.tvFooterLeft.setOnClickListener {
            val selectAll = viewModel.isSelectAll
            viewModel.selectStatus.forEachIndexed { index, b ->
                if (b != !selectAll) {
                    viewModel.selectStatus[index] = !selectAll
                }
            }
            adapter.notifyDataSetChanged()
            upSelectText()
        }
        viewModel.errorLiveData.observe(this) {
            binding.rotateLoading.gone()
            binding.tvMsg.apply {
                text = it
                visible()
            }
        }
        viewModel.successLiveData.observe(this) {
            binding.rotateLoading.gone()
            if (it > 0) {
                adapter.setItems(viewModel.allRules)
                upSelectText()
            } else {
                binding.tvMsg.apply {
                    setText(R.string.wrong_format)
                    visible()
                }
            }
        }
        val source = arguments?.getString("source")
        if (source.isNullOrEmpty()) {
            dismiss()
            return
        }
        viewModel.import(source)
    }

    private fun initMenu() {
        binding.toolBar.setOnMenuItemClickListener(this)
        binding.toolBar.inflateMenu(R.menu.import_replace)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_new_group -> alertCustomGroup(item)
            R.id.menu_keep_original_name -> {
                item.isChecked = !item.isChecked
                putPrefBoolean(PreferKey.importKeepName, item.isChecked)
            }
        }
        return true
    }

    private fun alertCustomGroup(item: MenuItem) {
        alert(R.string.diy_edit_source_group) {
            val alertBinding = DialogCustomGroupBinding.inflate(layoutInflater).apply {
                val groups = appDb.replaceRuleDao.allGroups()
                textInputLayout.setHint(R.string.group_name)
                editView.setFilterValues(groups.toList())
                editView.dropDownHeight = 180.dpToPx()
            }
            customView {
                alertBinding.root
            }
            okButton {
                viewModel.isAddGroup = alertBinding.swAddGroup.isChecked
                viewModel.groupName = alertBinding.editView.text?.toString()
                if (viewModel.groupName.isNullOrBlank()) {
                    item.title = getString(R.string.diy_source_group)
                } else {
                    val group = getString(R.string.diy_edit_source_group_title, viewModel.groupName)
                    if (viewModel.isAddGroup) {
                        item.title = "+$group"
                    } else {
                        item.title = group
                    }
                }
            }
            noButton()
        }
    }

    private fun upSelectText() {
        if (viewModel.isSelectAll) {
            binding.tvFooterLeft.text = getString(
                R.string.select_cancel_count,
                viewModel.selectCount,
                viewModel.allRules.size
            )
        } else {
            binding.tvFooterLeft.text = getString(
                R.string.select_all_count,
                viewModel.selectCount,
                viewModel.allRules.size
            )
        }
    }

    override fun onCodeSave(code: String, requestId: String?) {
        requestId?.toInt()?.let {
            GSON.fromJsonObject<ReplaceRule>(code).getOrNull()?.let { rule ->
                viewModel.allRules[it] = rule
                adapter.setItem(it, rule)
            }
        }
    }

    inner class SourcesAdapter(context: Context) :
        RecyclerAdapter<ReplaceRule, ItemSourceImportBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): ItemSourceImportBinding {
            return ItemSourceImportBinding.inflate(inflater, parent, false)
        }

        @SuppressLint("SetTextI18n")
        override fun convert(
            holder: ItemViewHolder,
            binding: ItemSourceImportBinding,
            item: ReplaceRule,
            payloads: MutableList<Any>
        ) {
            binding.run {
                cbSourceName.isChecked = viewModel.selectStatus[holder.layoutPosition]
                cbSourceName.text = if (item.group.isNullOrBlank()) {
                    item.name
                } else {
                    "${item.name}(${item.group})"
                }
                val localRule = viewModel.checkRules[holder.layoutPosition]
                tvSourceState.text = when {
                    localRule == null -> "新增"
                    item.pattern != localRule.pattern
                            || item.replacement != localRule.replacement
                            || item.isRegex != localRule.isRegex
                            || item.scope != localRule.scope -> "更新"

                    else -> "已有"
                }
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemSourceImportBinding) {
            binding.run {
                cbSourceName.setOnUserCheckedChangeListener { isChecked ->
                    viewModel.selectStatus[holder.layoutPosition] = isChecked
                    upSelectText()
                }
                root.onClick {
                    cbSourceName.isChecked = !cbSourceName.isChecked
                    viewModel.selectStatus[holder.layoutPosition] = cbSourceName.isChecked
                    upSelectText()
                }
                tvOpen.setOnClickListener {
                    val source = viewModel.allRules[holder.layoutPosition]
                    showDialogFragment(
                        CodeDialog(
                            GSON.toJson(source),
                            disableEdit = false,
                            requestId = holder.layoutPosition.toString()
                        )
                    )
                }
            }
        }

    }

}