package com.espert.reporteciudadanoadmin.plugins

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respondText
import java.net.URI
import java.util.concurrent.TimeUnit

fun Application.configureAuth() {
    val userPoolId = System.getenv("COGNITO_USER_POOL_ID")
        ?: error("COGNITO_USER_POOL_ID env var is required")
    val clientId = System.getenv("COGNITO_CLIENT_ID")
        ?: error("COGNITO_CLIENT_ID env var is required")

    val jwksUrl = "https://cognito-idp.us-east-1.amazonaws.com/$userPoolId/.well-known/jwks.json"

    val jwkProvider = JwkProviderBuilder(URI(jwksUrl).toURL())
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    install(Authentication) {
        jwt("cognito") {
            verifier(jwkProvider) {
                acceptLeeway(10)
            }
            validate { credential ->
                val clientIdClaim = credential.payload.getClaim("client_id").asString()
                if (clientIdClaim == clientId) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respondText("Invalid or missing token", status = HttpStatusCode.Unauthorized)
            }
        }
    }
}
