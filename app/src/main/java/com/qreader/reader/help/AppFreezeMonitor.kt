package com.qreader.reader.help

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import com.qreader.reader.help.config.AppConfig

object AppFreezeMonitor {

    private const val TAG = "AppFreezeMonitor"

    val handler by lazy {
        Handler(HandlerThread("AppFreezeMonitor").apply { start() }.looper)
    }

    val screenStatusReceiver by lazy {
        ScreenStatusReceiver()
    }

    private var registeredReceiver = false

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun init(context: Context) {
            if (registeredReceiver) {
                registeredReceiver = false
                context.unregisterReceiver(screenStatusReceiver)
            }
            return
        }

        if (!registeredReceiver) {
            registeredReceiver = true
            context.registerReceiver(screenStatusReceiver, screenStatusReceiver.filter)
        }

        var previous = SystemClock.uptimeMillis()

        val runnable = object : Runnable {
            override fun run() {
                val current = SystemClock.uptimeMillis()
                val elapsed = current - previous
                val extra = elapsed - 3000

                if (extra > 300) {
                }

                previous = current

                    handler.postDelayed(this, 3000)
                }
            }
        }
        handler.postDelayed(runnable, 3000)
    }

    class ScreenStatusReceiver : BroadcastReceiver() {

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
            }
        }
    }

}
