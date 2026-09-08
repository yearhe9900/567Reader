package com.qreader.reader.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import com.kyant.backdrop.Backdrop
import com.qreader.reader.R
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.BookGroup
import com.qreader.reader.model.BookCover
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * 分组抽屉玻璃弹框——标题栏分组按钮触发。
 *
 * 四格封面形式展示全部分组（取组内最近阅读的 4 本封面，不足补默认封面），点击切换书架当前显示的分组。
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

        // 宫格（3 列，四格封面形式，可滚动）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
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
    // 组内最近阅读的前 4 本 → 四格封面；不足 4 本的缺位由默认封面补齐
    val covers by remember(group.groupId) {
        appDb.bookDao.flowByGroup(group.groupId)
            .map { books ->
                books.sortedByDescending { it.durChapterTime }
                    .take(4)
                    .map { it.getDisplayCover() to it.origin }
            }
            .distinctUntilChanged()
    }.collectAsState(initial = emptyList())

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
        // 四格封面（2×2，组内前 4 本）
        Column(
            modifier = Modifier
                .size(58.dp, 78.dp)
                .clip(RoundedCornerShape(6.dp))
                .then(
                    if (isSelected) Modifier.background(accentColor.copy(0.25f))
                    else Modifier
                ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            repeat(2) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    repeat(2) { col ->
                        val cover = covers.getOrNull(row * 2 + col)
                        GroupCoverCell(
                            coverUrl = cover?.first,
                            sourceOrigin = cover?.second,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
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

/**
 * 分组四格封面中的单个格子：Glide 加载封面，封面为空/加载失败时回落到默认封面。
 */
@Composable
private fun GroupCoverCell(
    coverUrl: String?,
    sourceOrigin: String?,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(R.drawable.image_cover_default)
            }
        },
        update = { view ->
            BookCover.load(view.context, coverUrl, false, sourceOrigin).into(view)
        },
        modifier = modifier.fillMaxSize(),
    )
}
