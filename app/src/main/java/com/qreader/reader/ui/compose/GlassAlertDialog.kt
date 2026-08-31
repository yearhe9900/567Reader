package com.qreader.reader.ui.compose

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle

/**
 * Liquid Glass 风格的确认对话框（可在传统 Activity 中使用）
 */
class GlassAlertDialog : DialogFragment() {

    private var title: String = ""
    private var message: String = ""
    private var confirmText: String = "确定"
    private var cancelText: String = "取消"
    private var showCheckBox: Boolean = false
    private var checkBoxText: String = ""
    private var checkBoxChecked: Boolean = false
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Translucent_NoTitleBar)
    }

    @Composable
    private fun GlassAlertDialogContent() {
        val backdrop = rememberLayerBackdrop()

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 背景图片
            Image(
                painter = painterResource(id = com.qreader.reader.R.drawable.wallpaper_light),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
                contentScale = ContentScale.Crop
            )

            // 半透明遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onCancel?.invoke() }
            )

            // 对话框
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(24.dp) },
                        effects = {
                            vibrancy()
                            blur(24f.dp.toPx())
                            lens(32f.dp.toPx(), 48f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.3f))
                        }
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 标题
                    BasicText(
                        text = title,
                        style = TextStyle(Color.White, 20.sp, FontWeight.Bold, textAlign = TextAlign.Center)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 消息
                    BasicText(
                        text = message,
                        style = TextStyle(Color.White.copy(alpha = 0.9f), 16.sp, textAlign = TextAlign.Center)
                    )

                    // 复选框（如果有）
                    if (showCheckBox) {
                        Spacer(modifier = Modifier.height(16.dp))
                        // 这里可以添加复选框，但为了简化先省略
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 取消按钮
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { Capsule() },
                                    effects = {
                                        blur(4f.dp.toPx())
                                        lens(8f.dp.toPx(), 12f.dp.toPx())
                                    },
                                    onDrawSurface = {
                                        drawRect(Color.White.copy(alpha = 0.2f))
                                    }
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onCancel?.invoke() },
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                text = cancelText,
                                style = TextStyle(Color.White, 16.sp)
                            )
                        }

                        // 确认按钮
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { Capsule() },
                                    effects = {
                                        blur(4f.dp.toPx())
                                        lens(8f.dp.toPx(), 12f.dp.toPx())
                                    },
                                    onDrawSurface = {
                                        drawRect(Color.White.copy(alpha = 0.4f))
                                    }
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onConfirm?.invoke(checkBoxChecked) },
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                text = confirmText,
                                style = TextStyle(Color.White, 16.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}
