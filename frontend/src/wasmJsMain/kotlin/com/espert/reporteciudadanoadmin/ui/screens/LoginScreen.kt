package com.espert.reporteciudadanoadmin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.browser.window

private fun encodeURIComponent(value: String): String =
    js("encodeURIComponent(value)").toString()


private fun getEnvValue(key: String): String =
    js("(window.__ENV__ && window.__ENV__[key]) ? window.__ENV__[key] : ''").toString()

@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Reporte Ciudadano",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Admin Dashboard",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = {
                val cognitoDomain = getEnvValue("COGNITO_DOMAIN")
                val clientId = getEnvValue("COGNITO_CLIENT_ID")
                if (cognitoDomain.isBlank() || clientId.isBlank()) {
                    window.alert("Cognito configuration is missing. Contact the administrator.")
                    return@Button
                }
                val redirectUri = window.location.origin + "/"
                val authorizeUrl = "https://$cognitoDomain/oauth2/authorize" +
                    "?response_type=code" +
                    "&client_id=${encodeURIComponent(clientId)}" +
                    "&redirect_uri=${encodeURIComponent(redirectUri)}" +
                    "&scope=phone+openid+email"
                window.location.href = authorizeUrl
            }
        ) {
            Text("Sign in with Cognito")
        }
    }
}
