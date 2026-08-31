package com.qreader.reader.ui.book.read.config

import android.content.Context
import android.view.ViewGroup
import com.qreader.reader.base.adapter.ItemViewHolder
import com.qreader.reader.base.adapter.RecyclerAdapter
import com.qreader.reader.constant.EventBus
import com.qreader.reader.databinding.ItemBgImageBinding
import com.qreader.reader.help.config.ReadBookConfig
import com.qreader.reader.help.glide.ImageLoader
import com.qreader.reader.utils.postEvent
import java.io.File

class BgAdapter(context: Context, val textColor: Int) :
    RecyclerAdapter<String, ItemBgImageBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemBgImageBinding {
        return ItemBgImageBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemBgImageBinding,
        item: String,
        payloads: MutableList<Any>
    ) {
        binding.run {
            ImageLoader.load(
                context,
                context.assets.open("bg${File.separator}$item").readBytes()
            )
                .centerCrop()
                .into(ivBg)
            tvName.setTextColor(textColor)
            tvName.text = item.substringBeforeLast(".")
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemBgImageBinding) {
        holder.itemView.apply {
            this.setOnClickListener {
                getItemByLayoutPosition(holder.layoutPosition)?.let {
                    ReadBookConfig.durConfig.setCurBg(1, it)
                    postEvent(EventBus.UP_CONFIG, arrayListOf(1))
                }
            }
        }
    }
}