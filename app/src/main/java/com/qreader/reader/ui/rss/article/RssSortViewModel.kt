package com.qreader.reader.ui.rss.article

import android.app.Application
import android.content.Intent
import com.qreader.reader.base.BaseViewModel
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.RssReadRecord
import com.qreader.reader.data.entities.RssSource
import com.qreader.reader.help.source.removeSortCache


class RssSortViewModel(application: Application) : BaseViewModel(application) {
    var url: String? = null
    var sortUrl: String? = null
    var rssSource: RssSource? = null
    var order = System.currentTimeMillis()
    val articleStyle get() = rssSource?.articleStyle
    var searchKey: String? = null
    var sourceName: String? = null

    fun initData(intent: Intent, finally: () -> Unit) {
        execute {
            url = intent.getStringExtra("sourceUrl")
            url?.let { url ->
                rssSource = appDb.rssSourceDao.getByKey(url)
                rssSource?.let {
                    sourceName = it.sourceName
                } ?: let {
                    rssSource = RssSource(sourceUrl = url)
                }
            }
            sortUrl = intent.getStringExtra("sortUrl") ?: sortUrl
            searchKey = intent.getStringExtra("key")
        }.onFinally {
            finally()
        }
    }

    fun switchLayout() {
        rssSource?.let {
            if (it.articleStyle < 4) {
                it.articleStyle += 1
            } else {
                it.articleStyle = 0
            }
            execute {
                appDb.rssSourceDao.update(it)
            }
        }
    }

    fun clearArticles() {
        execute {
            url?.let {
                appDb.rssArticleDao.delete(it)
            }
            order = System.currentTimeMillis()
        }.onSuccess {

        }
    }

    fun clearSortCache(onFinally: () -> Unit) {
        execute {
            rssSource?.removeSortCache()
        }.onFinally {
            onFinally.invoke()
        }
    }

    fun getRecords(origin: String? = null): List<RssReadRecord> {
        origin?.let {
            return appDb.rssReadRecordDao.getRecordsByOrigin(it)
        }
        return appDb.rssReadRecordDao.getRecords()
    }

    fun countRecords(origin: String? = null) : Int {
        origin?.let {
            return appDb.rssReadRecordDao.countRecordsByOrigin(it)
        }
        return appDb.rssReadRecordDao.countRecords
    }

    fun deleteAllRecord(origin: String? = null) {
        execute {
            origin?.let {
                appDb.rssReadRecordDao.deleteRecordsByOrigin(it)
                return@execute
            }
            appDb.rssReadRecordDao.deleteAllRecord()
        }
    }

}