@file:Suppress("unused")

package com.qreader.reader.exception

/**
 * 并发限制
 */
class ConcurrentException(msg: String, val waitTime: Long) : NoStackTraceException(msg)