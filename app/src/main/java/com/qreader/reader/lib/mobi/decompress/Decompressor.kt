package com.qreader.reader.lib.mobi.decompress

interface Decompressor {

    fun decompress(data: ByteArray): ByteArray

}
