package com.qreader.reader.ui.main.bookshelf.style2

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qreader.reader.data.entities.Book
import com.qreader.reader.data.entities.BookGroup
import com.qreader.reader.databinding.ItemBookshelfListBinding
import com.qreader.reader.databinding.ItemBookshelfListGroupBinding
import com.qreader.reader.help.book.isLocal
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.utils.gone
import com.qreader.reader.utils.invisible
import com.qreader.reader.utils.visible
import splitties.views.onLongClick

@Suppress("UNUSED_PARAMETER")
class BooksAdapterList(context: Context, callBack: CallBack) :
    BaseBooksAdapter<RecyclerView.ViewHolder>(context, callBack) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> GroupViewHolder(ItemBookshelfListGroupBinding.inflate(inflater, parent, false))
            else -> BookViewHolder(ItemBookshelfListBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        when (holder) {
            is BookViewHolder -> (getItem(position) as? Book)?.let {
                holder.registerListener(it)
                holder.onBind(it, position, payloads)
            }

            is GroupViewHolder -> (getItem(position) as? BookGroup)?.let {
                holder.registerListener(it)
                holder.onBind(it, position, payloads)
            }
        }
    }

    inner class BookViewHolder(val binding: ItemBookshelfListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun onBind(item: Book, position: Int) = binding.run {
            tvName.text = item.name
            tvAuthor.text = item.author
            tvRead.text = item.durChapterTitle
            tvLast.text = item.latestChapterTitle
            ivCover.load(item, false)
            flHasNew.visible()
            ivAuthor.visible()
            ivLast.visible()
            ivRead.visible()
            upRefresh(this, item)
        }

        fun onBind(item: Book, position: Int, payloads: MutableList<Any>) = binding.run {
            if (payloads.isEmpty()) {
                onBind(item, position)
            } else {
                for (i in payloads.indices) {
                    val bundle = payloads[i] as Bundle
                    bundle.keySet().forEach {
                        when (it) {
                            "name" -> tvName.text = item.name
                            "author" -> tvAuthor.text = item.author
                            "dur" -> tvRead.text = item.durChapterTitle
                            "last" -> tvLast.text = item.latestChapterTitle
                            "cover" -> ivCover.load(
                                item,
                                false
                            )

                            "refresh" -> upRefresh(this, item)
                        }
                    }
                }
            }
        }

        fun registerListener(item: Any) {
            binding.root.setOnClickListener {
                callBack.onItemClick(item)
            }
            binding.root.onLongClick {
                callBack.onItemLongClick(item)
            }
        }

        private fun upRefresh(binding: ItemBookshelfListBinding, item: Book) {
            if (!item.isLocal && callBack.isUpdate(item.bookUrl)) {
                binding.bvUnread.invisible()
                binding.rlLoading.visible()
            } else {
                binding.rlLoading.gone()
                if (AppConfig.showUnread) {
                    binding.bvUnread.setHighlight(item.lastCheckCount > 0)
                    binding.bvUnread.setBadgeCount(item.getUnreadChapterNum())
                } else {
                    binding.bvUnread.invisible()
                }
            }
        }

    }

    inner class GroupViewHolder(val binding: ItemBookshelfListGroupBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun onBind(item: BookGroup, position: Int) = binding.run {
            tvName.text = item.groupName
            ivCover.load(item.cover)
            flHasNew.gone()
            ivAuthor.gone()
            ivLast.gone()
            ivRead.gone()
            tvAuthor.gone()
            tvLast.gone()
            tvRead.gone()
        }

        fun onBind(item: BookGroup, position: Int, payloads: MutableList<Any>) = binding.run {
            if (payloads.isEmpty()) {
                onBind(item, position)
            } else {
                for (i in payloads.indices) {
                    val bundle = payloads[i] as Bundle
                    bundle.keySet().forEach {
                        when (it) {
                            "groupName" -> tvName.text = item.groupName
                            "cover" -> ivCover.load(item.cover)
                        }
                    }
                }
            }
        }

        fun registerListener(item: Any) {
            binding.root.setOnClickListener {
                callBack.onItemClick(item)
            }
            binding.root.onLongClick {
                callBack.onItemLongClick(item)
            }
        }

    }

}