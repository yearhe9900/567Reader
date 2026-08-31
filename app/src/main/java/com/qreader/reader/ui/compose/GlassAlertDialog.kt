package com.qreader.reader.ui.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment

/**
 * Liquid Glass 风格的确认对话框（居中显示，非全屏）
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
        var checkBoxState by remember { mutableStateOf(checkBoxChecked) }

        // 半透明背景 + 居中对话框
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onCancel?.invoke() },
            contentAlignment = Alignment.Center
        ) {
            // 对话框主体 - 使用简单的玻璃效果（半透明背景）
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* 阻止点击穿透 */ }
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                BasicText(
                    text = title,
                    style = TextStyle(
                        Color.White,
                        20.sp,
                        FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 消息
                BasicText(
                    text = message,
                    style = TextStyle(
                        Color.White.copy(alpha = 0.9f),
                        16.sp,
                        textAlign = TextAlign.Center
                    )
                )

                // 复选框（如果有）
                if (showCheckBox && checkBoxText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { checkBoxState = !checkBoxState }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 简单的复选框
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(24.dp)
                                .background(
                                    if (checkBoxState) Color.White.copy(alpha = 0.6f)
                                    else Color.White.copy(alpha = 0.2f),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicText(
                            text = checkBoxText,
                            style = TextStyle(Color.White.copy(alpha = 0.9f), 16.sp)
                        )
                    }
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
                            .background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(24.dp)
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
                            .background(
                                color = Color.White.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onConfirm?.invoke(checkBoxState) },
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
