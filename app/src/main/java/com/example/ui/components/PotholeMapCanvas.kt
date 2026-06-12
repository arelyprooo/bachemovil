package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Report
import com.example.ui.BacheViewModel
import com.example.ui.PotholeZone
import com.example.ui.theme.*
import kotlin.math.abs

@OptIn(ExperimentalTextApi::class)
@Composable
fun PotholeMapCanvas(
    viewModel: BacheViewModel,
    modifier: Modifier = Modifier,
    onMapTap: (lat: Double, lng: Double) -> Unit = { _, _ -> }
) {
    val reports by viewModel.allReports.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val highIncidenceZones by viewModel.highIncidenceZones.collectAsState()
    val selectedReport by viewModel.selectedReport.collectAsState()

    var zoomLevel by remember { mutableStateOf(1.0f) }

    // Map Center coordinates corresponding to the center of the Canvas view
    val centerLatitude = 19.412154
    val centerLongitude = -99.162232
    val latDegreeSpan = 0.08 / zoomLevel
    val lngDegreeSpan = 0.08 / zoomLevel

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(24.dp))
            .testTag("pothole_canvas_container")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(reports, zoomLevel) {
                    detectTapGestures { offset ->
                        // Reverse engineer offset back to lat/long
                        val canvasWidth = size.width.toFloat()
                        val canvasHeight = size.height.toFloat()

                        val xFraction = offset.x / canvasWidth
                        val yFraction = offset.y / canvasHeight

                        val tappedLng = centerLongitude + (xFraction - 0.5f) * lngDegreeSpan
                        val tappedLat = centerLatitude - (yFraction - 0.5f) * latDegreeSpan

                        // Check if tapped near any report node (< 25 pixels)
                        var matchedReport: Report? = null
                        for (report in reports) {
                            val rX = ((report.longitude - centerLongitude) / lngDegreeSpan + 0.5f) * canvasWidth
                            val rY = (0.5f - (report.latitude - centerLatitude) / latDegreeSpan) * canvasHeight
                            val dist = abs(offset.x - rX) + abs(offset.y - rY)
                            if (dist < 32f) {
                                matchedReport = report
                                break
                            }
                        }

                        if (matchedReport != null) {
                            viewModel.selectReport(matchedReport)
                        } else {
                            onMapTap(tappedLat, tappedLng)
                            viewModel.setLocation(tappedLat, tappedLng)
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            // Helper function to map GPS back to pixels
            fun getPixelOffset(lat: Double, lng: Double): Offset {
                val x = ((lng - centerLongitude) / lngDegreeSpan + 0.5f) * width
                val y = (0.5f - (lat - centerLatitude) / latDegreeSpan) * height
                return Offset(x.toFloat(), y.toFloat())
            }

            // 1. Draw grid / streets representing CDMX
            drawCityGrid(width, height)

            // 2. Draw High Incidence Hotspots (Zones with multiple reports)
            for (zone in highIncidenceZones) {
                if (zone.reportCount >= 2) {
                    val pCenter = getPixelOffset(zone.latitude, zone.longitude)
                    val glowColor = when (zone.zoneColor) {
                        "Crítica" -> StatusReportado.copy(alpha = 0.15f)
                        else -> StatusRevision.copy(alpha = 0.12f)
                    }
                    val strokeColor = when (zone.zoneColor) {
                        "Crítica" -> StatusReportado.copy(alpha = 0.4f)
                        else -> StatusRevision.copy(alpha = 0.3f)
                    }

                    // Pulsing hotspot rings
                    drawCircle(
                        color = glowColor,
                        radius = (zone.reportCount * 30f) * zoomLevel,
                        center = pCenter
                    )
                    drawCircle(
                        color = strokeColor,
                        radius = (zone.reportCount * 30f) * zoomLevel,
                        center = pCenter,
                        style = Stroke(width = 2f)
                    )
                }
            }

            // 3. Draw Report nodes (potholes)
            for (report in reports) {
                val point = getPixelOffset(report.latitude, report.longitude)

                if (point.x in 0f..width && point.y in 0f..height) {
                    val nodeColor = when (report.status) {
                        "Reportado" -> StatusReportado
                        "En Revisión" -> StatusRevision
                        "Programado" -> StatusProgramado
                        "Reparado" -> StatusReparado
                        else -> UrbanSecondary
                    }

                    val isSelected = selectedReport?.id == report.id

                    // Glow behind selected report node
                    if (isSelected) {
                        drawCircle(
                            color = nodeColor.copy(alpha = 0.3f),
                            radius = 24f,
                            center = point
                        )
                        drawCircle(
                            color = nodeColor,
                            radius = 16f,
                            center = point,
                            style = Stroke(width = 3f)
                        )
                    }

                    // Core report dot
                    drawCircle(
                        color = nodeColor,
                        radius = if (isSelected) 10f else 7f,
                        center = point
                    )

                    // Draw alert flag or severity level number
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "!".repeat(report.rating),
                        topLeft = point + Offset(6f, -14f),
                        style = TextStyle(
                            color = nodeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // 4. Draw Current Citizen GPS Marker with radar ring pulse
            val citizenPos = getPixelOffset(currentLocation.first, currentLocation.second)
            if (citizenPos.x in 0f..width && citizenPos.y in 0f..height) {
                // Radar pulse ripple
                drawCircle(
                    color = UrbanPrimary.copy(alpha = 0.15f),
                    radius = 35f,
                    center = citizenPos
                )
                // Outer ring
                drawCircle(
                    color = UrbanPrimary.copy(alpha = 0.7f),
                    radius = 10f,
                    center = citizenPos,
                    style = Stroke(width = 2f)
                )
                // Center solid blue/amber dot
                drawCircle(
                    color = UrbanPrimary,
                    radius = 5f,
                    center = citizenPos
                )
            }
        }

        // Overlay Navigation / Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Zonas Críticas: Red",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = StatusReportado,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )

            IconButton(
                onClick = { if (zoomLevel < 3.0f) zoomLevel += 0.5f },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Zoom In",
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { if (zoomLevel > 0.6f) zoomLevel -= 0.5f },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomOut,
                    contentDescription = "Zoom Out",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Selected Report Indicator HUD top overlay
        selectedReport?.let { report ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), shape = RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Detalle",
                        tint = when (report.status) {
                            "Reportado" -> StatusReportado
                            "En Revisión" -> StatusRevision
                            "Programado" -> StatusProgramado
                            else -> StatusReparado
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Bache: ${report.locationDescription.take(24)}...",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(
                        onClick = { viewModel.selectReport(null) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(20.dp)
                    ) {
                        Text("Ver todo", fontSize = 11.sp, color = UrbanTertiary)
                    }
                }
            }
        }
    }
}

// Draw a beautiful custom CDMX street grid onto the Canvas scope
private fun DrawScope.drawCityGrid(width: Float, height: Float) {
    val roadColor = Color(0xFFECEFF1).copy(alpha = 0.22f)
    val dividerColor = Color(0xFFB0BEC5).copy(alpha = 0.15f)

    // Parallel roads (Verticals)
    val verticalRoads = listOf(
        0.18f * width, 0.40f * width, 0.65f * width, 0.88f * width
    )

    // Parallel roads (Horizontals)
    val horizontalRoads = listOf(
        0.20f * height, 0.45f * height, 0.75f * height
    )

    // Draw main diagonals representing Reformas/Avenues
    val diagonalPath = Path().apply {
        moveTo(0f, 0.15f * height)
        lineTo(width, 0.85f * height)
    }

    // Draw lines
    for (x in verticalRoads) {
        drawLine(
            color = roadColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 32f
        )
        drawLine(
            color = dividerColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 2f
        )
    }

    for (y in horizontalRoads) {
        drawLine(
            color = roadColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 32f
        )
        drawLine(
            color = dividerColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 2f
        )
    }

    drawPath(
        path = diagonalPath,
        color = roadColor,
        style = Stroke(width = 45f)
    )
    drawPath(
        path = diagonalPath,
        color = UrbanPrimary.copy(alpha = 0.2f),
        style = Stroke(width = 4f)
    )
}
