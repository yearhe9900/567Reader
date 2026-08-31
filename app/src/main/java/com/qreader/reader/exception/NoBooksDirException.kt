package com.qreader.reader.exception

import com.qreader.reader.R
import splitties.init.appCtx

class NoBooksDirException: NoStackTraceException(appCtx.getString(R.string.no_books_dir))