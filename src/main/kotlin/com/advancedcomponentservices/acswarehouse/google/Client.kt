package com.advancedcomponentservices.acswarehouse.google

import com.advancedcomponentservices.acswarehouse.google.models.BulkSheetRange
import com.advancedcomponentservices.acswarehouse.google.models.GSheet
import com.advancedcomponentservices.acswarehouse.google.models.GSpreadSheet
import com.advancedcomponentservices.acswarehouse.google.models.SheetRange
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.bouncycastle.util.io.pem.PemReader
import java.io.StringReader
import java.security.KeyFactory
import java.security.Signature
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64
import java.util.concurrent.TimeUnit


class Client(val serviceAccount: HashMap<String, String> =  hashMapOf()) {
    private var accessToken: String? = null
    private var expiresIn: Double? = null
    private var gSheets: List<GSpreadSheet>? = null
    val applicationFormType = "application/x-www-form-urlencoded".toMediaType()
    val client: OkHttpClient = OkHttpClient.Builder()
//        .addInterceptor(/* custom logic */)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    init {
        requestToken()
        setSpreadSheets()
    }

    fun getAccessToken(): String {
        val bufferSeconds: Double = 300.0
        val expired = Instant.now().epochSecond >= (expiresIn!! - bufferSeconds)
        if (accessToken == null || expired) {
            requestToken()
        }
        return accessToken!!
    }

    fun buildJwt(serviceAccountEmail: String, privateKeyPem: String, scopes: List<String>): String {
        // Parse RSA private key
        val pemReader = PemReader(StringReader(privateKeyPem))
        val pemObject = pemReader.readPemObject()
        val keySpec = PKCS8EncodedKeySpec(pemObject.content)
        val kf = KeyFactory.getInstance("RSA")
        val privateKey = kf.generatePrivate(keySpec) as RSAPrivateKey

        val now = Instant.now()
        val hourLater = now.plusSeconds(3600)

        val gson = Gson()
        val encoder = Base64.getUrlEncoder().withoutPadding()

        val header = mapOf("alg" to "RS256", "typ" to "JWT")
        val payload = mapOf(
            "iss" to serviceAccountEmail,
            "scope" to scopes.joinToString(" "),
            "aud" to "https://oauth2.googleapis.com/token",
            "iat" to now.epochSecond,
            "exp" to hourLater.epochSecond
        )

        val headerJson = gson.toJson(header)
        val payloadJson = gson.toJson(payload)

        val encodedHeader = encoder.encodeToString(headerJson.toByteArray(Charsets.UTF_8))
        val encodedPayload = encoder.encodeToString(payloadJson.toByteArray(Charsets.UTF_8))
        val unsignedToken = "$encodedHeader.$encodedPayload"

        // Sign with SHA256withRSA
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(unsignedToken.toByteArray(Charsets.UTF_8))
        val signatureBytes = signature.sign()
        val encodedSignature = encoder.encodeToString(signatureBytes)

        return "$unsignedToken.$encodedSignature"
    }

    fun requestToken() {
        val jwt = buildJwt(
            serviceAccount["client_email"]!!,
            serviceAccount["private_key"]!!,
            listOf(
                "https://www.googleapis.com/auth/drive",
                "https://www.googleapis.com/auth/drive.readonly",
                "https://www.googleapis.com/auth/drive.file",
                "https://www.googleapis.com/auth/spreadsheets",
                "https://www.googleapis.com/auth/spreadsheets.readonly"
            )
        )
        val requestBody = "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=$jwt"
            .toRequestBody(applicationFormType)

        val request = Request.Builder()
            .url(serviceAccount["token_uri"]!!)
            .post(requestBody)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Token request failed: ${response.code} - ${response.body?.string()}")

            val body = response.body?.string() ?: error("Empty response")
            val json = Gson().fromJson(body, Map::class.java)
            accessToken = json["access_token"] as String
            expiresIn =  json["expires_in"] as Double
        }


    }

    fun setSpreadSheets() {
        val request = Request.Builder()
            .url(Endpoints.DRIVE)
            .get()
            .header("Authorization", "Bearer $accessToken")
            .build()
        val spreadSheets: HashMap<String, String> = HashMap()
        val result = ArrayList<GSpreadSheet>()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Drive request failed: ${response.code} - ${response.body?.string()}")
            val body = response.body?.string() ?: error("Empty response")
            val json = Gson().fromJson(body, Map::class.java)
            val files = json["files"]
            if (files is List<*>) {
                for (file in files) {
                    if (file is Map<*, *>) {
                        val mimeType = file["mimeType"]
                        val name = file["name"]
                        val id = file["id"]
                        if (mimeType.toString().contains("spreadsheet"))
                        {
                            spreadSheets.put(id as String, name as String)
                        }
                    }
                }
            }
        }

        for (spreadSheet in spreadSheets) {
            val id = spreadSheet.key
            val sheetsData = ArrayList<GSheet>()
            val request = Request.Builder()
                .url("${Endpoints.SHEETS_READ}$id")
                .get()
                .header("Authorization", "Bearer $accessToken")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Sheets request failed: ${response.code} - ${response.body?.string()}")
                val body = response.body?.string() ?: error("Empty response")
                val json = Gson().fromJson(body, Map::class.java)
                val sheets =  json["sheets"]
                if (sheets is List<*>) {
                    for (sheet in sheets) {
                        if (sheet is Map<*, *>) {
                            val properties =  sheet["properties"]
                            if (properties is Map<*, *>) {
                                val title = properties["title"] as String
                                val id = (properties["sheetId"] as? Double)?.toInt()
                                val gSheet = GSheet(title, id!!)
                                sheetsData.add(gSheet)
                            }
                        }
                    }
                }
            }
            val resulSheet = GSpreadSheet(spreadSheet.value, spreadSheet.key, sheetsData)
            result.add(resulSheet)
        }
        gSheets = result
    }

    fun getSpreadSheet(spreadSheetName: String): GSpreadSheet? {
        for (spreadsheet in gSheets!!) {
            if (spreadsheet.name.equals(spreadSheetName, ignoreCase = true)) {
                return  spreadsheet
            }
        }
        return null
    }

    fun readSpreadSheet(gSheetId: String, sheetName: String): SheetRange {
        val request = Request.Builder()
            .url("${Endpoints.SHEETS_READ}$gSheetId/values/$sheetName")
            .get()
            .header("Authorization", "Bearer $accessToken")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Read sheet request failed: ${response.code} - ${response.body?.string()}")
            val body = response.body?.string() ?: error("Empty response")
            val json = Gson().fromJson(body, SheetRange::class.java)
            return json
        }
    }

    fun readSpreadSheets(gSheetId: String, sheetNames: Set<String>): BulkSheetRange
    {
        val url = Endpoints.BATCH_SHEETS_READ.replace("{spreadsheetId}", gSheetId)
        val ranges = sheetNames.joinToString(separator = "&ranges=", prefix = "ranges=")
        val request = Request.Builder()
            .url("$url?$ranges")
            .get()
            .header("Authorization", "Bearer $accessToken")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Read Batch Sheet request failed: ${response.code} - ${response.body?.string()}")
            val body = response.body?.string() ?: error("Empty response")
            val json = Gson().fromJson(body, BulkSheetRange::class.java)
            return json
        }
    }

    fun appendToSheet(gSheetId: String, sheetRange: String, rows: List<String>) {

    }

    fun clearFromSheet(gSheetId: String, sheetRange: String) {}

}