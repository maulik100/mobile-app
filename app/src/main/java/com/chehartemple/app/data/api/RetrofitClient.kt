package com.chehartemple.app.data.api

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import com.chehartemple.app.BuildConfig

object RetrofitClient {
    private const val BASE_URL = BuildConfig.API_BASE_URL

    private var accessToken: String? = null
    var onTokenExpired: (() -> Unit)? = null // callback to trigger logout

    fun setToken(t: String?) { accessToken = t }

    // Adds access token and source header to every request
    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
        accessToken?.let { request.addHeader("Authorization", "Bearer $it") }
        request.addHeader("X-Source", "MOBILE_APP")
        chain.proceed(request.build())
    }

    // Handles 401 → refresh token → retry or logout
    private val tokenRefreshInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code == 401 && !request.url.encodedPath.contains("auth/")) {
            response.close()
            val newToken = refreshToken()
            if (newToken != null) {
                accessToken = newToken
                // Retry original request with new token
                val newRequest = request.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
                chain.proceed(newRequest)
            } else {
                // Both tokens expired → trigger logout
                onTokenExpired?.invoke()
                response
            }
        } else {
            response
        }
    }

    private fun refreshToken(): String? {
        return runBlocking {
            try {
                val refreshToken = TokenManager.getRefreshToken() ?: return@runBlocking null
                val json = JSONObject().put("refreshToken", refreshToken).toString()
                val body = json.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("${BASE_URL}auth/refresh")
                    .post(body)
                    .build()

                val client = OkHttpClient.Builder().build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val newAccessToken = JSONObject(responseBody ?: "").getString("accessToken")
                    // Save new access token
                    val currentRefresh = TokenManager.getRefreshToken() ?: ""
                    TokenManager.saveTokens(newAccessToken, currentRefresh)
                    newAccessToken
                } else {
                    // Refresh token also expired → clear everything
                    TokenManager.clearTokens()
                    null
                }
            } catch (e: Exception) {
                TokenManager.clearTokens()
                null
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(tokenRefreshInterceptor)
        .apply {
            if (BuildConfig.ENABLE_LOGGING) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()

    val api: TempleApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TempleApi::class.java)
}
