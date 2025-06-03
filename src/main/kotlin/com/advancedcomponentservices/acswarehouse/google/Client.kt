package com.advancedcomponentservices.acswarehouse.google

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.bouncycastle.util.io.pem.PemReader
import java.io.StringReader
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Date

class Client(val serviceAccount: HashMap<String, String> =  hashMapOf()) {
    val client: OkHttpClient = OkHttpClient.Builder()
//        .addInterceptor(/* custom logic */)
        .build()
    init {

    }

    fun buildJwt(serviceAccountEmail: String, privateKeyPem: String, scopes: List<String>): String {
        // Parse PEM private key to RSAPrivateKey
        val pemReader = PemReader(StringReader(privateKeyPem))
        val pemObject = pemReader.readPemObject()
        val keySpec = PKCS8EncodedKeySpec(pemObject.content)
        val kf = KeyFactory.getInstance("RSA")
        val privateKey = kf.generatePrivate(keySpec) as RSAPrivateKey

        val now = Instant.now()
        val hourLater = now.plusSeconds(3600)

        val claimsSet = JWTClaimsSet.Builder()
            .issuer(serviceAccountEmail)
            .audience("https://oauth2.googleapis.com/token")
            .issueTime(Date.from(now))
            .expirationTime(Date.from(hourLater))
            .claim("scope", scopes.joinToString(" "))
            .build()

        val signer = RSASSASigner(privateKey)
        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(),
            claimsSet
        )
        signedJWT.sign(signer)

        return signedJWT.serialize()
    }

    fun requestToken() {
        fun makePost(url: String, body: String): String {
            val request = Request.Builder()
                .url(url)
                .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .build()

            client.newCall(request).execute().use {
                return it.body?.string() ?: ""
            }
        }
    }

}