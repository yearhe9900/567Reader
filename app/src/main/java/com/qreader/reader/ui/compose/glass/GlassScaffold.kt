package com.qreader.reader.ui.compose.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.qreader.reader.R

/**
 * 全 App 统一的 Compose 玻璃顶栏（View 体系 `TitleBar` 的 Compose 对应物）。
 *
 * 各已 Compose 化的页面此前各自复制了一份 `drawBackdrop + statusBarsPadding + height`
 * 样板（BookInfoScreen / TocGlassSheet / SearchScreen）。本文件是它们的收敛点：
 * 后续新页面一律用这里，不要再抄样板。
 *
 * ── 使用前提（务必遵守，否则崩溃）──
 * [backdrop] 必须来自**捕获层之外**。标准结构是：
 * ```
 * Box(Modifier.fillMaxSize()) {
 *     // 捕获层：背景内容，供顶栏玻璃采样
 *     Column(Modifier.fillMaxSize().layerBackdrop(backdrop)) { ...内容... }
 *     // 顶栏必须在捕获层之外（即上面这个 Column 的兄弟节点）
 *     GlassTopBar(title = "xx", backdrop = backdrop, onBack = { finish() })
 * }
 * ```
 * 把 [GlassTopBar] 放进 `layerBackdrop` 的子树里会**循环捕获，直接崩溃**。
 *
 * ── 尺寸约定 ──
 * 玻璃自身高度 = [GlassConfig.titleBarHeight]（56dp），**外加**状态栏高度，
 * 因此玻璃会一直延伸到屏幕顶边、把状态栏一起罩住，与状态栏连成一片（全 App 观感统一）。
 * 内容靠 [statusBarsPadding] 下移，不会顶到状态栏。
 *
 * 调用方若使用本组件，内容区需自行留出 `titleBarHeight + statusBars` 的顶部空间
 * （例如 `.padding(top = GlassConfig.titleBarHeight)` 再配合 `statusBarsPadding()`），
 * 因为本组件是**悬浮**在内容之上的。
 */
@Composable
fun GlassTopBar(
    title: String,
    backdrop: Backdrop,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isLightTheme: Boolean = GlassConfig.isLightTheme(LocalContext.current),
    /** 右侧动作区。用 [GlassTopBarIcon] 构造图标按钮，直接摆放即可。 */
    actions: @Composable (rowScope: RowScope) -> Unit = {},
) {
    val containerColor = GlassConfig.containerColor(isLightTheme)
    val contentColor = GlassConfig.contentColor(isLightTheme)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(0.dp) },
                effects = {
                    vibrancy()
                    blur(GlassConfig.blur.toPx())
                    lens(GlassConfig.lensX.toPx(), GlassConfig.lensY.toPx())
                },
                onDrawSurface = { drawRect(containerColor) },
            )
            .height(GlassConfig.titleBarHeight)
            .statusBarsPadding()
            .padding(start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            GlassTopBarIcon(
                iconRes = R.drawable.ic_arrow_back,
                contentDescription = "返回",
                contentColor = contentColor,
                onClick = onBack,
            )
        }
        BasicText(
            text = title,
            style = TextStyle(
                color = contentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (onBack != null) 8.dp else 12.dp),
        )
        actions(this)
    }
}

/**
 * 顶栏图标按钮：裸图标 + 40dp 触摸区，**不加**玻璃框。
 *
 * 顶栏本身已经是一整块玻璃，按钮再套一层 40dp 玻璃块会在玻璃上叠出可见的方块边界
 * （与目录页顶栏 `TocTopBar` 的处理保持一致）。
 */
@Composable
fun GlassTopBarIcon(
    iconRes: Int,
    contentDescription: String?,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = contentDescription,
        tint = contentColor,
        modifier = modifier
            .size(40.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(8.dp),
    )
}

/**
 * 悬浮型玻璃顶栏需要给内容预留的顶部高度（不含状态栏）。
 *
 * 用法：内容区 `.padding(top = GlassTopBarReservedHeight).statusBarsPadding()`。
 * 提出来是为了避免各页面硬编码，以及将来 [GlassConfig.titleBarHeight] 变化时多处不同步。
 */
val GlassTopBarReservedHeight = GlassConfig.titleBarHeight
