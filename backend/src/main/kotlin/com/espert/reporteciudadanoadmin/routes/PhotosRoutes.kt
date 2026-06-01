package com.espert.reporteciudadanoadmin.routes

import com.espert.reporteciudadanoadmin.domain.PhotoRepository
import com.espert.reporteciudadanoadmin.dto.PhotoKeysResponse
import com.espert.reporteciudadanoadmin.dto.PresignedUrlResponse
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.photosRoutes(repo: PhotoRepository) {
    route("/reports/{id}/photos") {
        get {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val keys = repo.listPhotoKeys(id)
            call.respond(PhotoKeysResponse(keys))
        }

        get("/{name}/url") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val url = repo.presignedGetUrl("reports/$id/$name")
            call.respond(PresignedUrlResponse(url))
        }
    }
}
