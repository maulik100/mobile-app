package com.chehartemple.app.data.api

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ActivityTracker {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun trackScreen(screen: String) {
        track("SCREEN_VIEW", screen, "Viewed $screen")
    }

    fun trackAction(action: String, screen: String, description: String = "") {
        track(action, screen, description.ifEmpty { "$action on $screen" })
    }

    private fun track(action: String, screen: String, description: String) {
        scope.launch {
            try {
                val sessionToken = TokenManager.getSessionToken()
                val location = appContext?.let { LocationHelper.getLocationString(it) }
                RetrofitClient.api.trackActivity(
                    buildMap {
                        put("action", action)
                        put("screen", screen)
                        put("description", description)
                        put("sessionToken", sessionToken ?: "")
                        if (location != null) put("location", location)
                    }
                )
            } catch (_: Exception) { }
        }
    }
}
