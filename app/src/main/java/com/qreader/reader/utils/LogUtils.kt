package com.qreader.reader.utils

import android.content.Context
import com.qreader.reader.BuildConfig

/**
 * 日志工具 - 已禁用，所有方法为空实现
 */
object LogUtils {
    fun init(context: Context) {
        // no-op
    }

    fun upLevel() {
        // no-op
    }

    fun logDeviceInfo() {
        // no-op
    }

    @JvmStatic
    fun d(tag: String, msg: String) {
        // no-op
    }

    inline fun d(tag: String, lazyMsg: () -> String) {
        // no-op
    }

    @JvmStatic
    fun e(tag: String, msg: String) {
        // no-op
    }

    @JvmStatic
    fun e(tag: String, msg: String, throwable: Throwable?) {
        // no-op
    }
}

fun Throwable.printOnDebug() {
    // no-op
}
