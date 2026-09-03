package com.qreader.reader.ui.main.my

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import com.qreader.reader.lib.theme.backgroundColor
import com.qreader.reader.utils.ColorUtils
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qreader.reader.R
import com.qreader.reader.constant.PreferKey
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle

/**
 * 「我的」设置页 —— Compose 实现
 *
 * 参考系统设置风格：纯色背景 + 大圆角分组卡片。
 * 页面由多个分组卡片组成，每个卡片内包含若干设置项；
 * 设置项分为两类：
 *  - [SettingItem.Action]：可点击跳转的条目（如"书源管理"），右侧带箭头图标；
 *  - [SettingItem.Toggle]：可切换开关的条目（如"Web 服务"），右侧为 Switch。
 *
 * 本页为纯展示/交互层，不持有任何状态，所有数据均由上层传入，
 * 交互通过回调函数向上层通知，符合单向数据流的设计。
 *
 * 注意：「主题模式」弹框在本页内以 Compose 覆盖层弹出（同 Compose surface，
 * 不是独立 Window 的 DialogFragment），玻璃模糊源来自本页内容自身（页面 Column
 * 包 `layerBackdrop`），因此玻璃采样的是真实设置页，无需截图、也不跨窗口。
 * 蒙板与卡片均为真·毛玻璃。
 *
 * @param webServiceChecked  Web 服务开关的当前状态
 * @param webServiceSummary  Web 服务设置项的摘要文案（用于展示连接状态等说明）
 * @param themeModeIndex     当前主题模式索引（用于在设置项右侧展示当前值）
 * @param onActionClick      点击 Action 类设置项时的回调，参数为设置项 key
 * @param onWebServiceToggle Web 服务开关切换时的回调，参数为新的开关状态
 * @param onThemeModeSelected 点击「主题模式」条目选中并确认后的回调，参数为选中的模式索引
 * @param themeDialogOpen    主题模式弹框是否打开（由上层持有，用于隐藏底部导航栏）
 * @param onThemeDialogOpenChange 主题模式弹框打开状态变化回调（开/关时上抛）
 * @param modifier           外部传入的 Modifier
 */
