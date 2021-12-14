package com.example.upbittrade.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.upbittrade.R
import com.example.upbittrade.fragment.LoginFragment
import com.example.upbittrade.model.DefaultViewModel
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

        val isFragmentContainer = savedInstanceState == null

        val preferenceUtil = PreferenceUtil(this)
        ACCESS_KEY = preferenceUtil.getString(PreferenceUtil.ACCESS_KEY, "")
        SECRET_KEY = preferenceUtil.getString(PreferenceUtil.SECRET_KEY, "")

        if (isFragmentContainer) {
            val upbitLoginFragment = LoginFragment()
            val fm = supportFragmentManager
            fm.beginTransaction().add(R.id.fragmentContainer, upbitLoginFragment).commit()
        }
    }
}

