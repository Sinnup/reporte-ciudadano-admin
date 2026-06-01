package com.espert.reporteciudadanoadmin.dto

import com.espert.reporteciudadanoadmin.domain.Report
import kotlinx.serialization.Serializable

@Serializable
data class StatusUpdateRequest(val status: String)

@Serializable
data class ReportsListResponse(val reports: List<Report>, val nextKey: String?)

@Serializable
data class PhotoKeysResponse(val keys: List<String>)

@Serializable
data class PresignedUrlResponse(val url: String)
