package com.qreader.reader.utils

import com.qreader.reader.data.entities.BookChapter

fun BookChapter.internString() {
    title = title.intern()
    bookUrl = bookUrl.intern()
}
