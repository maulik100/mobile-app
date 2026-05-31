package com.chehartemple.app.data.api

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth_tokens")

object TokenManager {

    private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    private val SESSION_TOKEN_KEY = stringPreferencesKey("session_token")

    private lateinit var context: Context

    fun init(ctx: Context) {
        context = ctx.applicationContext
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String, sessionToken: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = accessToken
            prefs[REFRESH_TOKEN_KEY] = refreshToken
            if (sessionToken != null) prefs[SESSION_TOKEN_KEY] = sessionToken
        }
        RetrofitClient.setToken(accessToken)
    }

    suspend fun getAccessToken(): String? {
        return context.dataStore.data.map { it[ACCESS_TOKEN_KEY] }.first()
    }

    suspend fun getRefreshToken(): String? {
        return context.dataStore.data.map { it[REFRESH_TOKEN_KEY] }.first()
    }

    suspend fun getSessionToken(): String? {
        return context.dataStore.data.map { it[SESSION_TOKEN_KEY] }.first()
    }

    suspend fun clearTokens() {
        context.dataStore.edit { it.clear() }
        RetrofitClient.setToken(null)
    }

    suspend fun hasTokens(): Boolean {
        return getRefreshToken() != null
    }
}
