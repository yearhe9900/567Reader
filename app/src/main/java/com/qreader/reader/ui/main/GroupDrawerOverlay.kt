package com.qreader.reader.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.qreader.reader.R
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.BookGroup
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog

/**
 * 分组抽屉玻璃弹框——标题栏分组按钮触发。
 *
 * 文件夹形式展示全部分组，点击切换书架当前显示的分组。
 *
 * @param backdrop       玻璃模糊源（由 MainScreen 提供）
 * @param currentGroupId 当前选中的分组 ID（用于高亮）
 * @param onSelectGroup  选中分组回调（切换书架数据源）
 * @param onDismiss      关闭回调
 */
@Composable
fun GroupDrawerOverlay(
    backdrop: Backdrop,
    currentGroupId: Long,
    onSelectGroup: (BookGroup) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val groups by appDb.bookGroupDao.flowAll()
        .collectAsState(initial = emptyList())

    LiquidGlassDialog(
        backdrop = backdrop,
        onDismiss = onDismiss,
        modifier = Modifier.fillMaxWidth(0.9f),
        cardRadius = 28.dp,
        contentPadding = PaddingValues(16.dp),
    ) { colors ->
        val contentColor = colors.contentColor
        val containerColor = colors.containerColor
        val accentColor = colors.accentColor

        // 标题
        BasicText(
            text = "分组",
            style = TextStyle(contentColor, 20.sp, FontWeight.Medium),
            modifier = Modifier.padding(bottom = 12.dp),
        )

        // 宫格（3 列，文件夹形式，可滚动）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 350.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            groups.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { group ->
                        GroupGridItem(
                            group = group,
                            isSelected = group.groupId == currentGroupId,
                            contentColor = contentColor,
                            containerColor = containerColor,
                            accentColor = accentColor,
                            onClick = {
                                onSelectGroup(group)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun GroupGridItem(
    group: BookGroup,
    isSelected: Boolean,
    contentColor: Color,
    containerColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) accentColor.copy(0.15f) else containerColor.copy(alpha = 0.3f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 文件夹图标
        Image(
            painter = painterResource(
                if (isSelected) R.drawable.ic_folder_open else R.drawable.ic_folder
            ),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            colorFilter = ColorFilter.tint(
                if (isSelected) accentColor else contentColor.copy(0.6f)
            ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicText(
            text = group.getManageName(context),
            style = TextStyle(
                if (isSelected) accentColor else contentColor.copy(0.9f),
                12.sp,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
