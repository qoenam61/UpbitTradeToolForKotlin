package com.example.upbittrade.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.upbittrade.R
import com.example.upbittrade.model.DefaultViewModel
import com.example.upbittrade.utils.PreferenceUtil


class LoginActivity : AppCompatActivity() {
    companion object {
        const val TAG = "LoginActivity"
        var ACCESS_KEY : String? = null
        var SECRET_KEY : String? = null
    }

    private lateinit var viewModel: DefaultViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        viewModel = ViewModelProvider(this).get(DefaultViewModel::class.java)

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

    override fun onStart() {
        super.onStart()

        viewModel.resultAppKeyListInfo?.observe(this) {Keys ->
            val pref = PreferenceUtil(this)

            val accessKey = findViewById<EditText>(R.id.edit_access_key)
            val secretKey = findViewById<EditText>(R.id.edit_secret_key)
            pref.setString(PreferenceUtil.ACCESS_KEY, accessKey.text.toString())
            pref.setString(PreferenceUtil.SECRET_KEY, secretKey.text.toString())

            val intent = Intent(this, TradePagerActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    private fun onLoginButton() {
        Log.d(TAG, "onLoginButton: ")
        val accessKey = findViewById<EditText>(R.id.edit_access_key)
        val secretKey = findViewById<EditText>(R.id.edit_secret_key)

        if (accessKey.text.isNullOrEmpty() || secretKey.text.isNullOrEmpty()) {
            return
        }

        Log.d(TAG, "onLoginButton: ${accessKey.text} , ${secretKey.text}")

        viewModel.searchAppKeyListInfo.value = arrayOf(accessKey.text.toString(), secretKey.text.toString())

        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(accessKey.windowToken, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
    }
}

