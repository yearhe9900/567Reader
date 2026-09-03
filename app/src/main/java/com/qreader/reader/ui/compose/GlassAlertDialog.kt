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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.qreader.reader.ui.compose.glass.GlassDialogTokens
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog

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
        // 玻璃态 dialog 固定深色风格，状态栏/导航栏图标使用浅色；
        // E-Ink 模式下背景为白色，图标改为深色。
        val isEInkMode = AppConfig.isEInkMode
        dialog?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = isEInkMode
                isAppearanceLightNavigationBars = isEInkMode
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

        val isEInkMode = AppConfig.isEInkMode

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
            // 玻璃采样源（截图/壁纸），与官方 demo 一致
            Image(
                painter = backdropPainter,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
                contentScale = ContentScale.Crop
            )

            // 复用通用液态玻璃弹框 LiquidGlassDialog（蒙板 + 圆角 48dp 玻璃卡片）
            LiquidGlassDialog(
                backdrop = backdrop,
                onDismiss = handleCancel,
                modifier = Modifier.fillMaxWidth(0.78f),
                cardRadius = 48.dp,
                contentPadding = PaddingValues(0.dp)
            ) { colors ->
                val contentColor = colors.contentColor
                val accentColor = colors.accentColor
                val containerColor = colors.containerColor

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
                                                brightness = GlassDialogTokens.cardBrightness,
                                                saturation = GlassDialogTokens.cardSaturation
                                            )
                                            blur(GlassDialogTokens.widgetBlur.toPx())
                                            lens(
                                                GlassDialogTokens.widgetLensX.toPx(),
                                                GlassDialogTokens.widgetLensY.toPx(),
                                                depthEffect = true
                                            )
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
                            .padding(horizontal = 16.dp),
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
                            .padding(horizontal = 16.dp),
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
