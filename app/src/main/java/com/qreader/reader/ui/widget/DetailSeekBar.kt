package com.qreader.reader.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.appcompat.widget.TooltipCompat
import androidx.compose.ui.graphics.toArgb
import com.qreader.reader.R
import com.qreader.reader.databinding.ViewDetailSeekBarBinding
import com.qreader.reader.help.config.ReadBookConfig
import com.qreader.reader.lib.theme.bottomBackground
import com.qreader.reader.lib.theme.getPrimaryTextColor
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.widget.seekbar.SeekBarChangeListener
import com.qreader.reader.utils.ColorUtils
import com.qreader.reader.utils.progressAdd

/**
 * 界面面板滑杆（Seekbar + GlassConfig 着色）。
 *
 * 不在 AndroidView 内嵌 Compose LiquidSlider：layout 期间写 snapshot state
 * 会触发无限 relayout，整页卡死。液态玻璃滑块需整页 Compose 化后再做。
 */
class DetailSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs),
    SeekBarChangeListener {
    private var binding: ViewDetailSeekBarBinding =
        ViewDetailSeekBarBinding.inflate(LayoutInflater.from(context), this, true)
    private val isBottomBackground: Boolean

    var valueFormat: ((progress: Int) -> String)? = null
    var onChanged: ((progress: Int) -> Unit)? = null
    var progress: Int
        get() = binding.seekBar.progress
        set(value) {
            binding.seekBar.progress = value
            upValue()
        }
    var max: Int
        get() = binding.seekBar.max
        set(value) {
            binding.seekBar.max = value
        }

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.DetailSeekBar)
        isBottomBackground =
            typedArray.getBoolean(R.styleable.DetailSeekBar_isBottomBackground, false)
        val title = typedArray.getText(R.styleable.DetailSeekBar_title)
        binding.tvSeekTitle.apply {
            text = title
            TooltipCompat.setTooltipText(this, title)
        }
        binding.seekBar.max = typedArray.getInteger(R.styleable.DetailSeekBar_max, 0)
        typedArray.recycle()
        if (!isInEditMode) {
            applyGlassTint()
        }
        if (isBottomBackground && !isInEditMode) {
            val isLight = ColorUtils.isColorLight(context.bottomBackground)
            val textColor = context.getPrimaryTextColor(isLight)
            binding.tvSeekTitle.setTextColor(textColor)
            binding.ivSeekPlus.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
            binding.ivSeekReduce.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
            binding.tvSeekValue.setTextColor(textColor)
        }
        binding.ivSeekPlus.setOnClickListener {
            binding.seekBar.progressAdd(1)
            onChanged?.invoke(binding.seekBar.progress)
        }
        binding.ivSeekReduce.setOnClickListener {
            binding.seekBar.progressAdd(-1)
            onChanged?.invoke(binding.seekBar.progress)
        }
        binding.seekBar.setOnSeekBarChangeListener(this)
    }

    /** 轨道/进度/滑块走 GlassConfig，明暗按阅读页背景。 */
    private fun applyGlassTint() {
        val isLight = ColorUtils.isColorLight(ReadBookConfig.bgMeanColor)
        val track = GlassConfig.toggleTrackColor(isLight)
        val progress = GlassConfig.toggleAccentColor(isLight)
        binding.seekBar.apply {
            progressBackgroundTintList = ColorStateList.valueOf(track.toArgb())
            progressTintList = ColorStateList.valueOf(progress.toArgb())
            thumbTintList = ColorStateList.valueOf(Color.WHITE)
        }
    }

    private fun upValue(progress: Int = binding.seekBar.progress) {
        binding.tvSeekValue.text = valueFormat?.invoke(progress) ?: progress.toString()
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        upValue(progress)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        onChanged?.invoke(binding.seekBar.progress)
    }
}
