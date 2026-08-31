package com.qreader.reader.ui.login

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.qreader.reader.R
import com.qreader.reader.constant.EventBus
import com.qreader.reader.data.entities.BaseSource
import com.qreader.reader.data.entities.HttpTTS
import com.qreader.reader.model.ReadAloud
import com.qreader.reader.ui.rss.read.RssJsExtensions
import com.qreader.reader.ui.widget.dialog.BottomWebViewDialog
import com.qreader.reader.utils.FileUtils
import com.qreader.reader.utils.postEvent
import com.qreader.reader.utils.sendToClip
import com.qreader.reader.utils.showDialogFragment
import com.qreader.reader.utils.toastOnUi
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference

@Suppress("unused")
class SourceLoginJsExtensions(
    activity: AppCompatActivity?,
    source: BaseSource?,
    bookType: Int = 0,
    callback: Callback? = null
) : RssJsExtensions(activity, source, bookType) {
    private val callbackRef: WeakReference<Callback> = WeakReference(callback)
    interface Callback {
        fun upUiData(data: Map<String, Any?>?)
        fun reUiView(deltaUp: Boolean = false)
    }

    fun upLoginData(data: Map<String, Any?>?) {
        callbackRef.get()?.upUiData(data)
    }

    @JvmOverloads
    fun reLoginView(deltaUp: Boolean = false) {
        callbackRef.get()?.reUiView(deltaUp)
    }

    fun refreshExplore() {
        callbackRef.get()?.reUiView()
    }

    fun refreshBookInfo() {
        postEvent(EventBus.REFRESH_BOOK_INFO, true)
    }

    fun refreshBookToc() {
        postEvent(EventBus.REFRESH_BOOK_TOC, true)
    }

    fun refreshContent() {
        postEvent(EventBus.REFRESH_BOOK_CONTENT, true)
    }

    fun copyText(text: String) {
        activityRef.get()?.sendToClip(text)
    }

    fun clearTtsCache() {
        if (getSource() !is HttpTTS) return
        val activity = activityRef.get() ?: return
        activity.lifecycleScope.launch(IO) {
            ReadAloud.upReadAloudClass()
            val ttsFolderPath = "${activity.cacheDir.absolutePath}${File.separator}httpTTS${File.separator}"
            FileUtils.listDirsAndFiles(ttsFolderPath)?.forEach {
                FileUtils.delete(it.absolutePath)
            }
            activity.toastOnUi(R.string.clear_cache_success)
        }
    }

    @JvmOverloads
    fun showBrowser(url: String, html: String? = null, preloadJs: String? = null, config: String? = null) {
        val activity = activityRef.get() ?: return
        val source = getSource() ?: return
        activity.showDialogFragment(
            BottomWebViewDialog(
                source.getKey(),
                bookType,
                url,
                html,
                preloadJs,
                config
            )
        )
    }

}