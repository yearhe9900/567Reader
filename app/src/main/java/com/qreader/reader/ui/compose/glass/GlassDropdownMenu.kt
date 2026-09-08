package com.qreader.reader.ui.compose.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/**
 * 通用玻璃态下拉菜单 overlay（原 MainScreen 私有组件，提取为共享组件）。
 *
 * 必须由调用方放在**全屏外层 Box**（与 backdrop 捕获源同 surface），否则 fillMaxSize/tap-outside 会失效。
 * 结构：透明全屏 tap-outside 层（indication=null 无波纹）+ 顶部右对齐玻璃面板。
 *
 * @param topPadding 面板距顶部距离，默认 108.dp（避开 100.dp 高的标题栏）
 * @param panelWidth 面板宽度
 * @param content    面板内容（通常传入若干 [GlassDropdownMenuItem]）
 */
@Composable
fun GlassDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    backdrop: Backdrop,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    topPadding: Dp = 108.dp,
    panelWidth: Dp = 200.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = expanded,
        modifier = modifier.fillMaxSize(),
        enter = fadeIn(tween(160)),
        exit = fadeOut(tween(120))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 透明全屏 tap-outside 层：点击面板外区域触发关闭
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest
                    )
            )
            // 玻璃面板：顶部右对齐
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = topPadding, end = 8.dp)
                    .width(panelWidth)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(16.dp) },
                        effects = {
                            vibrancy()
                            blur(GlassConfig.blur.toPx())
                            lens(GlassConfig.lensX.toPx(), GlassConfig.lensY.toPx())
                        },
                        onDrawSurface = { drawRect(containerColor.copy(alpha = 0.6f)) }
                    )
                    .padding(vertical = 8.dp),
                content = content,
            )
        }
    }
}

/**
 * 玻璃态下拉菜单项：可选图标 + 文字 + 可选勾选标记，整行可点。
 *
 * 不使用 Material3 DropdownMenuItem（其 background/shape 不可玻璃化），
 * 整行通过 [Modifier.clickable] 触发 [onClick]。
 *
 * @param iconRes 图标资源，传 null 则不显示图标（纯文字菜单项）
 * @param checked 传非 null 则在右侧显示勾选标记（对应原 MenuItem 的 checkable 项）
 */
@Composable
fun GlassDropdownMenuItem(
    text: String,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    checked: Boolean? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.75f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        BasicText(
            text = text,
            style = TextStyle(contentColor, 15.sp),
            modifier = Modifier.weight(1f),
        )
        if (checked != null) {
            BasicText(
                text = if (checked) "✓" else "",
                style = TextStyle(contentColor, 15.sp),
            )
        }
    }
}
