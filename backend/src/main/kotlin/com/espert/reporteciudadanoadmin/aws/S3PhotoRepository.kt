package com.espert.reporteciudadanoadmin.aws

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.ListObjectsV2Request
import aws.sdk.kotlin.services.s3.presigners.presignGetObject
import com.espert.reporteciudadanoadmin.domain.PhotoRepository
import kotlin.time.Duration.Companion.minutes

private const val BUCKET = "reporte-ciudadano-photos"

class S3PhotoRepository : PhotoRepository {

    private val client = S3Client { region = "us-east-1" }

    override suspend fun listPhotoKeys(reportId: String): List<String> {
        val request = ListObjectsV2Request {
            bucket = BUCKET
            prefix = "reports/$reportId/"
        }
        return client.listObjectsV2(request).contents?.mapNotNull { it.key } ?: emptyList()
    }

    override suspend fun presignedGetUrl(key: String): String {
        val getRequest = GetObjectRequest {
            bucket = BUCKET
            this.key = key
        }
        val presigned = client.presignGetObject(getRequest, duration = 15.minutes)
        return presigned.url.toString()
    }
}
