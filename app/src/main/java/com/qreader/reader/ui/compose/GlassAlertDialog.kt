package com.qreader.reader.ui.compose

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
import com.qreader.reader.help.config.AppConfig

/**
 * Liquid Glass 风格的确认对话框（官方 drawBackdrop 实现，居中显示）
 * - 背景以「原页面截图」(backdropBitmap) 作为 backdrop 采样源，无截图时回退到 wallpaper_light
 * - 卡片使用 colorControls + blur + lens(depthEffect) 玻璃效果，dim 遮罩仅盖在背景层、不压暗卡片
 * - 支持「保留本地文件」复选框
 * - 取消（点击遮罩或取消按钮）与确认都会触发回调并 dismiss 对话框
 */
class GlassAlertDialog : DialogFragment() {

    private var title: String = ""
    private var message: String = ""
    private var confirmText: String = "确定"
    private var cancelText: String = "取消"
    private var showCheckBox: Boolean = false
    private var checkBoxText: String = ""
    private var checkBoxChecked: Boolean = false
    private var backdropBitmap: Bitmap? = null
    private var onConfirm: ((Boolean) -> Unit)? = null
    private var onCancel: (() -> Unit)? = null

    companion object {
        fun newInstance(
            title: String,
            message: String,
            confirmText: String = "确定",
            cancelText: String = "取消",
            showCheckBox: Boolean = false,
            checkBoxText: String = "",
            checkBoxChecked: Boolean = false,
            backdropBitmap: Bitmap? = null,
            onConfirm: (Boolean) -> Unit,
            onCancel: () -> Unit
        ): GlassAlertDialog {
            return GlassAlertDialog().apply {
                this.title = title
                this.message = message
                this.confirmText = confirmText
                this.cancelText = cancelText
                this.showCheckBox = showCheckBox
                this.checkBoxText = checkBoxText
                this.checkBoxChecked = checkBoxChecked
                this.backdropBitmap = backdropBitmap
                this.onConfirm = onConfirm
                this.onCancel = onCancel
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                GlassAlertDialogContent()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // 玻璃态dialog不受主题模式影响，固定使用浅色主题风格（深色图标）
        // 墨水屏模式下也保持一致
        dialog?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Translucent_NoTitleBar)
    }

    @Composable
    private fun GlassAlertDialogContent() {
        val backdrop = rememberLayerBackdrop()
        var checkBoxState by remember { mutableStateOf(checkBoxChecked) }

        // 玻璃态dialog不受主题模式影响，使用固定的浅色主题颜色（墨水屏模式除外）
        val isEInkMode = AppConfig.isEInkMode
        val contentColor = Color.Black
        val accentColor = Color(0xFF0088FF)
        val containerColor = if (isEInkMode) Color.White else Color(0xFFFAFAFA).copy(0.6f)
        val dimColor = if (isEInkMode) Color.Transparent else Color(0xFF29293A).copy(0.23f)

        // 背景采样源：原页面截图优先，回退到 wallpaper_light
        val backdropPainter: Painter = if (backdropBitmap != null) {
            BitmapPainter(backdropBitmap!!.asImageBitmap())
        } else {
            painterResource(id = R.drawable.wallpaper_light)
        }

        val handleConfirm: () -> Unit = {
            onConfirm?.invoke(checkBoxState)
            dismiss()
        }
        val handleCancel: () -> Unit = {
            onCancel?.invoke()
            dismiss()
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // 1) backdrop 捕获源（与官方 demo 一致：全屏壁纸/截图）+ dim 遮罩
            //    dim 只盖在背景层，卡片作为独立兄弟节点画在其上，不会被压暗
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
                                if (!isEInkMode) {
                                    colorControls(
                                        brightness = 0.2f,
                                        saturation = 1.5f
                                    )
                                    blur(16f.dp.toPx())
                                    lens(24f.dp.toPx(), 48f.dp.toPx(), depthEffect = true)
                                }
                            },
                            highlight = { Highlight.Plain },
                            onDrawSurface = { drawRect(containerColor) }
                        )
                ) {
                    // 标题
                    BasicText(
                        text = title,
                        modifier = Modifier.padding(28f.dp, 24f.dp, 28f.dp, 12f.dp),
                        style = TextStyle(contentColor, 24f.sp, FontWeight.Medium)
                    )

                    // 消息
                    BasicText(
                        text = message,
                        modifier = Modifier.padding(24f.dp, 12f.dp, 24f.dp, 12f.dp),
                        style = TextStyle(contentColor.copy(0.68f), 15f.sp),
                        maxLines = 5
                    )

                    // 复选框（保留本地文件）
                    if (showCheckBox && checkBoxText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { checkBoxState = !checkBoxState }
                                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 玻璃态复选框
                            Box(
                                modifier = Modifier
                                    .size(24f.dp)
                                    .drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { RoundedRectangle(6f.dp) },
                                        effects = {
                                            if (!isEInkMode) {
                                                colorControls(
                                                    brightness = 0.2f,
                                                    saturation = 1.5f
                                                )
                                                blur(8f.dp.toPx())
                                                lens(16f.dp.toPx(), 24f.dp.toPx(), depthEffect = true)
                                            }
                                        },
                                        highlight = { Highlight.Plain },
                                        onDrawSurface = {
                                            drawRect(
                                                if (checkBoxState) accentColor
                                                else if (isEInkMode) containerColor
                                                else containerColor.copy(0.3f)
                                            )
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (checkBoxState) {
                                    BasicText(
                                        text = "✓",
                                        style = TextStyle(Color.White, 16f.sp, FontWeight.Bold)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Box(
                                modifier = Modifier.height(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText(
                                    text = checkBoxText,
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
                                text = cancelText,
                                style = TextStyle(contentColor, 16f.sp)
                            )
                        }

                        // 确认按钮
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
                                text = confirmText,
                                style = TextStyle(Color.White, 16f.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}
