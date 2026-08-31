package com.qreader.reader.ui.book.manga.entities

import com.qreader.reader.data.entities.BookChapter

data class MangaChapter(
    val chapter: BookChapter,
    val pages: List<BaseMangaPage>,
    val imageCount: Int
)
