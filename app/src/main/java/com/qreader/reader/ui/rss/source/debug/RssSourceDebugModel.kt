package com.qreader.reader.ui.rss.source.debug

import android.app.Application
import com.qreader.reader.base.BaseViewModel
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.RssSource
import com.qreader.reader.model.Debug

class RssSourceDebugModel(application: Application) : BaseViewModel(application),
    Debug.Callback {
    var rssSource: RssSource? = null
    private var callback: ((Int, String) -> Unit)? = null
    var listSrc: String? = null
    var contentSrc: String? = null

    fun initData(sourceUrl: String?, finally: () -> Unit) {
        sourceUrl?.let {
            execute {
                rssSource = appDb.rssSourceDao.getByKey(sourceUrl)
            }.onFinally {
                finally()
            }
        }
    }

    fun observe(callback: (Int, String) -> Unit) {
        this.callback = callback
    }

    fun startDebug(key: String, start: (() -> Unit)? = null, error: (() -> Unit)? = null) {
        execute {
            Debug.callback = this@RssSourceDebugModel
            Debug.startDebug(this, rssSource!!, key)
        }.onStart {
            start?.invoke()
        }.onError {
            error?.invoke()
        }
    }

    override fun printLog(state: Int, msg: String) {
        when (state) {
            10 -> listSrc = msg
            20 -> contentSrc = msg
            else -> callback?.invoke(state, msg)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Debug.cancelDebug(true)
    }

}
