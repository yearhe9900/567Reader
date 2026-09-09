package com.qreader.reader.ui.widget

import android.content.Context
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.widget.TooltipCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.qreader.reader.R
import com.qreader.reader.databinding.ViewDetailSeekBarBinding
import com.qreader.reader.lib.theme.bottomBackground
import com.qreader.reader.lib.theme.getPrimaryTextColor
import com.qreader.reader.ui.compose.glass.GlassToggleHost
import com.qreader.reader.ui.compose.liquid.LiquidSlider
import com.qreader.reader.utils.ColorUtils

/**
 * 界面面板滑杆：标题 + 减/加 + **LiquidSlider 真液态** + 数值。
 *
 * 玻璃采样源由 [GlassToggleHost] 提供（ReadStyleGlassSheet 组合时 attach）；
 * 无 backdrop 时不渲染滑轨（仅保留 +/- 与数值，避免退回普通 SeekBar 观感）。
 */
class DetailSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private val binding: ViewDetailSeekBarBinding =
        ViewDetailSeekBarBinding.inflate(LayoutInflater.from(context), this, true)
    private val isBottomBackground: Boolean

    private var progressState by mutableIntStateOf(0)
    private var maxState by mutableIntStateOf(0)

    var valueFormat: ((progress: Int) -> String)? = null
    var onChanged: ((progress: Int) -> Unit)? = null

    var progress: Int
        get() = progressState
        set(value) {
            progressState = value.coerceIn(0, maxState)
            upValue()
        }

    var max: Int
        get() = maxState
        set(value) {
            maxState = value.coerceAtLeast(0)
            progressState = progressState.coerceIn(0, maxState)
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
        maxState = typedArray.getInteger(R.styleable.DetailSeekBar_max, 0)
        typedArray.recycle()

        if (isBottomBackground && !isInEditMode) {
            val isLight = ColorUtils.isColorLight(context.bottomBackground)
            val textColor = context.getPrimaryTextColor(isLight)
            binding.tvSeekTitle.setTextColor(textColor)
            binding.ivSeekPlus.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
            binding.ivSeekReduce.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
            binding.tvSeekValue.setTextColor(textColor)
        }

        binding.ivSeekPlus.setOnClickListener {
            progress = progress + 1
            onChanged?.invoke(progress)
        }
        binding.ivSeekReduce.setOnClickListener {
            progress = progress - 1
            onChanged?.invoke(progress)
        }

        if (!isInEditMode) {
            binding.glassSlider.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            binding.glassSlider.setContent {
                val backdrop = GlassToggleHost.backdrop
                val isLight = GlassToggleHost.isLightTheme
                if (backdrop != null) {
                    LiquidSlider(
                        value = { progressState.toFloat() },
                        onValueChange = { p ->
                            progressState = p.toInt().coerceIn(0, maxState)
                            upValue(progressState)
                        },
                        valueRange = 0f..maxState.toFloat().coerceAtLeast(1f),
                        visibilityThreshold = 1f,
                        backdrop = backdrop,
                        onValueChangeFinished = {
                            onChanged?.invoke(progressState)
                        },
                    )
                }
            }
        }
    }

    private fun upValue(progress: Int = progressState) {
        binding.tvSeekValue.text = valueFormat?.invoke(progress) ?: progress.toString()
    }
}
