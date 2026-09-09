package com.qreader.reader.lib.prefs

import android.content.Context
import android.util.AttributeSet
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import com.qreader.reader.R
import com.qreader.reader.ui.compose.glass.GlassToggleHost
import com.qreader.reader.ui.compose.liquid.LiquidToggle

/**
 * 玻璃态 Switch：widget 用 Compose [LiquidToggle]（与「我的」页 Web 服务开关同款）。
 *
 * 玻璃采样源与明暗由 [GlassToggleHost] 提供；无 backdrop 时退回隐藏默认 Switch（不可见占位）。
 */
class SwitchPreference(context: Context, attrs: AttributeSet) :
    SwitchPreferenceCompat(context, attrs) {

    private val isBottomBackground: Boolean
    private var onLongClick: ((preference: SwitchPreference) -> Boolean)? = null

    init {
        layoutResource = R.layout.view_preference
        widgetLayoutResource = R.layout.view_glass_toggle
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.Preference)
        isBottomBackground = typedArray.getBoolean(R.styleable.Preference_isBottomBackground, false)
        typedArray.recycle()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        Preference.bindView<ComposeView>(
            context, holder, icon, title, summary,
            widgetLayoutResource,
            R.id.glass_toggle,
            isBottomBackground = isBottomBackground
        )
        val composeView = holder.findViewById(R.id.glass_toggle) as? ComposeView
        composeView?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            val backdrop = GlassToggleHost.backdrop
            if (backdrop != null) {
                setContent {
                    LiquidToggle(
                        selected = { isChecked },
                        onSelect = { checked ->
                            if (isChecked != checked) {
                                isChecked = checked
                            }
                        },
                        backdrop = backdrop,
                        isLightTheme = GlassToggleHost.isLightTheme,
                    )
                }
            } else {
                // 无玻璃采样源时清空，避免残留上一次 composition
                disposeComposition()
            }
        }
        super.onBindViewHolder(holder)
        onLongClick?.let { listener ->
            holder.itemView.setOnLongClickListener {
                listener.invoke(this)
            }
        }
    }

    fun onLongClick(listener: (preference: SwitchPreference) -> Boolean) {
        onLongClick = listener
    }
}
