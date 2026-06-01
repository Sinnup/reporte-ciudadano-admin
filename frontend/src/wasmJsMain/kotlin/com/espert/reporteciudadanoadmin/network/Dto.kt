package com.espert.reporteciudadanoadmin.network

import com.espert.reporteciudadanoadmin.domain.Report
import kotlinx.serialization.Serializable

@Serializable
data class ReportsResponse(
    val reports: List<Report>,
    val nextKey: String? = null
)

@Serializable
data class PhotoKeysResponse(
    val keys: List<String>
)

@Serializable
data class PhotoUrlResponse(
    val url: String
)

@Serializable
data class StatusUpdateRequest(
    val status: String
)
