package com.espert.reporteciudadanoadmin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.espert.reporteciudadanoadmin.domain.Report
import com.espert.reporteciudadanoadmin.domain.ReportStatus
import com.espert.reporteciudadanoadmin.network.ApiClient
import com.espert.reporteciudadanoadmin.ui.components.StatusBadge
import kotlinx.browser.document
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLImageElement

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportDetailScreen(
    reportId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var report by remember { mutableStateOf<Report?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf<ReportStatus?>(null) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var photoUrls by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(reportId) {
        isLoading = true
        errorMessage = null
        try {
            val loaded = ApiClient.getReport(reportId)
            report = loaded
            selectedStatus = loaded.status
            val keys = ApiClient.getPhotoKeys(reportId)
            val urls = keys.keys.map { key ->
                val filename = key.substringAfterLast("/")
                ApiClient.getPhotoUrl(reportId, filename).url
            }
            photoUrls = urls
        } catch (e: Exception) {
            errorMessage = "Failed to load report: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            errorMessage != null && report == null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onBack) { Text("Back") }
                }
            }

            report != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onBack) {
                        Text("< Back to list")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = report!!.title,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f)
                        )
                        StatusBadge(status = report!!.status)
                    }

                    HorizontalDivider()

                    Column {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = report!!.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column {
                            Text(
                                text = "Latitude",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = report!!.latitude.toString(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Column {
                            Text(
                                text = "Longitude",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = report!!.longitude.toString(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Column {
                            Text(
                                text = "Reported",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = report!!.createdAtFormatted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    HorizontalDivider()

                    Column {
                        Text(
                            text = "Update Status",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = statusDropdownExpanded,
                                onExpandedChange = { statusDropdownExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedStatus?.label ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Status") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded)
                                    },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = statusDropdownExpanded,
                                    onDismissRequest = { statusDropdownExpanded = false }
                                ) {
                                    ReportStatus.entries.forEach { status ->
                                        DropdownMenuItem(
                                            text = { Text(status.label) },
                                            onClick = {
                                                selectedStatus = status
                                                statusDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = {
                                    val status = selectedStatus ?: return@Button
                                    isSaving = true
                                    snackbarMessage = null
                                    scope.launch {
                                        try {
                                            ApiClient.updateStatus(reportId, status.name)
                                            report = report?.copy(status = status)
                                            snackbarMessage = "Status updated to ${status.label}"
                                        } catch (e: Exception) {
                                            snackbarMessage = "Failed to update: ${e.message}"
                                        } finally {
                                            isSaving = false
                                        }
                                    }
                                },
                                enabled = !isSaving && selectedStatus != null
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                                } else {
                                    Text("Save")
                                }
                            }
                        }
                    }

                    if (photoUrls.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            text = "Photos (${photoUrls.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            photoUrls.forEach { url ->
                                PhotoThumbnail(url = url, size = 120)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }

                snackbarMessage?.let { msg ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { snackbarMessage = null }) {
                                Text("Dismiss")
                            }
                        }
                    ) {
                        Text(msg)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoThumbnail(url: String, size: Int) {
    val containerId = remember(url) { "photo-${url.hashCode()}" }

    DisposableEffect(url) {
        val container = document.createElement("div")
        container.id = containerId
        container.setAttribute(
            "style",
            "width:${size}px;height:${size}px;overflow:hidden;border-radius:8px;background:#eee;" +
                "display:inline-block;"
        )
        val img = document.createElement("img") as HTMLImageElement
        img.src = url
        img.setAttribute(
            "style",
            "width:${size}px;height:${size}px;object-fit:cover;"
        )
        img.alt = "Report photo"
        container.appendChild(img)
        document.body?.appendChild(container)

        onDispose {
            container.parentNode?.removeChild(container)
        }
    }

    Spacer(modifier = Modifier.size(size.dp))
}
