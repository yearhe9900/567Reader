package com.qreader.reader.ui.book.read.config

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.get
import com.github.liuyueyi.quick.transfer.constants.TransType
import com.kyant.backdrop.Backdrop
import com.qreader.reader.R
import com.qreader.reader.base.adapter.ItemViewHolder
import com.qreader.reader.base.adapter.RecyclerAdapter
import com.qreader.reader.constant.EventBus
import com.qreader.reader.databinding.DialogReadBookStyleBinding
import com.qreader.reader.databinding.ItemReadStyleBinding
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.help.config.ReadBookConfig
import com.qreader.reader.lib.dialogs.selector
import com.qreader.reader.lib.theme.accentColor
import com.qreader.reader.model.ReadBook
import com.qreader.reader.ui.book.read.ReadBookActivity
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.GlassToggleHost
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog
import com.qreader.reader.ui.font.FontSelectDialog
import com.qreader.reader.utils.ChineseUtils
import com.qreader.reader.utils.ColorUtils
import com.qreader.reader.utils.dpToPx
import com.qreader.reader.utils.getIndexById
import com.qreader.reader.utils.postEvent
import com.qreader.reader.utils.showDialogFragment
import splitties.views.onLongClick

/**
 * 阅读页「界面」玻璃底部面板（in-tree，真采样正文）。
 *
 * 与 [MoreConfigGlassSheet] 同构：LiquidGlassDialog 底栏 + AndroidView 托管原布局；
 * 业务由 [ReadStyleBinder] 承载，不再走独立 Dialog 窗口。
 * DetailSeekBar 的 LiquidSlider 经 [GlassToggleHost] 取 backdrop。
 */
