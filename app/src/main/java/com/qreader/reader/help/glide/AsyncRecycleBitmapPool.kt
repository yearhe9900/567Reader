package com.qreader.reader.help.glide

import android.graphics.Bitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool
import com.qreader.reader.help.globalExecutor

class AsyncRecycleBitmapPool(private val delegate: BitmapPool) : BitmapPool by delegate {

    constructor(maxSize: Int) : this(
        if (maxSize > 0) {
            LruBitmapPool(maxSize.toLong())
        } else {
            BitmapPoolAdapter()
        }
    )

    override fun put(bitmap: Bitmap) {
        globalExecutor.execute {
            delegate.put(bitmap)
        }
    }

}
