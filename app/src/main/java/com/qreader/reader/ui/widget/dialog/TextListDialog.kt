package com.qreader.reader.ui.widget.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.qreader.reader.R
import com.qreader.reader.base.BaseDialogFragment
import com.qreader.reader.base.adapter.ItemViewHolder
import com.qreader.reader.base.adapter.RecyclerAdapter
import com.qreader.reader.databinding.DialogRecyclerViewBinding
import com.qreader.reader.databinding.ItemLogBinding
import com.qreader.reader.utils.setLayout
import com.qreader.reader.utils.viewbindingdelegate.viewBinding

@Suppress("unused")
class TextListDialog() : BaseDialogFragment(R.layout.dialog_recycler_view) {

    constructor(title: String, values: ArrayList<String>) : this() {
        arguments = Bundle().apply {
            putString("title", title)
            putStringArrayList("values", values)
        }
    }

    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val adapter by lazy { TextAdapter(requireContext()) }
    private var values: ArrayList<String>? = null

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, 0.9f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) = binding.run {
        arguments?.let {
            toolBar.title = it.getString("title")
            values = it.getStringArrayList("values")
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        adapter.setItems(values)
    }

    class TextAdapter(context: Context) :
        RecyclerAdapter<String, ItemLogBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): ItemLogBinding {
            return ItemLogBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemLogBinding,
            item: String,
            payloads: MutableList<Any>
        ) {
            binding.apply {
                if (textView.getTag(R.id.tag1) == null) {
                    val listener = object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(v: View) {
                            textView.isCursorVisible = false
                            textView.isCursorVisible = true
                        }

                        override fun onViewDetachedFromWindow(v: View) {}
                    }
                    textView.addOnAttachStateChangeListener(listener)
                    textView.setTag(R.id.tag1, listener)
                }
                textView.text = item
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemLogBinding) {
            //nothing
        }
    }

}