@Composable
fun ReadStyleGlassSheet(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
) {
    val activity = LocalContext.current as? ReadBookActivity ?: return
    val binder = remember { ReadStyleBinder(activity) }
    val isLightPage = remember { ColorUtils.isColorLight(ReadBookConfig.bgMeanColor) }

    DisposableEffect(backdrop, isLightPage) {
        GlassToggleHost.attach(backdrop, isLightPage)
        onDispose { GlassToggleHost.detach() }
    }

    LiquidGlassDialog(
        backdrop = backdrop,
        onDismiss = {
            binder.save()
            onDismiss()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(GlassConfig.sheetHeight)
            .navigationBarsPadding()
            .padding(horizontal = GlassConfig.sheetHorizontalPadding),
        cardRadius = GlassConfig.sheetCornerRadius,
        alignment = Alignment.BottomCenter,
        showScrim = false,
    ) {
        AndroidView(
            factory = { ctx ->
                val binding = DialogReadBookStyleBinding.inflate(LayoutInflater.from(ctx))
                binder.attach(binding)
                binding.root
            },
            modifier = Modifier.fillMaxWidth(),
            onRelease = { binder.detach() },
        )
    }

    DisposableEffect(Unit) {
        onDispose { binder.save() }
    }
}

/** 界面面板业务绑定：与原 ReadStyleDialog 对齐，宿主为玻璃底栏。 */
private class ReadStyleBinder(
    private val activity: ReadBookActivity,
) : FontSelectDialog.CallBack {

    private var binding: DialogReadBookStyleBinding? = null
    private var styleAdapter: StyleAdapter? = null

    fun attach(binding: DialogReadBookStyleBinding) {
        this.binding = binding
        val adapter = StyleAdapter()
        styleAdapter = adapter
        binding.rvStyle.adapter = adapter
        initStatic(binding)
        initEvents(binding)
        upView(binding)
        adapter.setItems(ReadBookConfig.configList)
    }

    fun detach() {
        binding = null
        styleAdapter = null
    }

    fun save() {
        ReadBookConfig.save()
    }

    private fun initStatic(binding: DialogReadBookStyleBinding) = binding.run {
        chineseConverter.onChanged {
            ChineseUtils.unLoad(*TransType.entries.toTypedArray())
            postEvent(EventBus.UP_CONFIG, arrayListOf(5))
        }
        textFontWeightConverter.onChanged {
            postEvent(EventBus.UP_CONFIG, arrayListOf(8, 9, 6))
        }
        dsbTextSize.valueFormat = { (it + 5).toString() }
        dsbTextLetterSpacing.valueFormat = { ((it - 50) / 100f).toString() }
        dsbLineSize.valueFormat = { ((it - 10) / 10f).toString() }
        dsbParagraphSpacing.valueFormat = { (it / 10f).toString() }
    }

    private fun initEvents(binding: DialogReadBookStyleBinding) = binding.run {
        cbShareLayout.isChecked = ReadBookConfig.shareLayout

        styleAdapter?.addFooterView {
            ItemReadStyleBinding.inflate(activity.layoutInflater, it, false).apply {
                ivStyle.setPadding(6.dpToPx(), 6.dpToPx(), 6.dpToPx(), 6.dpToPx())
                ivStyle.setText(null)
                val c = ReadBookConfig.durConfig.curTextColor()
                ivStyle.setColorFilter(c)
                ivStyle.borderColor = c
                ivStyle.setImageResource(R.drawable.ic_add)
                root.setOnClickListener {
                    ReadBookConfig.configList.add(ReadBookConfig.Config())
                    showBgTextConfig(ReadBookConfig.configList.lastIndex)
                }
            }
        }

        tvTextFont.setOnClickListener {
            activity.showDialogFragment<FontSelectDialog>()
        }
        tvTextIndent.setOnClickListener {
            activity.selector(
                title = activity.getString(R.string.text_indent),
                items = activity.resources.getStringArray(R.array.indent).toList(),
            ) { _, index ->
                ReadBookConfig.paragraphIndent = "　".repeat(index)
                postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
            }
        }
        tvPadding.setOnClickListener {
            activity.showPaddingConfig()
        }
        tvTip.setOnClickListener {
            TipConfigDialog().show(activity.supportFragmentManager, "tipConfigDialog")
        }
        rgPageAnim.setOnCheckedChangeListener { _, checkedId ->
            ReadBook.book?.setPageAnim(-1)
            ReadBookConfig.pageAnim = rgPageAnim.getIndexById(checkedId)
            activity.upPageAnim()
            ReadBook.loadContent(false)
        }
        cbShareLayout.onCheckedChangeListener = { _, isChecked ->
            ReadBookConfig.shareLayout = isChecked
            upView(binding)
            postEvent(EventBus.UP_CONFIG, arrayListOf(1, 2, 5))
        }
        dsbTextSize.onChanged = {
            ReadBookConfig.textSize = it + 5
            postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
        }
        dsbTextLetterSpacing.onChanged = {
            ReadBookConfig.letterSpacing = (it - 50) / 100f
            postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
        }
        dsbLineSize.onChanged = {
            ReadBookConfig.lineSpacingExtra = it
            postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
        }
        dsbParagraphSpacing.onChanged = {
            ReadBookConfig.paragraphSpacing = it
            postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
        }
    }

    private fun changeBgTextConfig(index: Int) {
        val oldIndex = ReadBookConfig.styleSelect
        if (index != oldIndex) {
            ReadBookConfig.styleSelect = index
            binding?.let { upView(it) }
            styleAdapter?.notifyItemChanged(oldIndex)
            styleAdapter?.notifyItemChanged(index)
            postEvent(EventBus.UP_CONFIG, arrayListOf(1, 2, 5))
            if (AppConfig.readBarStyleFollowPage) {
                postEvent(EventBus.UPDATE_READ_ACTION_BAR, true)
            }
        }
    }

    private fun showBgTextConfig(index: Int) {
        changeBgTextConfig(index)
        activity.showBgTextConfig()
    }

    private fun upView(binding: DialogReadBookStyleBinding) = binding.run {
        textFontWeightConverter.upUi(ReadBookConfig.textBold)
        ReadBook.pageAnim().let {
            if (it >= 0 && it < rgPageAnim.childCount) {
                rgPageAnim.check(rgPageAnim.getChildAt(it).id)
            }
        }
        ReadBookConfig.let {
            dsbTextSize.progress = it.textSize - 5
            dsbTextLetterSpacing.progress = (it.letterSpacing * 100).toInt() + 50
            dsbLineSize.progress = it.lineSpacingExtra
            dsbParagraphSpacing.progress = it.paragraphSpacing
        }
    }

    override val curFontPath: String
        get() = ReadBookConfig.textFont

    override fun selectFont(path: String) {
        if (path != ReadBookConfig.textFont || path.isEmpty()) {
            ReadBookConfig.textFont = path
            postEvent(EventBus.UP_CONFIG, arrayListOf(2, 5))
        }
    }

    private inner class StyleAdapter :
        RecyclerAdapter<ReadBookConfig.Config, ItemReadStyleBinding>(activity) {

        override fun getViewBinding(parent: ViewGroup): ItemReadStyleBinding {
            return ItemReadStyleBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemReadStyleBinding,
            item: ReadBookConfig.Config,
            payloads: MutableList<Any>,
        ) {
            binding.apply {
                ivStyle.setText(item.name.ifBlank { "文字" })
                ivStyle.setTextColor(item.curTextColor())
                ivStyle.setImageDrawable(item.curBgDrawable(100, 150))
                if (ReadBookConfig.styleSelect == holder.layoutPosition) {
                    ivStyle.borderColor = activity.accentColor
                    ivStyle.setTextBold(true)
                } else {
                    ivStyle.borderColor = item.curTextColor()
                    ivStyle.setTextBold(false)
                }
            }
        }

        override fun registerListener(
            holder: ItemViewHolder,
            binding: ItemReadStyleBinding,
        ) {
            binding.apply {
                ivStyle.setOnClickListener {
                    if (ivStyle.isInView) {
                        changeBgTextConfig(holder.layoutPosition)
                    }
                }
                ivStyle.onLongClick(ivStyle.isInView) {
                    if (ivStyle.isInView) {
                        showBgTextConfig(holder.layoutPosition)
                    }
                }
            }
        }
    }
}
