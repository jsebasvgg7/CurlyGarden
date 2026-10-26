package com.curlygarden.app

import android.app.Application
import com.curlygarden.app.push.ElevenReminder
import com.curlygarden.app.push.PushChannels

class CurlyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        PushChannels.create(this)
        ElevenReminder.reschedule(this)
    }
}
