package com.example.upbittrade.fragment

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
import com.example.upbittrade.model.DefaultViewModel
import com.example.upbittrade.utils.PreferenceUtil

class LoginFragment(private val viewModel: DefaultViewModel): Fragment() {
    object TAG {
        const val name = "LoginFragment"
    }

    object KeyObject {
        var accessKey : String? = null
        var secretKey : String? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferenceUtil = activity?.let { PreferenceUtil(it) }
        KeyObject.accessKey = preferenceUtil?.getString(preferenceUtil.ACCESS_KEY, "")
        KeyObject.secretKey = preferenceUtil?.getString(preferenceUtil.SECRET_KEY, "")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_login, container, false)

        if (KeyObject.accessKey!!.isNotEmpty()) {
            val accessKey = view.findViewById<EditText>(R.id.edit_access_key)
            accessKey.setText(KeyObject.accessKey)
        }
        if (KeyObject.secretKey!!.isNotEmpty()) {
            val secretKey = view.findViewById<EditText>(R.id.edit_secret_key)
            secretKey.setText(KeyObject.secretKey)
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
            Log.d(TAG.toString(), "onStart: ")
        }
    }

    private fun onLoginButton(view: View) {
        val accessKey = view.findViewById<EditText>(R.id.edit_access_key)
        val secretKey = view.findViewById<EditText>(R.id.edit_secret_key)

        KeyObject.accessKey = accessKey.text.toString()
        KeyObject.secretKey = secretKey.text.toString()
        Log.d(LoginActivity.TAG.toString(), "onLoginButton - accessKey: " + KeyObject.accessKey + " secretKey: " + KeyObject.secretKey)

        if (KeyObject.accessKey.isNullOrEmpty() || KeyObject.secretKey.isNullOrEmpty()) {
            Log.d(TAG.toString(), "onLoginButton: null")
        } else {
            viewModel.setKey(KeyObject.accessKey!!, KeyObject.secretKey!!)
            viewModel.setSearchAccountInfo(true)
        }

        val inputMethodManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager!!.hideSoftInputFromWindow(accessKey.windowToken, 0)
    }
}