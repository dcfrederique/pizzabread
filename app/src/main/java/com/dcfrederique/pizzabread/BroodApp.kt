package com.dcfrederique.pizzabread

import android.app.Application
import com.dcfrederique.pizzabread.alarm.Notifications
import com.dcfrederique.pizzabread.data.BakeStore

class BroodApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        BakeStore.init(this)
    }
}
