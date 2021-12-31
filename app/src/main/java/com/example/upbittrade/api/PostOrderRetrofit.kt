package com.example.upbittrade.api

import android.util.Log
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

class PostOrderRetrofit(accessKey: String, secretKey: String): DefaultRetrofit(accessKey, secretKey) {

    var params: HashMap<String?, String?>? = null

    override fun getAuthToken(): String {
        if (params == null) {
            return ""
        }
        val queryElements: ArrayList<String> = ArrayList()
        for ((key, value) in params!!) {
            queryElements.add("$key=$value")
        }

        val queryString = java.lang.String.join("&", *queryElements.toTypedArray())
        val md = MessageDigest.getInstance("SHA-512")
        md.update(queryString.toByteArray(charset("UTF-8")))

        val queryHash = java.lang.String.format("%0128x", BigInteger(1, md.digest()))

        val algorithm = Algorithm.HMAC256(secretKey)
        val jwtToken = JWT.create()
            .withClaim("access_key", accessKey)
            .withClaim("nonce", UUID.randomUUID().toString())
            .withClaim("query_hash", queryHash)
            .withClaim("query_hash_alg", "SHA512")
            .sign(algorithm)

        return "Bearer $jwtToken"
    }
}