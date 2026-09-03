package com.qreader.reader.ui.book.group

import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.qreader.reader.R
import com.qreader.reader.data.entities.BookGroup
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.ui.compose.glass.GlassDialogTokens
import com.qreader.reader.ui.compose.glass.glassDialogColors
import com.qreader.reader.ui.file.HandleFileContract
import com.qreader.reader.ui.widget.image.CoverImageView
import com.qreader.reader.utils.FileUtils
import com.qreader.reader.utils.MD5Utils
import com.qreader.reader.utils.externalFiles
import com.qreader.reader.utils.inputStream
import com.qreader.reader.utils.readUri
import com.qreader.reader.utils.toastOnUi
import splitties.init.appCtx

/**
 * 编辑 / 添加分组弹框（Compose 玻璃态，对标「主题模式」弹框）。
 *
 * 仅用于书架页长按分组入口；其余 5 个 XML 弹框入口仍走原 [GroupEditDialog]。
 * 玻璃模糊源来自 [backdrop]（由 MainScreen 的页面内容 layerBackdrop 提供），
 * 因此玻璃采样的是真实书架页，与主题弹框一致。蒙板 + 卡片均为真·毛玻璃。
 *
 * @param backdrop  玻璃模糊源（书架页内容），由 MainScreen 传入同一 Backdrop 实例
 * @param target    要编辑的分组；null 表示添加新分组
 * @param onDismiss 关闭回调
 */
