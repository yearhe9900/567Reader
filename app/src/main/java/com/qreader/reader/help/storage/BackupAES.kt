package com.qreader.reader.help.storage

import cn.hutool.crypto.symmetric.AES
import com.qreader.reader.help.config.LocalConfig
import com.qreader.reader.utils.MD5Utils

class BackupAES : AES(
    MD5Utils.md5Encode(LocalConfig.password ?: "").encodeToByteArray(0, 16)
)