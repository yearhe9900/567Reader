package com.qreader.reader.ui.association

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.qreader.reader.R
import com.qreader.reader.base.BaseDialogFragment
import com.qreader.reader.base.adapter.ItemViewHolder
import com.qreader.reader.base.adapter.RecyclerAdapter
import com.qreader.reader.data.entities.TxtTocRule
import com.qreader.reader.databinding.DialogRecyclerViewBinding
import com.qreader.reader.databinding.ItemSourceImportBinding
import com.qreader.reader.lib.theme.primaryColor
import com.qreader.reader.ui.widget.dialog.CodeDialog
import com.qreader.reader.ui.widget.dialog.WaitDialog
import com.qreader.reader.utils.GSON
import com.qreader.reader.utils.gone
import com.qreader.reader.utils.setLayout
import com.qreader.reader.utils.showDialogFragment
import com.qreader.reader.utils.viewbindingdelegate.viewBinding
import com.qreader.reader.utils.visible
import splitties.views.onClick

class ImportTxtTocRuleDialog() : BaseDialogFragment(R.layout.dialog_recycler_view) {

    constructor(source: String, finishOnDismiss: Boolean = false) : this() {
        arguments = Bundle().apply {
            putString("source", source)
            putBoolean("finishOnDismiss", finishOnDismiss)
        }
    }

    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val viewModel by viewModels<ImportTxtTocRuleViewModel>()
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
        binding.toolBar.setTitle(R.string.import_txt_toc_rule)
        binding.rotateLoading.visible()
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
                adapter.setItems(viewModel.allSources)
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
        viewModel.importSource(source)
    }

    private fun upSelectText() {
        if (viewModel.isSelectAll) {
            binding.tvFooterLeft.text = getString(
                R.string.select_cancel_count,
                viewModel.selectCount,
                viewModel.allSources.size
            )
        } else {
            binding.tvFooterLeft.text = getString(
                R.string.select_all_count,
                viewModel.selectCount,
                viewModel.allSources.size
            )
        }
    }

    inner class SourcesAdapter(context: Context) :
        RecyclerAdapter<TxtTocRule, ItemSourceImportBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): ItemSourceImportBinding {
            return ItemSourceImportBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemSourceImportBinding,
            item: TxtTocRule,
            payloads: MutableList<Any>
        ) {
            binding.apply {
                cbSourceName.isChecked = viewModel.selectStatus[holder.layoutPosition]
                cbSourceName.text = item.name
                item.example?.takeIf{ it.isNotBlank() }?.let {
                    showComment.text = it
                    showComment.visible()
                    showComment.setOnClickListener {
                            if (showComment.maxLines == 3) {
                                showComment.maxLines = 39
                            } else {
                                showComment.maxLines = 3
                            }
                        }
                } ?: run {
                    showComment.gone()
                }
                val localSource = viewModel.checkSources[holder.layoutPosition]
                tvSourceState.text = when {
                    localSource == null -> "新增"
                    item != localSource -> "更新"
                    else -> "已有"
                }
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemSourceImportBinding) {
            binding.apply {
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
                    val source = viewModel.allSources[holder.layoutPosition]
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