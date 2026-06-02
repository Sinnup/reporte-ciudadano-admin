package com.espert.reporteciudadanoadmin.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ReportStatus {
    SENT,
    SEEN,
    PENDING,
    IN_PROGRESS,
    RESOLVED,
    DISCARDED,
    ;

    val displayLabel: String get() = when (this) {
        SENT -> "Submitted"
        SEEN -> "Seen"
        PENDING -> "Captured"
        IN_PROGRESS -> "In Progress"
        RESOLVED -> "Complete"
        DISCARDED -> "Discarded"
    }
}
