package com.espert.reporteciudadanoadmin.plugins

import com.espert.reporteciudadanoadmin.aws.DynamoDbReportRepository
import com.espert.reporteciudadanoadmin.aws.S3PhotoRepository
import com.espert.reporteciudadanoadmin.routes.photosRoutes
import com.espert.reporteciudadanoadmin.routes.reportsRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

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
