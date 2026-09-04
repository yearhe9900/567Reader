package com.qreader.reader.ui.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.qreader.reader.R
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog

/**
 * 「排序」选择弹框（Compose 玻璃态，基于通用 [LiquidGlassDialog]）。
 *
 * 与 [GroupEditOverlay] / 主题弹框同模式：玻璃模糊源来自 [backdrop]（由 MainScreen 的
 * 页面内容 layerBackdrop 提供），因此玻璃采样的是真实书架页，而非弹框自身。
 * 必须由 MainScreen 放在全屏外层 Box 的直接子节点位置渲染——不能放在 BookshelfPage 内部，
 * 否则弹框落在 backdrop 捕获层内部，drawBackdrop 会采样到「包含弹框自身」的层，导致循环捕获崩溃。
 *
 * @param backdrop    玻璃模糊源（书架页内容），由 MainScreen 传入同一 Backdrop 实例
 * @param currentSort 当前排序索引（用于高亮选中项）
 * @param onSelect    选中某个排序项的回调，参数为排序索引
 * @param onDismiss   关闭回调
 */
@Composable
fun SortDialogOverlay(
    backdrop: Backdrop,
    currentSort: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    LiquidGlassDialog(
        backdrop = backdrop,
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .wrapContentHeight(),
        cardRadius = 28.dp,
        contentPadding = PaddingValues(20.dp),
    ) { colors ->
        BasicText(
            text = stringResource(R.string.sort),
            style = TextStyle(color = colors.contentColor, fontSize = 18.sp),
            modifier = Modifier.padding(bottom = 16.dp),
        )
        val sortOptions = listOf(
            R.string.bookshelf_px_0 to 0,
            R.string.bookshelf_px_1 to 1,
            R.string.bookshelf_px_2 to 2,
            R.string.bookshelf_px_3 to 3,
            R.string.bookshelf_px_4 to 4,
            R.string.bookshelf_px_5 to 5,
        )
        sortOptions.forEach { (resId, sortIndex) ->
            val isSelected = currentSort == sortIndex
            TextButton(
                onClick = { onSelect(sortIndex) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                BasicText(
                    text = stringResource(resId),
                    style = TextStyle(
                        color = if (isSelected) colors.accentColor else colors.contentColor,
                        fontSize = 15.sp,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
            }
        }
    }
}