@Composable
fun GroupEditOverlay(
    backdrop: Backdrop,
    target: BookGroup?,
    onDismiss: () -> Unit
) {
    val activity = LocalContext.current as AppCompatActivity
    val context = LocalContext.current
    val isEInkMode = AppConfig.isEInkMode
    val colors = glassDialogColors(isEInkMode)
    val contentColor = colors.contentColor
    val accentColor = colors.accentColor
    val containerColor = colors.containerColor
    val dimColor = colors.dimColor
    val scrimColor = colors.scrimColor

    val isEdit = target != null
    var name by remember { mutableStateOf(target?.groupName ?: "") }
    var sortIndex by remember {
        mutableStateOf(
            run {
                val idx = (target?.bookSort ?: -1) + 1
                val opts = context.resources.getStringArray(R.array.book_sort)
                if (idx in opts.indices) idx else 0
            }
        )
    }
    var enableRefresh by remember { mutableStateOf(target?.enableRefresh ?: true) }
    var onlyUpdateRead by remember { mutableStateOf(target?.onlyUpdateRead ?: false) }
    var coverPath by remember { mutableStateOf(target?.cover) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCoverMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val sortOptions = remember { context.resources.getStringArray(R.array.book_sort) }
    val coverViewRef = remember { mutableStateOf<CoverImageView?>(null) }

    val groupViewModel = remember { ViewModelProvider(activity)[GroupViewModel::class.java] }

    val pickLauncher = rememberLauncherForActivityResult(HandleFileContract()) { result ->
        val uri = result.uri ?: return@rememberLauncherForActivityResult
        val cv = coverViewRef.value ?: return@rememberLauncherForActivityResult
        if (uri.scheme?.lowercase() in listOf("http", "https")) {
            cv.load(uri.toString())
            coverPath = cv.bitmapPath
        } else {
            activity.readUri(uri) { fileDoc, inputStream ->
                try {
                    val suffix = if (fileDoc.name.contains(".9.png", true)) {
                        ".9.png"
                    } else {
                        "." + fileDoc.name.substringAfterLast(".")
                    }
                    val fileName = uri.inputStream(context).getOrThrow().use { tmp ->
                        MD5Utils.md5Encode(tmp) + suffix
                    }
                    val file = FileUtils.createFileIfNotExist(context.externalFiles, "covers", fileName)
                    file.outputStream().use { out -> inputStream.copyTo(out) }
                    cv.load(file.absolutePath)
                    coverPath = cv.bitmapPath
                } catch (e: Exception) {
                    appCtx.toastOnUi(e.localizedMessage)
                }
            }
        }
    }

    // 封面变化时刷新 CoverImageView（仅在变化时，避免每次重组重载）
    LaunchedEffect(coverPath) {
        coverViewRef.value?.load(coverPath)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1) 蒙板（真·毛玻璃）：模糊真实书架页 + 页面色染色 + dim 压暗
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(0.5f.dp) },
                    effects = {
                        if (!isEInkMode) {
                            colorControls(
                                brightness = GlassDialogTokens.scrimBrightness,
                                saturation = GlassDialogTokens.scrimSaturation
                            )
                            blur(GlassDialogTokens.scrimBlur.toPx())
                            lens(
                                GlassDialogTokens.scrimLensX.toPx(),
                                GlassDialogTokens.scrimLensY.toPx(),
                                depthEffect = true
                            )
                        }
                    },
                    highlight = { Highlight.Plain },
                    onDrawSurface = { drawRect(scrimColor) }
                )
                .drawWithContent {
                    drawContent()
                    drawRect(dimColor)
                }
        )

        // 2) 居中玻璃卡片（点外部区域 = 取消）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .verticalScroll(rememberScrollState())
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* 点卡片内部不关闭 */ }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(28f.dp) },
                        effects = {
                            if (!isEInkMode) {
                                colorControls(
                                    brightness = GlassDialogTokens.cardBrightness,
                                    saturation = GlassDialogTokens.cardSaturation
                                )
                                blur(GlassDialogTokens.cardBlur.toPx())
                                lens(
                                    GlassDialogTokens.cardLensX.toPx(),
                                    GlassDialogTokens.cardLensY.toPx(),
                                    depthEffect = true
                                )
                            }
                        },
                        highlight = { Highlight.Plain },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .padding(20.dp)
            ) {
                // 标题
                BasicText(
                    text = stringResource(if (isEdit) R.string.group_edit else R.string.add_group),
                    modifier = Modifier.padding(bottom = 14.dp),
                    style = TextStyle(contentColor, 20.sp, FontWeight.Medium)
                )

                // 封面 + 名称 + 排序 + 勾选
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp, 126.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!coverPath.isNullOrEmpty()) {
                                    showCoverMenu = true
                                } else {
                                    pickLauncher.launch { mode = HandleFileContract.IMAGE }
                                }
                            }
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                CoverImageView(ctx).also {
                                    coverViewRef.value = it
                                    it.load(coverPath)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        DropdownMenu(
                            expanded = showCoverMenu,
                            onDismissRequest = { showCoverMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { BasicText(stringResource(R.string.select_image)) },
                                onClick = {
                                    showCoverMenu = false
                                    pickLauncher.launch { mode = HandleFileContract.IMAGE }
                                }
                            )
                            DropdownMenuItem(
                                text = { BasicText(stringResource(R.string.delete)) },
                                onClick = {
                                    showCoverMenu = false
                                    coverViewRef.value?.load()
                                    coverPath = null
                                }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .weight(1f)
                    ) {
                        // 名称
                        TextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { BasicText(stringResource(R.string.group_name)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(contentColor, 16.sp),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = contentColor,
                                unfocusedTextColor = contentColor,
                                disabledTextColor = contentColor,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                cursorColor = accentColor,
                                focusedIndicatorColor = accentColor,
                                unfocusedIndicatorColor = contentColor.copy(alpha = 0.4f),
                                focusedLabelColor = contentColor.copy(alpha = 0.7f),
                                unfocusedLabelColor = contentColor.copy(alpha = 0.7f)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 排序
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicText(
                                text = stringResource(R.string.sort),
                                style = TextStyle(contentColor, 14.sp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(containerColor.copy(alpha = 0.2f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { showSortMenu = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                BasicText(
                                    text = sortOptions.getOrElse(sortIndex) { "" },
                                    style = TextStyle(contentColor, 14.sp)
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                sortOptions.forEachIndexed { i, label ->
                                    DropdownMenuItem(
                                        text = { BasicText(label, style = TextStyle(contentColor, 14.sp)) },
                                        onClick = {
                                            sortIndex = i
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 勾选：允许下拉刷新
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = enableRefresh,
                                onCheckedChange = { enableRefresh = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = accentColor,
                                    uncheckedColor = contentColor.copy(alpha = 0.7f),
                                    checkmarkColor = Color.White
                                )
                            )
                            BasicText(
                                text = stringResource(R.string.allow_drop_down_refresh),
                                style = TextStyle(contentColor, 14.sp)
                            )
                        }
                        // 勾选：只更新已读
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = onlyUpdateRead,
                                onCheckedChange = { onlyUpdateRead = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = accentColor,
                                    uncheckedColor = contentColor.copy(alpha = 0.7f),
                                    checkmarkColor = Color.White
                                )
                            )
                            BasicText(
                                text = stringResource(R.string.only_update_read),
                                style = TextStyle(contentColor, 14.sp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEdit) {
                        Row(
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showDeleteConfirm = true }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicText(
                                text = stringResource(R.string.delete),
                                style = TextStyle(accentColor, 15.sp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // 取消
                    Row(
                        modifier = Modifier
                            .clip(Capsule())
                            .background(containerColor.copy(alpha = 0.2f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onDismiss() }
                            .height(44.dp)
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            text = stringResource(R.string.cancel),
                            style = TextStyle(contentColor, 15.sp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    // 确定
                    Row(
                        modifier = Modifier
                            .clip(Capsule())
                            .background(accentColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                val groupName = name
                                if (groupName.isBlank()) {
                                    appCtx.toastOnUi("分组名称不能为空")
                                    return@clickable
                                }
                                val bookSort = sortIndex - 1
                                val cov = coverPath
                                if (isEdit) {
                                    target!!.let {
                                        it.groupName = groupName
                                        it.cover = cov
                                        it.bookSort = bookSort
                                        it.enableRefresh = enableRefresh
                                        it.onlyUpdateRead = onlyUpdateRead
                                        groupViewModel.upGroup(it) { onDismiss() }
                                    }
                                } else {
                                    groupViewModel.addGroup(
                                        groupName,
                                        bookSort,
                                        enableRefresh,
                                        onlyUpdateRead,
                                        cov
                                    ) {
                                        onDismiss()
                                    }
                                }
                            }
                            .height(44.dp)
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            text = stringResource(R.string.ok),
                            style = TextStyle(Color.White, 15.sp)
                        )
                    }
                }
            }
        }
    }

    // 删除确认
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { BasicText(stringResource(R.string.delete)) },
            text = { BasicText(stringResource(R.string.sure_del)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        target?.let { groupViewModel.delGroup(it) { onDismiss() } }
                    }
                ) { BasicText(stringResource(R.string.delete), style = TextStyle(accentColor)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    BasicText(stringResource(R.string.cancel), style = TextStyle(contentColor))
                }
            }
        )
    }
}
