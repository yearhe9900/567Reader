package com.qreader.reader.ui.login

import android.app.Application
import android.content.Intent
import com.script.rhino.runScriptWithContext
import com.qreader.reader.base.BaseViewModel
import com.qreader.reader.constant.BookType
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.BaseSource
import com.qreader.reader.data.entities.Book
import com.qreader.reader.data.entities.BookChapter
import com.qreader.reader.exception.NoStackTraceException
import com.qreader.reader.model.AudioPlay
import com.qreader.reader.model.ReadBook
import com.qreader.reader.model.VideoPlay
import com.qreader.reader.utils.toastOnUi

class SourceLoginViewModel(application: Application) : BaseViewModel(application) {

    var source: BaseSource? = null
    var headerMap: Map<String, String> = emptyMap()
    var book: Book? = null
    var bookType: Int = 0
    var chapter: BookChapter? = null
    var loginInfo: MutableMap<String, String> = mutableMapOf()

    fun initData(intent: Intent, success: (bookSource: BaseSource) -> Unit, error: () -> Unit) {
        execute {
            bookType = intent.getIntExtra("bookType", 0)
            when (bookType) {
                BookType.text -> {
                    source = ReadBook.bookSource
                    book = ReadBook.book?.also {
                        chapter = appDb.bookChapterDao.getChapter(it.bookUrl, ReadBook.durChapterIndex)
                    }
                }

                BookType.audio -> {
                    source = AudioPlay.bookSource
                    book = AudioPlay.book
                    chapter = AudioPlay.durChapter
                }

                BookType.video -> {
                    source = VideoPlay.source
                    book = VideoPlay.book
                    chapter = VideoPlay.chapter
                }

                else -> {
                    val sourceKey = intent.getStringExtra("key")
                        ?: throw NoStackTraceException("没有参数")
                    val type = intent.getStringExtra("type")
                    source = when (type) {
                        "bookSource" ->  appDb.bookSourceDao.getBookSource(sourceKey)
                        "rssSource" -> appDb.rssSourceDao.getByKey(sourceKey)
                        "httpTts" -> appDb.httpTTSDao.get(sourceKey.toLong())
                        else -> null
                    }
                    val bookUrl = intent.getStringExtra("bookUrl")
                    book = bookUrl?.let {
                        appDb.bookDao.getBook(it) ?: appDb.searchBookDao.getSearchBook(it)?.toBook()
                    }
                }
            }
            headerMap = runScriptWithContext {
                source?.getHeaderMap(true) ?: emptyMap()
            }
            source?.let{ loginInfo = it.getLoginInfoMap() }
            source
        }.onSuccess {
            if (it != null) {
                success.invoke(it)
            } else {
                context.toastOnUi("未找到书源")
            }
        }.onError {
            error.invoke()
        }
    }

}