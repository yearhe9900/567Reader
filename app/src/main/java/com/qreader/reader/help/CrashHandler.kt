package com.qreader.reader.help

import android.content.Context

/**
 * 崩溃处理 - 已禁用，所有方法为空实现
 */
class CrashHandler(val context: Context) : Thread.UncaughtExceptionHandler {
    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        // no-op
    }

    companion object {
        fun doHeapDump(force: Boolean = false) {
            // no-op
        }
    }
}
