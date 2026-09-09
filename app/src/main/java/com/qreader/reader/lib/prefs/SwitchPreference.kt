package com.qreader.reader.lib.prefs

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import com.kyant.shapes.Capsule
import com.qreader.reader.R
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.GlassToggleHost
import com.qreader.reader.ui.compose.liquid.LiquidToggle

/**
 * 玻璃态 Switch：有 [GlassToggleHost.backdrop] 时用真玻璃 [LiquidToggle]（与 Web 服务开关同款）；
 * 无采样源（普通 Preference 设置页）时回退为同尺寸静态开关，避免空白。
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
            setContent {
                SwitchToggleContent(
                    isChecked = { isChecked },
                    onCheckedChange = { checked ->
                        if (isChecked != checked) {
                            isChecked = checked
                        }
                    },
                    backdrop = GlassToggleHost.backdrop,
                    isLightTheme = GlassToggleHost.isLightTheme,
                )
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

@Composable
private fun SwitchToggleContent(
    isChecked: () -> Boolean,
    onCheckedChange: (Boolean) -> Unit,
    backdrop: com.kyant.backdrop.Backdrop?,
    isLightTheme: Boolean,
) {
    if (backdrop != null) {
        LiquidToggle(
            selected = isChecked,
            onSelect = onCheckedChange,
            backdrop = backdrop,
            isLightTheme = isLightTheme,
        )
    } else {
        FlatToggle(
            selected = isChecked,
            onSelect = onCheckedChange,
            isLightTheme = isLightTheme,
        )
    }
}

/** 无 backdrop 时的静态开关：尺寸/配色仍走 [GlassConfig]，不散落硬编码。 */
@Composable
private fun FlatToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    isLightTheme: Boolean,
) {
    var checked by remember { mutableStateOf(selected()) }
    // 外部 isChecked 变化时同步（RecyclerView 复用）
    androidx.compose.runtime.SideEffect {
        checked = selected()
    }
    val track = if (checked) {
        GlassConfig.toggleAccentColor(isLightTheme)
    } else {
        GlassConfig.toggleTrackColor(isLightTheme)
    }
    val thumbTravel = GlassConfig.toggleWidth - GlassConfig.toggleHeight
    Box(
        modifier = Modifier
            .size(GlassConfig.toggleWidth, GlassConfig.toggleHeight)
            .clip(Capsule())
            .background(track)
            .semantics { role = Role.Switch }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                checked = !checked
                onSelect(checked)
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .graphicsLayer {
                    translationX = if (checked) thumbTravel.toPx() else 0f
                }
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
