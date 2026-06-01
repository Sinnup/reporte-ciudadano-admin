package com.espert.reporteciudadanoadmin.plugins

import com.espert.reporteciudadanoadmin.aws.DynamoDbReportRepository
import com.espert.reporteciudadanoadmin.aws.S3PhotoRepository
import com.espert.reporteciudadanoadmin.routes.photosRoutes
import com.espert.reporteciudadanoadmin.routes.reportsRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK)
        }
        authenticate("cognito") {
            route("/api") {
                reportsRoutes(DynamoDbReportRepository())
                photosRoutes(S3PhotoRepository())
            }
        }
    }
}
