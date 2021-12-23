package com.example.upbittrade.fragment

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.example.upbittrade.R
import com.example.upbittrade.activity.LoginActivity
import com.example.upbittrade.activity.LoginActivity.Companion.ACCESS_KEY
import com.example.upbittrade.activity.LoginActivity.Companion.SECRET_KEY
import com.example.upbittrade.model.DefaultViewModel
import com.example.upbittrade.utils.PreferenceUtil

class LoginFragment: Fragment() {
    companion object {
        const val TAG = "LoginFragment"
        lateinit var mainActivity: LoginActivity
        var viewModel: DefaultViewModel? = null
    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        ACCESS_KEY = arguments?.getString(PreferenceUtil.ACCESS_KEY)
//        SECRET_KEY = arguments?.getString(PreferenceUtil.SECRET_KEY)
//    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        mainActivity = activity as LoginActivity
        viewModel = DefaultViewModel(application = activity.application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        if (!ACCESS_KEY.isNullOrEmpty()) {
            val accessKey = view.findViewById<EditText>(R.id.edit_access_key)
            accessKey.setText(ACCESS_KEY)
        }
        if (!SECRET_KEY.isNullOrEmpty()) {
            val secretKey = view.findViewById<EditText>(R.id.edit_secret_key)
            secretKey.setText(SECRET_KEY)
        }

        val loginButton = view.findViewById<Button>(R.id.btn_login)
        loginButton?.setOnClickListener {
            onLoginButton(view)
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        viewModel?.resultAccountsInfo?.observe(viewLifecycleOwner) {
            Log.i(TAG, "onStart: ")
        }
    }

    private fun onLoginButton(view: View) {
        val accessKey = view.findViewById<EditText>(R.id.edit_access_key)
        val secretKey = view.findViewById<EditText>(R.id.edit_secret_key)

        ACCESS_KEY = accessKey.text.toString()
        SECRET_KEY = secretKey.text.toString()
        Log.i(TAG,
            "onLoginButton - accessKey: $ACCESS_KEY secretKey: $SECRET_KEY"
        )

        if (ACCESS_KEY.isNullOrEmpty() || SECRET_KEY.isNullOrEmpty()) {
            Log.d(TAG, "onLoginButton: null")
        } else {
            viewModel?.setKey(ACCESS_KEY!!, SECRET_KEY!!)
            viewModel?.setSearchAccountInfo(true)
        }

        val inputMethodManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(accessKey.windowToken, 0)
    }
}