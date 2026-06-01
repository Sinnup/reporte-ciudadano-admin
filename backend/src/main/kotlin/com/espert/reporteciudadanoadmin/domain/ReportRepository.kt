package com.espert.reporteciudadanoadmin.domain

interface ReportRepository {
    suspend fun listReports(status: String?, limit: Int, lastKey: String?): Pair<List<Report>, String?>
    suspend fun getReport(id: String): Report?
    suspend fun updateReportStatus(id: String, status: ReportStatus): Boolean
}
