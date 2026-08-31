package com.qreader.reader.model

import android.content.Context
import com.qreader.reader.constant.IntentAction
import com.qreader.reader.service.DownloadService
import com.qreader.reader.utils.startService

object Download {


    fun start(context: Context, url: String, fileName: String) {
        context.startService<DownloadService> {
            action = IntentAction.start
            putExtra("url", url)
            putExtra("fileName", fileName)
        }
    }

}