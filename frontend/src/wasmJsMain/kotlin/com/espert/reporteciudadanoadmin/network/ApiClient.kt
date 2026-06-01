package com.espert.reporteciudadanoadmin.network

import com.espert.reporteciudadanoadmin.domain.Report
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.browser.window
import kotlinx.serialization.json.Json

object ApiClient {

    private val baseUrl: String
        get() = window.location.origin

    private val httpClient = HttpClient(Js) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }
    }

    private fun authHeader(): String = "Bearer ${AuthStore.load().orEmpty()}"

    suspend fun getReports(
        status: String? = null,
        limit: Int = 50,
        lastKey: String? = null
    ): ReportsResponse {
        val params = buildList {
            add("limit=$limit")
            if (status != null) add("status=$status")
            if (lastKey != null) add("lastKey=$lastKey")
        }.joinToString("&")
        val url = "$baseUrl/api/reports?$params"
        return httpClient.get(url) {
            header("Authorization", authHeader())
        }.body()
    }

    suspend fun getReport(id: String): Report {
        return httpClient.get("$baseUrl/api/reports/$id") {
            header("Authorization", authHeader())
        }.body()
    }

    suspend fun updateStatus(id: String, status: String) {
        httpClient.put("$baseUrl/api/reports/$id/status") {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(StatusUpdateRequest(status))
        }
    }

    suspend fun getPhotoKeys(reportId: String): PhotoKeysResponse {
        return httpClient.get("$baseUrl/api/reports/$reportId/photos") {
            header("Authorization", authHeader())
        }.body()
    }

    suspend fun getPhotoUrl(reportId: String, filename: String): PhotoUrlResponse {
        return httpClient.get("$baseUrl/api/reports/$reportId/photos/$filename/url") {
            header("Authorization", authHeader())
        }.body()
    }
}
