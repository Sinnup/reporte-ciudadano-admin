package com.espert.reporteciudadanoadmin.routes

import com.espert.reporteciudadanoadmin.domain.ReportRepository
import com.espert.reporteciudadanoadmin.domain.ReportStatus
import com.espert.reporteciudadanoadmin.dto.ReportsListResponse
import com.espert.reporteciudadanoadmin.dto.StatusUpdateRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.reportsRoutes(repo: ReportRepository) {
    route("/reports") {
        get {
            val status = call.request.queryParameters["status"]
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 200) ?: 50
            val lastKey = call.request.queryParameters["lastKey"]

            val (reports, nextKey) = repo.listReports(status, limit, lastKey)
            call.respond(ReportsListResponse(reports, nextKey))
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val report = repo.getReport(id) ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(report)
        }

        put("/{id}/status") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val body = call.receive<StatusUpdateRequest>()
            val newStatus = runCatching { ReportStatus.valueOf(body.status) }.getOrElse {
                return@put call.respond(HttpStatusCode.BadRequest, "Unknown status: ${body.status}")
            }
            val updated = repo.updateReportStatus(id, newStatus)
            if (updated) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
