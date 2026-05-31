package com.chehartemple.app.data.api

import org.json.JSONObject
import retrofit2.HttpException

object ApiErrorParser {
    fun parse(e: Exception): String {
        return when (e) {
            is HttpException -> {
                try {
                    val body = e.response()?.errorBody()?.string()
                    if (body != null) {
                        val json = JSONObject(body)
                        // Try message field first, then fieldErrors
                        if (json.has("message")) {
                            json.getString("message")
                        } else if (json.has("fieldErrors")) {
                            val fields = json.getJSONObject("fieldErrors")
                            fields.keys().asSequence().map { fields.getString(it) }.joinToString(". ")
                        } else {
                            getDefaultMessage(e.code())
                        }
                    } else {
                        getDefaultMessage(e.code())
                    }
                } catch (_: Exception) {
                    getDefaultMessage(e.code())
                }
            }
            is java.net.ConnectException -> "Unable to connect to server. Please check your internet connection."
            is java.net.SocketTimeoutException -> "Connection timed out. Please try again."
            is java.net.UnknownHostException -> "No internet connection. Please check your network."
            else -> e.message ?: "Something went wrong. Please try again."
        }
    }

    private fun getDefaultMessage(code: Int): String {
        return when (code) {
            401 -> "Invalid email or password. Please try again."
            403 -> "Access denied. Please verify your email first."
            409 -> "This account already exists. Please login or use a different email."
            500 -> "Server error. Please try again later."
            else -> "Something went wrong. Please try again."
        }
    }
}
