package com.qreader.reader.ui.main.explore

import android.app.Application
import com.qreader.reader.base.BaseViewModel
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.BookSourcePart
import com.qreader.reader.help.config.SourceConfig
import com.qreader.reader.help.source.SourceHelp

class ExploreViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(bookSource: BookSourcePart) {
        execute {
            val minXh = appDb.bookSourceDao.minOrder
            bookSource.customOrder = minXh - 1
            appDb.bookSourceDao.upOrder(bookSource)
        }
    }

    fun deleteSource(source: BookSourcePart) {
        execute {
            SourceHelp.deleteBookSource(source.bookSourceUrl)
        }
    }

}