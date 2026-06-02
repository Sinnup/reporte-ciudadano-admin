package com.espert.reporteciudadanoadmin.aws

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ConditionalCheckFailedException
import aws.sdk.kotlin.services.dynamodb.model.GetItemRequest
import aws.sdk.kotlin.services.dynamodb.model.ScanRequest
import aws.sdk.kotlin.services.dynamodb.model.UpdateItemRequest
import com.espert.reporteciudadanoadmin.domain.Report
import com.espert.reporteciudadanoadmin.domain.ReportRepository
import com.espert.reporteciudadanoadmin.domain.ReportStatus
import java.util.Base64

private const val TABLE_NAME = "reporte-ciudadano-reports"

class DynamoDbReportRepository : ReportRepository {

    private val client = DynamoDbClient { region = "us-east-1" }

    override suspend fun listReports(status: String?, limit: Int, lastKey: String?): Pair<List<Report>, String?> {
        val decodedLastKey: Map<String, AttributeValue>? = lastKey?.let { token ->
            val id = String(Base64.getUrlDecoder().decode(token))
            mapOf("id" to AttributeValue.S(id))
        }

        val request = ScanRequest {
            tableName = TABLE_NAME
            if (status != null) {
                filterExpression = "#s = :status"
                expressionAttributeNames = mapOf("#s" to "status")
                expressionAttributeValues = mapOf(":status" to AttributeValue.S(status))
            }
            this.limit = limit
            exclusiveStartKey = decodedLastKey
        }

        val response = client.scan(request)

        val reports = response.items?.mapNotNull { item ->
            runCatching { item.toReport() }.getOrNull()
        } ?: emptyList()

        val nextKey = response.lastEvaluatedKey
            ?.get("id")
            ?.let { it as? AttributeValue.S }
            ?.value
            ?.let { Base64.getUrlEncoder().encodeToString(it.toByteArray()) }

        return Pair(reports, nextKey)
    }

    override suspend fun getReport(id: String): Report? {
        val request = GetItemRequest {
            tableName = TABLE_NAME
            key = mapOf("id" to AttributeValue.S(id))
        }
        val item = client.getItem(request).item?.takeIf { it.isNotEmpty() } ?: return null
        return runCatching { item.toReport() }.getOrNull()
    }

    override suspend fun updateReportStatus(id: String, status: ReportStatus): Boolean {
        return try {
            val request = UpdateItemRequest {
                tableName = TABLE_NAME
                key = mapOf("id" to AttributeValue.S(id))
                updateExpression = "SET #s = :status"
                expressionAttributeNames = mapOf("#s" to "status")
                expressionAttributeValues = mapOf(":status" to AttributeValue.S(status.name))
                conditionExpression = "attribute_exists(id)"
            }
            client.updateItem(request)
            true
        } catch (_: ConditionalCheckFailedException) {
            false
        }
    }
}

internal fun Map<String, AttributeValue>.toReport(): Report = Report(
    id = (this["id"] as AttributeValue.S).value,
    title = (this["title"] as AttributeValue.S).value,
    description = (this["description"] as AttributeValue.S).value,
    latitude = (this["latitude"] as AttributeValue.N).value.toDouble(),
    longitude = (this["longitude"] as AttributeValue.N).value.toDouble(),
    status = ReportStatus.valueOf((this["status"] as AttributeValue.S).value),
    createdAt = (this["createdAt"] as AttributeValue.N).value.toLong(),
)
