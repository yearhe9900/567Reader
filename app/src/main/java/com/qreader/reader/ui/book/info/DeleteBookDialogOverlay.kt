package com.qreader.reader.ui.book.info

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.Capsule
import com.qreader.reader.R
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog

/**
 * 删除书籍确认弹框（Compose 玻璃态，基于 [LiquidGlassDialog]）。
 *
 * 视觉风格与排序弹框（SortDialogOverlay）完全对齐：
 * 窄卡片（0.78f）、大圆角（48dp）、玻璃态确认/取消按钮。
 *
 * @param backdrop         玻璃模糊源
 * @param showCheckBox     是否显示复选框（本地书籍时显示）
 * @param checkBoxText     复选框文案
 * @param checkBoxChecked  复选框初始状态
 * @param onConfirm        确认回调，参数为复选框状态
 * @param onCancel         取消回调
 */
@Composable
fun DeleteBookDialogOverlay(
    backdrop: Backdrop,
    showCheckBox: Boolean = false,
    checkBoxText: String = "",
    checkBoxChecked: Boolean = false,
    onConfirm: (Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    var isChecked by remember { mutableStateOf(checkBoxChecked) }

    LiquidGlassDialog(
        backdrop = backdrop,
        onDismiss = onCancel,
        modifier = Modifier.fillMaxWidth(0.78f),
        cardRadius = 48.dp,
        contentPadding = PaddingValues(0.dp),
    ) { colors ->
        val contentColor = colors.contentColor
        val accentColor = colors.accentColor

        // 标题
        BasicText(
            text = stringResource(R.string.draw),
            modifier = Modifier.padding(start = 28.dp, top = 24.dp, end = 28.dp, bottom = 8.dp),
            style = TextStyle(contentColor, 24.sp, FontWeight.Medium),
        )

        // 提示信息
        BasicText(
            text = stringResource(R.string.sure_del),
            modifier = Modifier.padding(start = 28.dp, end = 28.dp, bottom = 16.dp),
            style = TextStyle(contentColor.copy(0.8f), 16.sp),
        )

        // 复选框（本地书籍时显示）
        if (showCheckBox && checkBoxText.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { isChecked = !isChecked }
                    .padding(start = 20.dp, end = 28.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { isChecked = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = accentColor,
                        uncheckedColor = contentColor.copy(0.5f),
                    ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicText(
                    text = checkBoxText,
                    style = TextStyle(contentColor.copy(0.9f), 14.sp),
                )
            }
        }

        // 按钮行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 取消按钮
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(Capsule())
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCancel,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = stringResource(R.string.cancel),
                    style = TextStyle(contentColor, 16.sp),
                )
            }

            // 确认按钮
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(Capsule())
                    .background(accentColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onConfirm(isChecked) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = stringResource(R.string.ok),
                    style = TextStyle(Color.White, 16.sp, FontWeight.Medium),
                )
            }
        }
    }
}
