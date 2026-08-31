package com.qreader.reader.utils.canvasrecorder.pools

import android.graphics.Picture
import com.qreader.reader.utils.objectpool.BaseObjectPool

class PicturePool : BaseObjectPool<Picture>(64) {

    override fun create(): Picture = Picture()

}
