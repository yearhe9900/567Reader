package com.qreader.reader.ui.book.import.remote

import android.app.Application
import com.qreader.reader.base.BaseViewModel
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.Server

class ServersViewModel(application: Application): BaseViewModel(application) {


    fun delete(server: Server) {
        execute {
            appDb.serverDao.delete(server)
        }
    }

}