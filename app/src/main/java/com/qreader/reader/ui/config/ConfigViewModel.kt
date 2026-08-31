package com.qreader.reader.ui.config

import android.app.Application
import android.content.Context
import com.qreader.reader.R
import com.qreader.reader.base.BaseViewModel
import com.qreader.reader.data.appDb
import com.qreader.reader.help.AppWebDav
import com.qreader.reader.help.book.BookHelp
import com.qreader.reader.utils.FileUtils
import com.qreader.reader.utils.restart
import com.qreader.reader.utils.toastOnUi
import kotlinx.coroutines.delay
import splitties.init.appCtx

class ConfigViewModel(application: Application) : BaseViewModel(application) {

    fun upWebDavConfig() {
        execute {
            AppWebDav.upConfig()
        }
    }

    fun clearCache() {
        execute {
            BookHelp.clearCache()
            FileUtils.delete(context.cacheDir.absolutePath)
        }.onSuccess {
            context.toastOnUi(R.string.clear_cache_success)
        }
    }

    fun clearWebViewData() {
        execute {
            FileUtils.delete(context.getDir("webview", Context.MODE_PRIVATE))
            FileUtils.delete(context.getDir("hws_webview", Context.MODE_PRIVATE), true)
            context.toastOnUi(R.string.clear_webview_data_success)
            delay(3000)
            appCtx.restart()
        }
    }

    fun shrinkDatabase() {
        execute {
            appDb.openHelper.writableDatabase.execSQL("VACUUM")
        }.onSuccess {
            context.toastOnUi(R.string.success)
        }
    }

}
