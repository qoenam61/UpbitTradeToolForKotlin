package com.example.upbittrade.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences

@Suppress("PropertyName")
class PreferenceUtil(context: Context) {
    companion object {
        const val ACCESS_KEY: String = "access_key"
        const val SECRET_KEY: String = "secret_key"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("upbit_trade_key", Context.MODE_PRIVATE)

    fun getString(key: String, defValue: String): String {
        return prefs.getString(key, defValue).toString()
    }

    fun setString(key: String, data: String) {
        prefs.edit().putString(key, data).apply()
    }
}