package com.example.upbittrade.activity

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.upbittrade.R
import com.example.upbittrade.fragment.LoginFragment
import com.example.upbittrade.utils.PreferenceUtil


class LoginActivity : AppCompatActivity() {
    companion object {
        const val TAG = "LoginActivity"
        var ACCESS_KEY : String? = null
        var SECRET_KEY : String? = null
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val preferenceUtil = PreferenceUtil(this)
        ACCESS_KEY = preferenceUtil.getString(PreferenceUtil.ACCESS_KEY, "")
        SECRET_KEY = preferenceUtil.getString(PreferenceUtil.SECRET_KEY, "")


        if (!ACCESS_KEY.isNullOrEmpty()) {
            val accessKey = findViewById<EditText>(R.id.edit_access_key)
            accessKey.setText(ACCESS_KEY)
        }
        if (!SECRET_KEY.isNullOrEmpty()) {
            val secretKey = findViewById<EditText>(R.id.edit_secret_key)
            secretKey.setText(SECRET_KEY)
        }

        val loginButton = findViewById<Button>(R.id.btn_login)
        loginButton?.setOnClickListener {
            onLoginButton()
        }

    }

    private fun onLoginButton() {
        if (ACCESS_KEY.isNullOrEmpty() || SECRET_KEY.isNullOrEmpty()) {
            Log.d(LoginFragment.TAG, "onLoginButton: null")
        }

        val accessKey = findViewById<EditText>(R.id.edit_access_key)
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(accessKey.windowToken, 0)
    }
}

