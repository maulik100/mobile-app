package com.chehartemple.app

import android.app.Application
import com.chehartemple.app.data.api.ActivityTracker
import com.chehartemple.app.data.api.TokenManager
import com.chehartemple.app.data.notification.EventNotificationWorker

class CheharTempleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenManager.init(this)
        ActivityTracker.init(this)
        EventNotificationWorker.schedule(this)
    }
}
