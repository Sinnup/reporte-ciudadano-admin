package com.espert.reporteciudadanoadmin.domain

import kotlinx.serialization.Serializable

@Serializable
data class Report(
    val id: String,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val status: ReportStatus,
    val createdAt: Long,
    val photoKeys: List<String> = emptyList(),
)
