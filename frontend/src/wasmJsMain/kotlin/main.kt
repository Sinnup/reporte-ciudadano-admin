import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import com.espert.reporteciudadanoadmin.network.AuthStore
import com.espert.reporteciudadanoadmin.ui.screens.LoginScreen
import com.espert.reporteciudadanoadmin.ui.screens.ReportDetailScreen
import com.espert.reporteciudadanoadmin.ui.screens.ReportsListScreen
import com.espert.reporteciudadanoadmin.ui.screens.ReportsMapScreen
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.js.Js
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ---------------------------------------------------------------------------
// Navigation model
// ---------------------------------------------------------------------------

sealed class Screen {
    object Login : Screen()
    object List : Screen()
    data class Detail(val id: String) : Screen()
    object Map : Screen()
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun getEnvValue(key: String): String = js("(window.__ENV__ && window.__ENV__[key]) ? window.__ENV__[key] : ''").toString()

@Serializable
private data class TokenResponse(
    val access_token: String? = null,
    val id_token: String? = null,
    val error: String? = null
)

private val tokenHttpClient = HttpClient(Js)

/**
 * Exchanges a Cognito authorization code for an access token using
 * the Authorization Code + PKCE flow token endpoint.
 */
private suspend fun exchangeCodeForToken(code: String): String? {
    val cognitoDomain = getEnvValue("COGNITO_DOMAIN")
    val clientId = getEnvValue("COGNITO_CLIENT_ID")
    if (cognitoDomain.isBlank() || clientId.isBlank()) return null

    val redirectUri = window.location.origin + "/"
    val tokenEndpoint = "https://$cognitoDomain/oauth2/token"

    return try {
        val responseBody: String = tokenHttpClient.submitForm(
            url = tokenEndpoint,
            formParameters = Parameters.build {
                append("grant_type", "authorization_code")
                append("client_id", clientId)
                append("redirect_uri", redirectUri)
                append("code", code)
            }
        ).body()
        val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<TokenResponse>(responseBody)
        parsed.access_token
    } catch (e: Exception) {
        null
    }
}

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        MaterialTheme {
            var screen by remember {
                mutableStateOf<Screen>(
                    if (AuthStore.load() != null) Screen.List else Screen.Login
                )
            }

            // Handle OAuth callback: if URL contains ?code=..., exchange it for a token
            LaunchedEffect(Unit) {
                val search = window.location.search
                if (search.contains("code=")) {
                    val code = search
                        .removePrefix("?")
                        .split("&")
                        .firstOrNull { it.startsWith("code=") }
                        ?.removePrefix("code=")
                    if (code != null) {
                        val token = exchangeCodeForToken(code)
                        if (token != null) {
                            AuthStore.save(token)
                            // Clean the authorization code from the URL bar
                            window.history.replaceState(null, "", "/")
                            screen = Screen.List
                        }
                    }
                }
            }

            when (val current = screen) {
                is Screen.Login -> {
                    LoginScreen(modifier = Modifier.fillMaxSize())
                }

                is Screen.List, is Screen.Map -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            when (current) {
                                is Screen.List -> ReportsListScreen(
                                    onReportSelected = { id -> screen = Screen.Detail(id) },
                                    modifier = Modifier.fillMaxSize()
                                )
                                is Screen.Map -> ReportsMapScreen(
                                    onReportSelected = { id -> screen = Screen.Detail(id) },
                                    modifier = Modifier.fillMaxSize()
                                )
                                else -> Unit
                            }
                        }

                        NavigationBar(modifier = Modifier.fillMaxWidth()) {
                            NavigationBarItem(
                                selected = current is Screen.List,
                                onClick = { screen = Screen.List },
                                label = { Text("List") },
                                icon = {}
                            )
                            NavigationBarItem(
                                selected = current is Screen.Map,
                                onClick = { screen = Screen.Map },
                                label = { Text("Map") },
                                icon = {}
                            )
                        }
                    }
                }

                is Screen.Detail -> {
                    ReportDetailScreen(
                        reportId = current.id,
                        onBack = { screen = Screen.List },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
