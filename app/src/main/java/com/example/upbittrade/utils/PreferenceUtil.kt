package com.example.upbittrade.utils

import android.content.Context
import android.content.SharedPreferences

@Suppress("PropertyName")
class PreferenceUtil(context: Context) {
    companion object {
        const val ACCESS_KEY: String = "access_key"
        const val SECRET_KEY: String = "secret_key"
        const val SUCCESS_LOGIN: String = "success_login"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("upbit_trade_key", Context.MODE_PRIVATE)

    fun getString(key: String, defValue: String): String {
        return prefs.getString(key, defValue).toString()
    }

    fun setString(key: String, data: String) {
        prefs.edit().putString(key, data).apply()
    }

    fun setBoolean(key: String, data: Boolean) {
        prefs.edit().putBoolean(key, data).apply()
    }

    fun getBoolean(key: String, data: Boolean): Boolean {
        return prefs.getBoolean(key, false)
    }
}