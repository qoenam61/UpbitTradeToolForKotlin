package com.example.upbittrade.api

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

class AppKeyAccountRetrofit(accessKey: String, secretKey: String) : DefaultRetrofit(accessKey, secretKey) {
    override fun getAuthToken(): String {
        val algorithm: Algorithm =
            Algorithm.HMAC256(secretKey)
        val jwtToken: String = JWT.create()
            .withClaim("access_key", accessKey)
            .withClaim("nonce", UUID.randomUUID().toString())
            .sign(algorithm)

        return "Bearer $jwtToken"
    }
}