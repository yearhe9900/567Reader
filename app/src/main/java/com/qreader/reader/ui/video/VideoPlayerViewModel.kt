package com.qreader.reader.ui.video

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.script.rhino.runScriptWithContext
import com.qreader.reader.base.BaseViewModel
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.BookSource
import com.qreader.reader.data.entities.RssSource
import com.qreader.reader.model.VideoPlay
import com.qreader.reader.ui.login.SourceLoginJsExtensions
import com.qreader.reader.utils.toastOnUi

class VideoPlayerViewModel(application: Application) : BaseViewModel(application) {
    val upStarMenuData = MutableLiveData<Boolean>()
    fun removeFromBookshelf(success: (() -> Unit)?) {
        execute {
            VideoPlay.book?.let {
                appDb.bookDao.delete(it)
            }
        }.onSuccess {
            success?.invoke()
        }
    }

    fun addFavorite(success: () -> Unit) {
        execute {
            VideoPlay.rssStar ?: VideoPlay.rssRecord?.toStar()?.let {
                appDb.rssStarDao.insert(it)
                VideoPlay.rssStar = it
            }
        }.onSuccess {
            upStarMenuData.postValue(true)
            success.invoke()
        }
    }

    fun updateFavorite(title: String?, group: String?) {
        execute {
            (VideoPlay.rssStar ?: VideoPlay.rssRecord?.toStar())?.let {
                it.title = title ?: it.title
                it.group = group ?: it.group
                appDb.rssStarDao.update(it)
                VideoPlay.rssStar = it
            }
        }.onSuccess {
            upStarMenuData.postValue(true)
        }
    }

    fun delFavorite() {
        execute {
            VideoPlay.rssStar?.let {
                appDb.rssStarDao.delete(it.origin, it.link)
                VideoPlay.rssRecord = it.toRecord()
                VideoPlay.rssStar = null
            }
        }.onSuccess {
            upStarMenuData.postValue(true)
        }
    }

    fun upSource(success: (() -> Unit)? = null) {
        when (val source = VideoPlay.source) {
            is BookSource -> {
                VideoPlay.source = appDb.bookSourceDao.getBookSource(source.getKey())?.also {
                    success?.invoke()
                }
            }
            is RssSource -> {
                VideoPlay.source = appDb.rssSourceDao.getByKey(source.getKey())
            }
        }
    }

    fun onButtonClick(activity: AppCompatActivity, name: String, click: String) {
        val source = VideoPlay.source ?: return
        val book = VideoPlay.book ?: return
        execute {
            val java = SourceLoginJsExtensions(activity, source)
            runScriptWithContext {
                source.evalJS(click) {
                    put("result", null)
                    put("java", java)
                    put("book", book)
                }
            }
        }.onError {
            context.toastOnUi("$name click error\n${it.localizedMessage}")
        }
    }
}