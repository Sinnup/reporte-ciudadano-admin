package com.espert.reporteciudadanoadmin.routes

import com.espert.reporteciudadanoadmin.domain.Report
import com.espert.reporteciudadanoadmin.domain.ReportRepository
import com.espert.reporteciudadanoadmin.domain.ReportStatus
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
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
import kotlin.test.assertNotNull

private val sampleReport = Report(
    id = "r1",
    title = "Pothole on Main St",
    description = "Large pothole",
    latitude = 19.4326,
    longitude = -99.1332,
    status = ReportStatus.SENT,
    createdAt = 1_700_000_000_000L,
)

private class FakeReportRepository(
    private val reports: List<Report> = listOf(sampleReport),
    private val updateResult: Boolean = true,
) : ReportRepository {
    override suspend fun listReports(status: String?, limit: Int, lastKey: String?): Pair<List<Report>, String?> {
        val filtered = if (status != null) reports.filter { it.status.name == status } else reports
        return Pair(filtered, null)
    }

    override suspend fun getReport(id: String): Report? = reports.find { it.id == id }

    override suspend fun updateReportStatus(id: String, status: ReportStatus): Boolean = updateResult
}

private fun ApplicationTestBuilder.setup(repo: ReportRepository) {
    application {
        this.install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        routing { route("/api") { reportsRoutes(repo) } }
    }
}

class ReportsRoutesTest {

    @Test
    fun `GET reports returns 200 with report list`() = testApplication {
        setup(FakeReportRepository())
        val response = client.get("/api/reports")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val reports = body["reports"]?.jsonArray
        assertNotNull(reports)
        assertEquals(1, reports.size)
        assertEquals("r1", reports[0].jsonObject["id"]?.jsonPrimitive?.content)
    }

    @Test
    fun `GET reports filters by status`() = testApplication {
        val repo = FakeReportRepository(
            reports = listOf(
                sampleReport,
                sampleReport.copy(id = "r2", status = ReportStatus.RESOLVED),
            ),
        )
        setup(repo)
        val response = client.get("/api/reports?status=RESOLVED")
        assertEquals(HttpStatusCode.OK, response.status)
        val reports = Json.parseToJsonElement(response.bodyAsText()).jsonObject["reports"]?.jsonArray
        assertEquals(1, reports?.size)
        assertEquals("r2", reports?.get(0)?.jsonObject?.get("id")?.jsonPrimitive?.content)
    }

    @Test
    fun `GET report by id returns 200`() = testApplication {
        setup(FakeReportRepository())
        val response = client.get("/api/reports/r1")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("r1", body["id"]?.jsonPrimitive?.content)
    }

    @Test
    fun `GET report by id returns 404 when not found`() = testApplication {
        setup(FakeReportRepository())
        val response = client.get("/api/reports/unknown")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT status returns 200 on success`() = testApplication {
        setup(FakeReportRepository(updateResult = true))
        val response = client.put("/api/reports/r1/status") {
            contentType(ContentType.Application.Json)
            setBody("""{"status":"SEEN"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT status returns 404 when report not found`() = testApplication {
        setup(FakeReportRepository(updateResult = false))
        val response = client.put("/api/reports/unknown/status") {
            contentType(ContentType.Application.Json)
            setBody("""{"status":"SEEN"}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT status returns 400 for unknown status value`() = testApplication {
        setup(FakeReportRepository())
        val response = client.put("/api/reports/r1/status") {
            contentType(ContentType.Application.Json)
            setBody("""{"status":"INVALID"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
