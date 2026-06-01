package com.espert.reporteciudadanoadmin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.espert.reporteciudadanoadmin.domain.ReportStatus

private fun statusColor(status: ReportStatus): Color = when (status) {
    ReportStatus.SENT -> Color(0xFF1565C0)
    ReportStatus.SEEN -> Color(0xFF6A1B9A)
    ReportStatus.PENDING -> Color(0xFFE65100)
    ReportStatus.IN_PROGRESS -> Color(0xFF00838F)
    ReportStatus.RESOLVED -> Color(0xFF2E7D32)
    ReportStatus.DISCARDED -> Color(0xFF616161)
}

@Composable
fun StatusBadge(status: ReportStatus, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = statusColor(status),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status.label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 14.sp
        )
    }
}
