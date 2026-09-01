package com.qreader.reader.ui.main.my

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.qreader.reader.R

/**
 * "主题模式"选择对话框 —— Liquid Glass 风格（DialogFragment）
 *
 * 与删除书籍的 [com.qreader.reader.ui.compose.GlassAlertDialog] 采用**完全相同的机制**：
 * 独立 Window（`STYLE_NO_FRAME` + `Theme_Translucent_NoTitleBar`）+ 由调用方在 Activity
 * 层截取的页面 Bitmap 作为 backdrop 采样源。
 *
 * 这样做的关键原因：DialogFragment 的窗口尺寸 = Activity 窗口尺寸，
 * 截图（`window.decorView`）与弹框内容区坐标系天然一致，`fillMaxSize` 可无缝覆盖，
 * 不会出现 Compose `Dialog` / `Popup` 那种「内容区受 insets 影响 → 与截图尺寸不匹配 →
 * ContentScale.Crop 居中裁剪 → 视觉错位、页面下移、闪动」的问题。
 *
 * 交互：点遮罩外部或"取消"关闭且不提交；点"确定"才回调 [onSelect] 提交选中索引。
 */
class ThemeModeDialogFragment : DialogFragment() {

    private var titleText: String = "主题模式"
    private var labels: Array<String> = emptyArray()
    private var selectedIndex: Int = 0
    private var backdropBitmap: Bitmap? = null
    private var onSelect: ((Int) -> Unit)? = null
    private var onCancel: (() -> Unit)? = null

    companion object {

        private const val ARG_TITLE = "arg_title"
        private const val ARG_LABELS = "arg_labels"
        private const val ARG_SELECTED = "arg_selected"

        fun newInstance(
            title: String = "主题模式",
            labels: Array<String>,
            selectedIndex: Int = 0,
            backdropBitmap: Bitmap? = null,
            onSelect: (Int) -> Unit,
            onCancel: () -> Unit = {}
        ): ThemeModeDialogFragment {
            return ThemeModeDialogFragment().apply {
                // title/labels/selectedIndex 走 arguments，配置变更（旋转）后可恢复
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putStringArray(ARG_LABELS, labels)
                    putInt(ARG_SELECTED, selectedIndex)
                }
                this.backdropBitmap = backdropBitmap
                this.onSelect = onSelect
                this.onCancel = onCancel
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Translucent_NoTitleBar)
        arguments?.let {
            titleText = it.getString(ARG_TITLE) ?: "主题模式"
            labels = it.getStringArray(ARG_LABELS) ?: emptyArray()
            selectedIndex = it.getInt(ARG_SELECTED, 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ThemeModeDialogContent()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // 让弹框窗口的系统栏（状态栏 / 手势栏）图标配色与底层页面主题一致，
        // 避免弹框出现时系统按 Theme_Translucent_NoTitleBar 的默认暗色窗口
        // 把图标翻成白色、弹框消失又翻回，造成"变色"观感。
        val isLight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES
        dialog?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = isLight
                isAppearanceLightNavigationBars = isLight
            }
        }
    }

    @Composable
    private fun ThemeModeDialogContent() {
        val backdrop = rememberLayerBackdrop()
        var currentIndex by remember { mutableStateOf(selectedIndex) }

        val isLightTheme = !isSystemInDarkTheme()
        val contentColor = if (isLightTheme) Color.Black else Color.White
        val accentColor = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
        val containerColor =
            if (isLightTheme) Color(0xFFFAFAFA).copy(0.6f)
            else Color(0xFF121212).copy(0.4f)
        val dimColor =
            if (isLightTheme) Color(0xFF29293A).copy(0.23f)
            else Color(0xFF121212).copy(0.56f)

        // 背景采样源：调用方截取的页面截图优先，回退到 wallpaper_light
        val backdropPainter: Painter = if (backdropBitmap != null) {
            BitmapPainter(backdropBitmap!!.asImageBitmap())
        } else {
            painterResource(id = R.drawable.wallpaper_light)
        }

        val handleConfirm: () -> Unit = {
            onSelect?.invoke(currentIndex)
            dismiss()
        }
        val handleCancel: () -> Unit = {
            onCancel?.invoke()
            dismiss()
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // 1) backdrop 采样源 + dim 遮罩（dim 只盖背景层，卡片不被压暗）
            Image(
                painter = backdropPainter,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    .drawWithContent {
                        drawContent()
                        drawRect(dimColor)
                    },
                contentScale = ContentScale.Crop
            )

            // 2) 居中玻璃卡片层（点外部区域 = 取消）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { handleCancel() },
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
                                    brightness = if (isLightTheme) 0.2f else 0f,
                                    saturation = 1.5f
                                )
                                blur(if (isLightTheme) 16f.dp.toPx() else 8f.dp.toPx())
                                lens(24f.dp.toPx(), 48f.dp.toPx(), depthEffect = true)
                            },
                            highlight = { Highlight.Plain },
                            onDrawSurface = { drawRect(containerColor) }
                        )
                ) {
                    // 标题
                    BasicText(
                        text = titleText,
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
                                                brightness = if (isLightTheme) 0.2f else 0f,
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
                                            .size(8f.dp)
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
                                ) { handleCancel() }
                                .height(48f.dp)
                                .weight(1f)
                                .padding(horizontal = 16f.dp),
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
                                ) { handleConfirm() }
                                .height(48f.dp)
                                .weight(1f)
                                .padding(horizontal = 16f.dp),
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
}
