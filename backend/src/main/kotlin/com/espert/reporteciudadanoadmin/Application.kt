package com.espert.reporteciudadanoadmin

import com.espert.reporteciudadanoadmin.aws.DynamoDbReportRepository
import com.espert.reporteciudadanoadmin.aws.S3PhotoRepository
import com.espert.reporteciudadanoadmin.routes.photosRoutes
import com.espert.reporteciudadanoadmin.routes.reportsRoutes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toInt() ?: 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Put)
    }
    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK)
        }
        route("/api") {
            reportsRoutes(DynamoDbReportRepository())
            photosRoutes(S3PhotoRepository())
        }
    }
}
