package com.espert.reporteciudadanoadmin.domain

import kotlinx.serialization.Serializable

/**
 * Formats a Unix-epoch millisecond timestamp into a locale date string
 * using the JS Date API (available in wasmJs via js() interop).
 */
private fun formatDate(epochMs: Long): String =
    js("new Date(epochMs).toLocaleDateString('es-MX', { year: 'numeric', month: 'short', day: 'numeric' })")

@Serializable
data class Report(
    val id: String,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val status: ReportStatus,
    val createdAt: Long,
    val photoKeys: List<String> = emptyList()
) {
    val createdAtFormatted: String
        get() = formatDate(createdAt)
}
