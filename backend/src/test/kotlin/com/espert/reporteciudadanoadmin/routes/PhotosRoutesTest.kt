package com.espert.reporteciudadanoadmin.routes

import com.espert.reporteciudadanoadmin.domain.PhotoRepository
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakePhotoRepository(
    private val keys: List<String> = listOf("reports/r1/photo1.jpg", "reports/r1/photo2.jpg"),
    private val url: String = "https://s3.example.com/presigned",
) : PhotoRepository {
    override suspend fun listPhotoKeys(reportId: String): List<String> = keys
    override suspend fun presignedGetUrl(key: String): String = url
}

private fun ApplicationTestBuilder.setup(repo: PhotoRepository) {
    application {
        this.install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        routing { route("/api") { photosRoutes(repo) } }
    }
}

class PhotosRoutesTest {

    @Test
    fun `GET photos returns key list`() = testApplication {
        setup(FakePhotoRepository())
        val response = client.get("/api/reports/r1/photos")
        assertEquals(HttpStatusCode.OK, response.status)
        val keys = Json.parseToJsonElement(response.bodyAsText()).jsonObject["keys"]?.jsonArray
        assertEquals(2, keys?.size)
        assertEquals("reports/r1/photo1.jpg", keys?.get(0)?.jsonPrimitive?.content)
    }

    @Test
    fun `GET photo url returns presigned url`() = testApplication {
        setup(FakePhotoRepository())
        val response = client.get("/api/reports/r1/photos/photo1.jpg/url")
        assertEquals(HttpStatusCode.OK, response.status)
        val url = Json.parseToJsonElement(response.bodyAsText()).jsonObject["url"]?.jsonPrimitive?.content
        assertEquals("https://s3.example.com/presigned", url)
    }
}
