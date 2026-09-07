package com.qreader.reader.ui.book.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qreader.reader.R
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.Book
import com.qreader.reader.data.entities.SearchBook
import com.qreader.reader.data.entities.SearchKeyword
import com.qreader.reader.lib.theme.isDarkTheme
import com.qreader.reader.lib.theme.ThemeStore
import kotlinx.coroutines.flow.distinctUntilChanged
import splitties.init.appCtx

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBookClick: (String, String, String) -> Unit,
    onBookshelfBookClick: (Book) -> Unit,
    onSearchScopeClick: () -> Unit,
    onSourceManageClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSearching by viewModel.isSearchLiveData.observeAsState(false)
    val searchBooks by viewModel.searchBookLiveData.observeAsState(emptyList())

    val context = LocalContext.current
    val isDark = context.isDarkTheme
    val primaryColor = Color(ThemeStore.primaryColor(context))
    val titleBarColor = primaryColor
    val searchFieldColor = Color.White.copy(alpha = if (isDark) 0.14f else 0.25f)
    val textColor = Color(ThemeStore.textColorPrimary(context))
    val focusManager = LocalFocusManager.current

    var query by remember { mutableStateOf("") }
    var showInputHelp by remember { mutableStateOf(true) }
    var historyKeywords by remember { mutableStateOf(emptyList<SearchKeyword>()) }
    var matchedBooks by remember { mutableStateOf(emptyList<Book>()) }

    fun doSearch(key: String) {
        val trimmed = key.trim()
        if (trimmed.isNotEmpty()) {
            viewModel.saveSearchKey(trimmed)
            viewModel.searchKey = ""
            viewModel.search(trimmed)
            showInputHelp = false
            focusManager.clearFocus()
        }
    }

    // 收集搜索历史
    LaunchedEffect(query) {
        if (query.isBlank()) {
            appDb.searchKeywordDao.flowByTime().collect { historyKeywords = it }
        } else {
            appDb.searchKeywordDao.flowSearch(query).collect { historyKeywords = it }
        }
    }

    // 收集书架匹配
    LaunchedEffect(query) {
        if (query.isBlank()) {
            matchedBooks = emptyList()
        } else {
            appDb.bookDao.flowSearch(query).collect { matchedBooks = it }
        }
    }

    // 滚动到底部自动加载更多
    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .distinctUntilChanged()
            .collect { layoutInfo ->
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
                if (lastVisible != null && lastVisible.index >= layoutInfo.totalItemsCount - 3) {
                    if (!isSearching && viewModel.searchKey.isNotEmpty() && viewModel.hasMore) {
                        viewModel.search("")
                    }
                }
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        // ── 标题栏（深色底 + 内嵌胶囊搜索框 + 菜单）──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(titleBarColor)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = textColor,
                    )
                }
                // 胶囊搜索框
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(searchFieldColor)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { newQuery ->
                            query = newQuery
                            viewModel.stop()
                            showInputHelp = true
                        },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(textColor, 15.sp),
                        cursorBrush = SolidColor(textColor),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { doSearch(query) }),
                        decorationBox = { innerTextField ->
                            Box {
                                if (query.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.search_book_key),
                                        style = TextStyle(textColor.copy(alpha = 0.5f), 15.sp)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { doSearch(query) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                // 三点菜单
                IconButton(onClick = onSearchScopeClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = textColor,
                    )
                }
            }
        }

        // 搜索进度条
        AnimatedVisibility(
            visible = isSearching,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = primaryColor
            )
        }

        // 输入帮助（搜索历史 + 书架匹配）或搜索结果
        if (showInputHelp) {
            InputHelpContent(
                historyKeywords = historyKeywords,
                matchedBooks = matchedBooks,
                onHistoryClick = { keyword ->
                    query = keyword
                    doSearch(keyword)
                },
                onHistoryDelete = { keyword -> viewModel.deleteHistory(keyword) },
                onClearHistory = { viewModel.clearHistory() },
                onBookClick = { book -> onBookshelfBookClick(book) },
                modifier = Modifier.weight(1f)
            )
        } else {
            // 搜索结果列表
            Box(modifier = Modifier.weight(1f)) {
                if (searchBooks.isEmpty() && !isSearching) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(
                            items = searchBooks,
                            key = { "${it.name}-${it.author}-${it.bookUrl}" }
                        ) { searchBook ->
                            SearchBookItem(
                                searchBook = searchBook,
                                isInBookshelf = viewModel.isInBookShelf(searchBook),
                                primaryColor = primaryColor,
                                onClick = {
                                    onBookClick(searchBook.name, searchBook.author, searchBook.bookUrl)
                                }
                            )
                        }

                        if (isSearching) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = primaryColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // 开始/停止 FAB
        AnimatedVisibility(
            visible = !showInputHelp && (isSearching || (viewModel.hasMore && viewModel.searchKey.isNotEmpty())),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            androidx.compose.material3.FloatingActionButton(
                onClick = {
                    if (isSearching) viewModel.stop() else viewModel.search("")
                },
                containerColor = primaryColor
            ) {
                Icon(
                    imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (isSearching) stringResource(R.string.stop) else "继续搜索"
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InputHelpContent(
    historyKeywords: List<SearchKeyword>,
    matchedBooks: List<Book>,
    onHistoryClick: (String) -> Unit,
    onHistoryDelete: (SearchKeyword) -> Unit,
    onClearHistory: () -> Unit,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // 书架匹配
        if (matchedBooks.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.bookshelf),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            item {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    matchedBooks.forEach { book ->
                        FilletText(
                            text = book.name,
                            onClick = { onBookClick(book) }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // 搜索历史
        if (historyKeywords.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.searchHistory),
                        style = MaterialTheme.typography.titleSmall
                    )
                    TextButton(onClick = onClearHistory) {
                        Text(
                            text = stringResource(R.string.clear),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            item {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    historyKeywords.forEach { keyword ->
                        FilletText(
                            text = keyword.word,
                            onClick = { onHistoryClick(keyword.word) },
                            onLongClick = { onHistoryDelete(keyword) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilletText(
    text: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun SearchBookItem(
    searchBook: SearchBook,
    isInBookshelf: Boolean,
    primaryColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = searchBook.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isInBookshelf) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "已收藏",
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor,
                        modifier = Modifier
                            .background(primaryColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.author_show, searchBook.author),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val latestChapter = searchBook.latestChapterTitle
            if (!latestChapter.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.lasted_show, latestChapter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val intro = searchBook.intro
            if (!intro.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = searchBook.trimIntro(appCtx),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (searchBook.origins.size > 1) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(primaryColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${searchBook.origins.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
        }
    }
}
