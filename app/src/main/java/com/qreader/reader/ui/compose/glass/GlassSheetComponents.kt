package com.qreader.reader.ui.compose.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.qreader.reader.ui.compose.liquid.LiquidSlider

/**
 * 玻璃面板共用滑杆行：标题 | − | LiquidSlider | ＋ | 数值。
 * 仅供 in-tree Compose 面板使用（勿嵌 AndroidView）。
 */
@Composable
fun GlassSliderRow(
    title: String,
    value: Int,
    max: Int,
    contentColor: Color,
    backdrop: Backdrop,
    display: (Int) -> String = { it.toString() },
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = title,
            style = TextStyle(contentColor, 14.sp),
            modifier = Modifier.width(48.dp),
        )
        StepBtn("−", contentColor) { if (value > 0) onChange(value - 1) }
        Spacer(Modifier.width(4.dp))
        Box(
            Modifier
                .weight(1f)
                .height(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            LiquidSlider(
                value = { value.toFloat() },
                onValueChange = { onChange(it.toInt().coerceIn(0, max)) },
                valueRange = 0f..max.toFloat().coerceAtLeast(1f),
                visibilityThreshold = 1f,
                backdrop = backdrop,
                onValueChangeFinished = { onChange(value) },
            )
        }
        Spacer(Modifier.width(4.dp))
        StepBtn("+", contentColor) { if (value < max) onChange(value + 1) }
        Spacer(Modifier.width(4.dp))
        BasicText(
            text = display(value),
            style = TextStyle(contentColor, 14.sp, textAlign = TextAlign.End),
            modifier = Modifier.width(40.dp),
        )
    }
}

@Composable
private fun StepBtn(
    text: String,
    contentColor: Color,
    onClick: () -> Unit,
) {
    BasicText(
        text = text,
        style = TextStyle(contentColor, 18.sp),
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** 玻璃面板开关行。 */
@Composable
fun GlassToggleRow(
    label: String,
    checked: Boolean,
    contentColor: Color,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = label,
            style = TextStyle(contentColor, 14.sp),
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .width(40.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (checked) accent else contentColor.copy(alpha = 0.25f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onCheckedChange(!checked) },
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .padding(3.dp)
                    .width(16.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
            )
        }
    }
}