@Composable
fun MySettingsScreen(
    webServiceChecked: Boolean,
    webServiceSummary: String,
    themeModeIndex: Int,
    onActionClick: (String) -> Unit,
    onWebServiceToggle: (Boolean) -> Unit,
    onThemeModeSelected: (Int) -> Unit,
    themeDialogOpen: Boolean,
    onThemeDialogOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 根据系统深色/浅色模式动态切换配色：
    // 浅色：页面背景 #F2F2F7（iOS 风格浅灰），卡片为白色；
    // 深色：页面背景为纯黑，卡片为 #1C1C1E（深灰）。
    val isLightTheme = !isSystemInDarkTheme()
    val pageBackgroundColor = if (isLightTheme) Color(0xFFF2F2F7) else Color.Black
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val containerColor = if (isLightTheme) Color.White else Color(0xFF1C1C1E)
    val iconTint = contentColor.copy(0.7f)

    // 主题模式的可选文案列表（从资源 R.array.theme_mode 读取，如"跟随系统/浅色/深色"）
    val themeModeLabels = context.resources.getStringArray(R.array.theme_mode)

    // 主题模式弹框（同 Compose surface 覆盖层，玻璃采样真实设置页，无截图）
    val backdrop = rememberLayerBackdrop()

    // 根据传入的开关状态、摘要文案与主题索引构建分组数据；
    // 当任一依赖变化时自动重建，保证 UI 与数据同步。
    val categories = remember(
        webServiceChecked,
        webServiceSummary,
        themeModeIndex
    ) {
        buildSettingCategories(
            context = context,
            themeModeLabels = themeModeLabels,
            themeModeIndex = themeModeIndex,
            webServiceChecked = webServiceChecked,
            webServiceSummary = webServiceSummary
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 页面内容：包在 layerBackdrop 内，作为主题弹框玻璃的模糊源。
        // 仅弹框打开时启用 layerBackdrop，避免常驻 GPU 离屏开销。
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBackgroundColor)
                .then(if (themeDialogOpen) Modifier.layerBackdrop(backdrop) else Modifier)
        ) {
            // 可滚动内容容器（卡片之间 16dp 间距，靠卡片本身分组，无分类标题）
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    // 底部 88dp 为悬浮玻璃导航栏预留（导航栏 72dp + 16dp 余量），
                    // 保证滚到底时最后一项不被导航栏遮挡
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp)
            ) {
                categories.forEachIndexed { index, category ->
                    // 仅卡片之间加 16dp，第一个卡片之前由 Column top=12dp 给出呼吸感
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 圆角卡片（大圆角、纯色背景、无边框）
                    GlassSettingsCard(containerColor = containerColor) {
                        category.items.forEach { item ->
                            when (item) {
                                is SettingItem.Action -> ActionRow(
                                    item = item,
                                    contentColor = contentColor,
                                    iconTint = iconTint,
                                    onClick = {
                                        if (item.key == PreferKey.themeMode) {
                                            // 主题模式：在本页内弹出玻璃覆盖层（采样真实设置页）
                                            onThemeDialogOpenChange(true)
                                        } else {
                                            onActionClick(item.key)
                                        }
                                    }
                                )

                                is SettingItem.Toggle -> ToggleRow(
                                    item = item,
                                    contentColor = contentColor,
                                    iconTint = iconTint,
                                    onCheckedChange = { checked ->
                                        if (item.key == PreferKey.webService) {
                                            onWebServiceToggle(checked)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 主题模式弹框覆盖层（真·毛玻璃：玻璃采样真实设置页，无截图）
        AnimatedVisibility(
            visible = themeDialogOpen,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(120))
        ) {
            ThemeModeDialogOverlay(
                backdrop = backdrop,
                labels = themeModeLabels,
                initialIndex = themeModeIndex,
                onConfirm = { index ->
                    onThemeModeSelected(index)
                    onThemeDialogOpenChange(false)
                },
                onDismiss = { onThemeDialogOpenChange(false) }
            )
        }
    }

}

/**
 * 「主题模式」选择弹框（同 surface 玻璃覆盖层）
 *
 * 由 [MySettingsScreen] 在本页内弹出：玻璃模糊源来自页面内容自身（[backdrop]，
 * 由页面 Column 包 `layerBackdrop` 提供），因此玻璃采样的是真实设置页，
 * 无需截图、也不跨窗口。蒙板与卡片均为真·毛玻璃。
 *
 * @param backdrop      玻璃模糊源（页面内容），由宿主传入同一 [Backdrop] 实例
 * @param labels        主题模式候选文案（"跟随系统/浅色/深色"）
 * @param initialIndex  当前选中索引
 * @param onConfirm     点击「确定」回调，参数为选中的索引
 * @param onDismiss     点击外部 / 「取消」关闭回调
 */
@Composable
private fun ThemeModeDialogOverlay(
    backdrop: Backdrop,
    labels: Array<String>,
    initialIndex: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(initialIndex) }

    // 主题弹框样式固定（不随明暗主题变化）：统一采用深色模式一套值
    val contentColor = Color.White
    val accentColor = Color(0xFF0091FF)
    val containerColor = Color(0xFF121212).copy(0.4f)
    val dimColor = Color(0xFF121212).copy(0.56f)
    // 蒙板染色：用页面底色轻微染色，使毛玻璃更有"材质感"
    val scrimColor = Color.Black.copy(0.5f)

    Box(modifier = Modifier.fillMaxSize()) {
        // 1) 蒙板（真·毛玻璃）：模糊真实设置页 + 页面色染色 + dim 压暗
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(0.5f.dp) },
                    effects = {
                        colorControls(
                            brightness = 0f,
                            saturation = 1.4f
                        )
                        blur(12f.dp.toPx())
                        lens(28f.dp.toPx(), 56f.dp.toPx(), depthEffect = true)
                    },
                    highlight = { Highlight.Plain },
                    onDrawSurface = { drawRect(scrimColor) }
                )
                .drawWithContent {
                    drawContent()
                    drawRect(dimColor)
                }
        )

        // 2) 居中玻璃卡片层（点外部区域 = 取消）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* 点卡片内部不关闭 */ }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(48f.dp) },
                        effects = {
                            colorControls(
                                brightness = 0f,
                                saturation = 1.5f
                            )
                            blur(8f.dp.toPx())
                            lens(24f.dp.toPx(), 48f.dp.toPx(), depthEffect = true)
                        },
                        highlight = { Highlight.Plain },
                        onDrawSurface = { drawRect(containerColor) }
                    )
            ) {
                // 标题
                BasicText(
                    text = stringResource(R.string.theme_mode),
                    modifier = Modifier.padding(28f.dp, 24f.dp, 28f.dp, 12f.dp),
                    style = TextStyle(contentColor, 24f.sp, FontWeight.Medium)
                )

                // 单选列表（玻璃态圆形单选圈）
                labels.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { currentIndex = index }
                            .padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24f.dp)
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { Capsule() },
                                    effects = {
                                        colorControls(
                                            brightness = 0f,
                                            saturation = 1.5f
                                        )
                                        blur(8f.dp.toPx())
                                        lens(16f.dp.toPx(), 24f.dp.toPx(), depthEffect = true)
                                    },
                                    highlight = { Highlight.Plain },
                                    onDrawSurface = {
                                        drawRect(
                                            if (index == currentIndex) accentColor
                                            else containerColor.copy(0.3f)
                                        )
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index == currentIndex) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(Capsule())
                                        .background(Color.White)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier.height(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                text = label,
                                style = TextStyle(contentColor.copy(0.9f), 16f.sp)
                            )
                        }
                    }
                }

                // 按钮
                Row(
                    modifier = Modifier
                        .padding(24f.dp, 16f.dp, 24f.dp, 24f.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16f.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 取消按钮
                    Row(
                        modifier = Modifier
                            .clip(Capsule())
                            .background(containerColor.copy(0.2f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onDismiss() }
                            .height(48f.dp)
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(
                            4f.dp,
                            Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            text = "取消",
                            style = TextStyle(contentColor, 16f.sp)
                        )
                    }

                    // 确定按钮
                    Row(
                        modifier = Modifier
                            .clip(Capsule())
                            .background(accentColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onConfirm(currentIndex) }
                            .height(48f.dp)
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(
                            4f.dp,
                            Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            text = "确定",
                            style = TextStyle(Color.White, 16f.sp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 圆角分组卡片容器
 *
 * 模拟 iOS/系统设置的分组卡片样式：大圆角（28dp）纯色背景、无边框、上下留白 8dp。
 * 每个分类（如"管理"、"设置"）使用一张卡片承载其中的设置项列表。
 *
 * @param containerColor 卡片背景色（浅色为白色，深色为 #1C1C1E）
 * @param content        卡片内部的组合内容（通常为多个设置行）
 */
@Composable
private fun GlassSettingsCard(
    containerColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(28.dp))
            .padding(vertical = 4.dp),
        content = content
    )
}

/**
 * 可点击的设置行（Action 类条目）
 *
 * 布局从左到右依次为：图标 → 标题/摘要（占满剩余宽度）→ 可选当前值 → 右箭头"›"。
 * 整行可点击，点击事件由 onClick 回调抛出，具体跳转逻辑由上层根据 key 分发。
 *
 * @param item        设置项数据（含 key、图标、标题、摘要、当前值）
 * @param contentColor 文字与图标主色调
 * @param iconTint    图标着色（透明度略低的文字色）
 * @param onClick     点击整行时触发的回调
 */
@Composable
private fun ActionRow(
    item: SettingItem.Action,
    contentColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            // 使用无涟漪效果（indication = null）的点击，模拟 iOS 简约风格
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧图标，统一用 iconTint 着色（contentDescription 为 null，装饰性图标无需读屏描述）
        Image(
            painter = painterResource(id = item.icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(iconTint)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // 中间区域：标题 + 可选摘要，占据剩余宽度
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = item.title,
                style = TextStyle(contentColor, 16.sp)
            )
            item.summary?.let {
                BasicText(
                    text = it,
                    style = TextStyle(contentColor.copy(0.55f), 13.sp)
                )
            }
        }

        // 右侧：当前值（如主题模式当前选项），仅在存在时显示
        item.value?.let {
            BasicText(
                text = it,
                style = TextStyle(contentColor.copy(0.55f), 14.sp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        // 右侧：进入箭头"›"，暗示该行可点击跳转
        BasicText(
            text = "›",
            style = TextStyle(contentColor.copy(0.35f), 20.sp)
        )
    }
}

/**
 * 带开关的设置行（Toggle 类条目）
 *
 * 布局从左到右依次为：图标 → 标题/摘要（占满剩余宽度）→ Switch 开关。
 * 开关状态由上层通过 item.checked 传入，切换时通过 onCheckedChange 回调上报。
 *
 * @param item            设置项数据（含 key、图标、标题、摘要、开关状态）
 * @param contentColor    文字与图标主色调
 * @param iconTint        图标着色
 * @param onCheckedChange 开关切换时的回调，参数为新的布尔状态
 */
@Composable
private fun ToggleRow(
    item: SettingItem.Toggle,
    contentColor: Color,
    iconTint: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧图标，统一着色
        Image(
            painter = painterResource(id = item.icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(iconTint)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // 中间区域：标题 + 可选摘要
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = item.title,
                style = TextStyle(contentColor, 16.sp)
            )
            item.summary?.let {
                BasicText(
                    text = it,
                    style = TextStyle(contentColor.copy(0.55f), 13.sp)
                )
            }
        }

        // 右侧：Material3 开关，自定义为"白色滑块 + 蓝色轨道"以贴合整体风格
        Switch(
            checked = item.checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF0088FF),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = contentColor.copy(0.25f)
            )
        )
    }
}

/**
 * 设置项数据模型（sealed class）
 *
 * 定义了页面中所有设置条目的统一结构，所有设置项都包含：
 *  - key：唯一标识，用于上层分发点击/切换事件；
 *  - icon：图标资源 id；
 *  - title：主标题文案；
 *  - summary：可选摘要说明文案。
 *
 * 具体分为两种类型：
 *  - [Action]：可点击跳转的条目（可额外携带 value 展示当前值）；
 *  - [Toggle]：可开关的条目（携带 checked 表示当前开关状态）。
 */
private sealed class SettingItem {
    abstract val key: String
    abstract val icon: Int
    abstract val title: String
    abstract val summary: String?

    /** 可点击跳转的设置项，value 用于在右侧展示当前值（如当前主题模式名） */
    data class Action(
        override val key: String,
        override val icon: Int,
        override val title: String,
        override val summary: String? = null,
        val value: String? = null
    ) : SettingItem()

    /** 可切换开关的设置项，checked 表示当前开关状态 */
    data class Toggle(
        override val key: String,
        override val icon: Int,
        override val title: String,
        override val summary: String? = null,
        val checked: Boolean
    ) : SettingItem()
}

/**
 * 设置分组
 *
 * 一组设置项构成一个分类，渲染时每个分类对应一张圆角卡片。
 *
 * @param title 分组名称（"管理"、"外观与服务"、"设置"、"工具"、"关于"）
 * @param items 该分组下的设置项列表
 */
private data class SettingCategory(
    val title: String,
    val items: List<SettingItem>
)

/**
 * 构建设置页面的全部分组数据
 *
 * 将页面划分为 5 个分组：管理、外观与服务、设置、工具、关于。
 * 所有标题/摘要文案优先从资源文件读取以支持多语言；
 * 需要动态展示的值（主题模式当前选项、Web 服务状态）作为参数传入。
 *
 * @param context          用于读取资源文案（getString）
 * @param themeModeLabels  主题模式候选文案列表
 * @param themeModeIndex   当前主题模式索引，用于在"主题模式"条目右侧展示当前值
 * @param webServiceChecked Web 服务开关当前状态
 * @param webServiceSummary Web 服务条目摘要（展示连接状态等说明）
 * @return 按页面展示顺序排列的分组列表
 */
private fun buildSettingCategories(
    context: android.content.Context,
    themeModeLabels: Array<String>,
    themeModeIndex: Int,
    webServiceChecked: Boolean,
    webServiceSummary: String
): List<SettingCategory> {
    return listOf(
        // 分组一：管理 —— 书源、TXT 目录规则、净化替换、词典规则
        SettingCategory(
            title = "管理",
            items = listOf(
                SettingItem.Action(
                    key = "bookSourceManage",
                    icon = R.drawable.ic_cfg_source,
                    title = context.getString(R.string.book_source_manage),
                    summary = context.getString(R.string.book_source_manage_desc)
                ),
                SettingItem.Action(
                    key = "txtTocRuleManage",
                    icon = R.drawable.ic_cfg_source,
                    title = context.getString(R.string.txt_toc_rule),
                    summary = context.getString(R.string.config_txt_toc_rule)
                ),
                SettingItem.Action(
                    key = "replaceManage",
                    icon = R.drawable.ic_cfg_replace,
                    title = context.getString(R.string.replace_purify),
                    summary = context.getString(R.string.replace_purify_desc)
                ),
                SettingItem.Action(
                    key = "dictRuleManage",
                    icon = R.drawable.ic_translate,
                    title = context.getString(R.string.dict_rule),
                    summary = context.getString(R.string.config_dict_rule)
                )
            )
        ),

        // 分组二：外观与服务 —— 主题模式（点击弹窗选择）、Web 服务（开关切换）
        SettingCategory(
            title = "外观与服务",
            items = listOf(
                // 主题模式：点击后在 ThemeModeDialog 中选择，右侧展示当前模式名称
                SettingItem.Action(
                    key = PreferKey.themeMode,
                    icon = R.drawable.ic_cfg_theme,
                    title = context.getString(R.string.theme_mode),
                    summary = context.getString(R.string.theme_mode_desc),
                    value = themeModeLabels.getOrNull(themeModeIndex)
                ),
                // Web 服务：开关切换，摘要由上层根据连接状态动态提供
                SettingItem.Toggle(
                    key = PreferKey.webService,
                    icon = R.drawable.ic_cfg_web,
                    title = context.getString(R.string.web_service),
                    summary = webServiceSummary,
                    checked = webServiceChecked
                )
            )
        ),

        // 分组三：设置 —— 备份与恢复、主题、其他设置
        SettingCategory(
            title = "设置",
            items = listOf(
                SettingItem.Action(
                    key = "web_dav_setting",
                    icon = R.drawable.ic_cfg_backup,
                    title = context.getString(R.string.backup_restore),
                    summary = context.getString(R.string.web_dav_set_import_old)
                ),
                SettingItem.Action(
                    key = "theme_setting",
                    icon = R.drawable.ic_cfg_theme,
                    title = context.getString(R.string.theme_setting),
                    summary = context.getString(R.string.theme_setting_s)
                ),
                SettingItem.Action(
                    key = "setting",
                    icon = R.drawable.ic_cfg_other,
                    title = context.getString(R.string.other_setting),
                    summary = context.getString(R.string.other_setting_s)
                )
            )
        ),

        // 分组四：工具 —— 书签、阅读记录、文件管理
        SettingCategory(
            title = "工具",
            items = listOf(
                SettingItem.Action(
                    key = "bookmark",
                    icon = R.drawable.ic_bookmark,
                    title = context.getString(R.string.bookmark),
                    summary = context.getString(R.string.all_bookmark)
                ),
                SettingItem.Action(
                    key = "readRecord",
                    icon = R.drawable.ic_history,
                    title = context.getString(R.string.read_record),
                    summary = context.getString(R.string.read_record_summary)
                ),
                SettingItem.Action(
                    key = "fileManage",
                    icon = R.drawable.ic_folder_outline,
                    title = context.getString(R.string.file_manage),
                    summary = context.getString(R.string.file_manage_summary)
                )
            )
        ),

        // 分组五：关于 —— 应用版本信息、退出应用
        SettingCategory(
            title = "关于",
            items = listOf(
                SettingItem.Action(
                    key = "appVersion",
                    icon = R.drawable.ic_cfg_about,
                    title = "系统版本",
                    summary = "当前应用版本"
                ),
                SettingItem.Action(
                    key = "exit",
                    icon = R.drawable.ic_exit,
                    title = context.getString(R.string.exit)
                )
            )
        )
    )
}
