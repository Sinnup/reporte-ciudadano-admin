package com.espert.reporteciudadanoadmin.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ReportStatus {
    SENT,
    SEEN,
    PENDING,
    IN_PROGRESS,
    RESOLVED,
    DISCARDED;

    val label: String
        get() = when (this) {
            SENT -> "Submitted"
            SEEN -> "Seen"
            PENDING -> "Captured"
            IN_PROGRESS -> "In Process"
            RESOLVED -> "Resolved"
            DISCARDED -> "Discarded"
        }
}
