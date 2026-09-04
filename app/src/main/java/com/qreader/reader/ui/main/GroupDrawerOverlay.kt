package com.qreader.reader.ui.main

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.qreader.reader.R
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.BookGroup
import com.qreader.reader.ui.book.manage.BookshelfManageActivity
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog

/**
 * 分组抽屉玻璃弹框——从书架页右边缘滑出。
 *
 * 展示全部分组（含系统分组与用户自建分组），宫格排列，可滚动。
 * 点击某个分组跳转到书架管理页（BookshelfManageActivity）并定位到该分组。
 *
 * @param backdrop  玻璃模糊源（由 MainScreen 提供）
 * @param onDismiss 关闭回调
 */
@Composable
fun GroupDrawerOverlay(
    backdrop: Backdrop,
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

        // 标题
        BasicText(
            text = context.getString(R.string.group_manage),
            style = TextStyle(contentColor, 20.sp, FontWeight.Medium),
            modifier = Modifier.padding(bottom = 12.dp),
        )

        // 宫格（3 列，可滚动）
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
                            contentColor = contentColor,
                            containerColor = containerColor,
                            onClick = {
                                onDismiss()
                                context.startActivity(
                                    Intent(context, BookshelfManageActivity::class.java).apply {
                                        putExtra("groupId", group.groupId)
                                    }
                                )
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
    contentColor: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor.copy(alpha = 0.3f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            text = group.getManageName(context),
            style = TextStyle(contentColor.copy(0.9f), 13.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
