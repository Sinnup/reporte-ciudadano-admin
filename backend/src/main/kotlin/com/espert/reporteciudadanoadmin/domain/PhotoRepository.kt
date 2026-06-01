package com.espert.reporteciudadanoadmin.domain

interface PhotoRepository {
    suspend fun listPhotoKeys(reportId: String): List<String>
    suspend fun presignedGetUrl(key: String): String
}
