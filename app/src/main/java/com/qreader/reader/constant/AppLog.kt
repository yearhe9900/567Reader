package com.qreader.reader.constant

/**
 * 日志记录 - 已禁用，所有方法为空实现
 */
object AppLog {
    fun put(message: String?, throwable: Throwable? = null, toast: Boolean = false) {
        // no-op
    }

    fun putNotSave(message: String?, throwable: Throwable? = null, toast: Boolean = false) {
        // no-op
    }

    fun putDebug(message: String?, throwable: Throwable? = null) {
        // no-op
    }
}
