package com.example.upbittrade.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.upbittrade.R
import com.example.upbittrade.fragment.LoginFragment
import com.example.upbittrade.model.DefaultViewModel


class LoginActivity : AppCompatActivity() {
    object TAG {
        const val name = "LoginActivity"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val isFragmentContainer = savedInstanceState == null

        val viewModel: DefaultViewModel = ViewModelProvider(this).get(DefaultViewModel::class.java)

        if (isFragmentContainer) {
            val upbitLoginFragment = LoginFragment(viewModel!!)
            val fm = supportFragmentManager
            fm.beginTransaction().add(R.id.fragmentContainer, upbitLoginFragment).commit()
        }
    }
}

