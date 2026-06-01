package com.espert.reporteciudadanoadmin.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.espert.reporteciudadanoadmin.domain.Report
import com.espert.reporteciudadanoadmin.network.ApiClient
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement

// ---------------------------------------------------------------------------
// Leaflet JS interop — Leaflet 1.9.4 is loaded via index.html, so `L` is
// available on the global JS scope. We call it via js("...") expressions to
// keep the wasmJs compilation happy (dynamic is not valid in wasmJs; use js()).
// ---------------------------------------------------------------------------

private fun leafletMap(divId: String): JsAny =
    js("L.map(divId)")

private fun leafletTileLayer(url: String, attribution: String): JsAny =
    js("L.tileLayer(url, { attribution: attribution, maxZoom: 19 })")

private fun leafletLatLng(lat: Double, lng: Double): JsAny =
    js("[lat, lng]")

private fun leafletMarker(latlng: JsAny): JsAny =
    js("L.marker(latlng)")

private fun leafletBounds(): JsAny =
    js("[]")

private fun boundsAdd(bounds: JsAny, latlng: JsAny) {
    js("bounds.push(latlng)")
}

private fun addToMap(obj: JsAny, map: JsAny) {
    js("obj.addTo(map)")
}

private fun bindPopup(marker: JsAny, html: String) {
    js("marker.bindPopup(html)")
}

private fun setView(map: JsAny, lat: Double, lng: Double, zoom: Int) {
    js("map.setView([lat, lng], zoom)")
}

private fun fitBounds(map: JsAny, bounds: JsAny) {
    js("map.fitBounds(bounds, { padding: [40, 40] })")
}

private fun onMarkerClick(marker: JsAny, callback: () -> Unit) {
    js("marker.on('click', function() { callback() })")
}

// ---------------------------------------------------------------------------

private fun buildLeafletMap(
    divId: String,
    reports: List<Report>,
    onReportSelected: (String) -> Unit
) {
    val map = leafletMap(divId)
    val tileLayer = leafletTileLayer(
        "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
        "© OpenStreetMap contributors"
    )
    addToMap(tileLayer, map)

    if (reports.isEmpty()) {
        // Default view — Monterrey, Mexico
        setView(map, 25.6866, -100.3161, 12)
        return
    }

    val bounds = leafletBounds()
    reports.forEach { report ->
        val latlng = leafletLatLng(report.latitude, report.longitude)
        val marker = leafletMarker(latlng)
        val popupHtml = "<b>${report.title}</b><br/>${report.status.label}"
        bindPopup(marker, popupHtml)
        val id = report.id
        onMarkerClick(marker) { onReportSelected(id) }
        addToMap(marker, map)
        boundsAdd(bounds, latlng)
    }

    fitBounds(map, bounds)
}

@Composable
fun ReportsMapScreen(
    onReportSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val mapDivId = "leaflet-map"
    var reports by remember { mutableStateOf<List<Report>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reportsReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            val response = ApiClient.getReports(limit = 500)
            reports = response.reports
            reportsReady = true
        } catch (e: Exception) {
            errorMessage = "Failed to load map data: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            errorMessage != null -> {
                Text(
                    text = errorMessage ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // Mount / refresh the Leaflet div once reports are ready
        if (reportsReady) {
            DisposableEffect(reports) {
                val existingDiv = document.getElementById(mapDivId)
                val mapDiv: HTMLDivElement = if (existingDiv != null) {
                    existingDiv as HTMLDivElement
                } else {
                    val div = document.createElement("div") as HTMLDivElement
                    div.id = mapDivId
                    document.body?.appendChild(div)
                    div
                }

                mapDiv.setAttribute(
                    "style",
                    "position:fixed;top:56px;left:0;width:100%;height:calc(100vh - 56px);z-index:0;"
                )

                buildLeafletMap(mapDivId, reports, onReportSelected)

                onDispose {
                    // Hide the map container when navigating away
                    mapDiv.setAttribute("style", "display:none;")
                }
            }
        }
    }
}
