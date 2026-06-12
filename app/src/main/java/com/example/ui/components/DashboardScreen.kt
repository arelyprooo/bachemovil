package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import com.example.data.model.Report
import com.example.data.model.User
import com.example.ui.BacheViewModel
import com.example.ui.NotificationAlert
import com.example.ui.PotholeZone
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: BacheViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val reports by viewModel.allReports.collectAsState()
    val userReports by viewModel.userReports.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val selectedReport by viewModel.selectedReport.collectAsState()
    val highIncidenceZones by viewModel.highIncidenceZones.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = Consulta Pública, 1 = Reportar Bache, 2 = Mis Reportes
    var showNotificationsDrawer by remember { mutableStateOf(false) }

    // Floating action button triggers new report switch
    val unreadNotificationsCount = notifications.count { !it.isRead }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "BacheReport",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        currentUser?.let {
                            Text(
                                text = "Hola, ${it.fullName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    // Notification Bell with Badge
                    IconButton(
                        onClick = {
                            showNotificationsDrawer = true
                            viewModel.markNotificationsRead()
                        },
                        modifier = Modifier.testTag("notification_bell_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationsCount > 0) {
                                    Badge(
                                        containerColor = StatusReportado,
                                        contentColor = Color.White
                                    ) {
                                        Text(unreadNotificationsCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Alertas de Baches"
                            )
                        }
                    }

                    // Logout Icon
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("logout_app_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Cerrar Sesión",
                            tint = StatusReportado
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Map, contentDescription = "Mapa público") },
                    label = { Text("Consulta Pública", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_consulta_publica")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.AddLocation, contentDescription = "Reportar Bache") },
                    label = { Text("Reportar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_reportar")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Mis reportes") },
                    label = { Text("Mi Historial", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_historial")
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Main views navigation
            Crossfade(targetState = activeTab, label = "tab_transitions") { tab ->
                when (tab) {
                    0 -> PublicConsultationView(
                        reports = reports,
                        highIncidenceZones = highIncidenceZones,
                        selectedReport = selectedReport,
                        viewModel = viewModel
                    )
                    1 -> ReportBacheFormView(
                        currentLocation = currentLocation,
                        viewModel = viewModel,
                        onReportSubmitted = {
                            activeTab = 0 // return to map showing new report
                        }
                    )
                    2 -> CitizenHistoryView(
                        userReports = userReports,
                        currentUser = currentUser,
                        viewModel = viewModel
                    )
                }
            }

            // Report Details Sheet (Bottom Modal-like backdrop layout)
            AnimatedVisibility(
                visible = selectedReport != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                selectedReport?.let { report ->
                    ReportDetailSheet(
                        report = report,
                        onClose = { viewModel.selectReport(null) },
                        onUpvote = { viewModel.upvoteReport(report.id) }
                    )
                }
            }

            // Notification Center Modal
            AnimatedVisibility(
                visible = showNotificationsDrawer,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                NotificationCenterDrawer(
                    alerts = notifications,
                    onClose = { showNotificationsDrawer = false },
                    onClear = { viewModel.clearNotifications() }
                )
            }
        }
    }
}

// ==========================================
// VIEW 1: PUBLIC CONSULTATION & MAPS
// ==========================================
@Composable
fun PublicConsultationView(
    reports: List<Report>,
    highIncidenceZones: List<PotholeZone>,
    selectedReport: Report?,
    viewModel: BacheViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("Todos") }

    val filteredReports = reports.filter { report ->
        val matchesSearch = report.locationDescription.contains(searchQuery, ignoreCase = true) ||
                report.description.contains(searchQuery, ignoreCase = true)
        val matchesFilter = statusFilter == "Todos" || report.status == statusFilter
        matchesSearch && matchesFilter
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Mapa de Incidencia Vial",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Observa reportes en tiempo real y toca indicadores para consultarlos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Vector Map Canvas Component
            PotholeMapCanvas(
                viewModel = viewModel,
                onMapTap = { lat, lng ->
                    // user clicked map
                }
            )
        }

        // Zones with High Incidence statistics
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Zonas de Alta Incidencia",
                            tint = StatusReportado
                        )
                        Text(
                            text = "Zonas Críticas de Alta Incidencia",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Vías identificadas con mayor concentración de problemas viales abiertos.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (highIncidenceZones.isEmpty()) {
                        Text(
                            text = "Sin incidencias registradas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                    } else {
                        highIncidenceZones.take(3).forEach { zone ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        viewModel.setLocation(zone.latitude, zone.longitude)
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                if (zone.zoneColor == "Crítica") StatusReportado else StatusRevision,
                                                shape = CircleShape
                                            )
                                    )
                                    Column {
                                        Text(
                                            text = zone.name,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${zone.reportCount} reportes activos",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                SuggestChipBadge(
                                    label = zone.zoneColor,
                                    color = if (zone.zoneColor == "Crítica") StatusReportado else StatusRevision
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Feed and Search
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Consulta Pública de Incidentes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar calle, colonia, bache...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_reports_input"),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                // Status filters horizontal scroll
                val statusOptions = listOf("Todos", "Reportado", "En Revisión", "Programado", "Reparado")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp)
                ) {
                    items(statusOptions) { statusOption ->
                        FilterChip(
                            selected = statusFilter == statusOption,
                            onClick = { statusFilter = statusOption },
                            label = { Text(statusOption, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // List of filtered reports
        if (filteredReports.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No se encontraron reportes con estos criterios.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredReports, key = { it.id }) { report ->
                ReportItemCard(
                    report = report,
                    onClick = { viewModel.selectReport(report) },
                    onUpvote = { viewModel.upvoteReport(report.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ==========================================
// VIEW 2: REPORT NEW BACHE FORM
// ==========================================
@Composable
fun ReportBacheFormView(
    currentLocation: Pair<Double, Double>,
    viewModel: BacheViewModel,
    onReportSubmitted: () -> Unit
) {
    var locationDesc by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf(2) } // 1 = Leve, 2 = Medio, 3 = Crítico

    // Authentic presets of potholes for seamless testing in emulators + custom selection
    var selectedPhotoPreset by remember { mutableStateOf("preset_pothole_1") }

    val scrollState = rememberScrollState()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Reportar Nuevo Incidente",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Escribe los detalles e incluye evidencia fotográfica. La geolocalización se registra automáticamente.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        // Geolocation display banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "GPS automático",
                    tint = UrbanPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Geolocalización Automática Activa",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Lat: ${String.format(Locale.US, "%.5f", currentLocation.first)}, Lng: ${String.format(Locale.US, "%.5f", currentLocation.second)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Street ref description input
        OutlinedTextField(
            value = locationDesc,
            onValueChange = { locationDesc = it },
            label = { Text("Dirección Exacta o Referencia") },
            placeholder = { Text("Ej. Calle Juárez #450, Col. Roma, frente a papelería") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("submit_location_input"),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
        )

        // Description note
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Descripción del Peligro") },
            placeholder = { Text("Ej. El bache es muy profundo y se llena de agua cuando llueve, lo que hace que los autos caigan.") },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .testTag("submit_desc_input"),
            shape = RoundedCornerShape(16.dp),
            maxLines = 4,
            leadingIcon = { Icon(Icons.Outlined.Feedback, contentDescription = null) }
        )

        // Severity slider/toggle buttons
        Text(
            text = "Nivel de Severidad / Riesgo Vial",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val levels = listOf(
                Triple(1, "Bajo", StatusReparado),
                Triple(2, "Medio", StatusRevision),
                Triple(3, "Crítico", StatusReportado)
            )

            levels.forEach { (level, name, color) ->
                val selected = severity == level
                Button(
                    onClick = { severity = level },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("severity_button_${level}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) color else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // PHOTOGRAPHIC EVIDENCE SELECTION ASSISTANT
        Text(
            text = "Evidencia Fotográfica",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Selecciona una de las fotografías o referencias para enviarla con el reporte vial:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        // Photographic horizontal presets (Bypass system emulator photo captures)
        val photoPresets = listOf(
            Pair("preset_pothole_1", "Bache profundo asfaltado"),
            Pair("preset_pothole_2", "Coladera rota vial"),
            Pair("preset_pothole_3", "Socavón asfáltico"),
            Pair("preset_pothole_4", "Grietas hundidas de paso"),
            Pair("preset_pothole_5", "Hundimiento crítico de calle")
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(photoPresets) { (presetId, label) ->
                val isSelected = selectedPhotoPreset == presetId
                Box(
                    modifier = Modifier
                        .size(100.dp, 120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { selectedPhotoPreset = presetId }
                        .testTag("preset_img_card_$presetId")
                ) {
                    // Visual placeholder representation
                    ImagePlaceholderGenerator(
                        presetId = presetId,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Selection Glow / Check overlay
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(UrbanPrimary.copy(alpha = 0.25f))
                        )
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = UrbanPrimary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                        )
                    }

                    // Lower title banner
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Asistente fotográfico móvil activo para pruebas",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        // Submission Button
        Button(
            onClick = {
                val success = viewModel.submitReport(
                    description = description,
                    photoPath = selectedPhotoPreset,
                    latitude = currentLocation.first,
                    longitude = currentLocation.second,
                    locationDescription = locationDesc,
                    rating = severity
                )
                if (success) {
                    onReportSubmitted()
                    // Clear form
                    locationDesc = ""
                    description = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(52.dp)
                .testTag("submit_report_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            enabled = locationDesc.isNotBlank() && description.isNotBlank()
        ) {
            Icon(Icons.Default.Send, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Enviar Reporte Ciudadano",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

// ==========================================
// VIEW 3: CITIZEN REPORT HISTORY
// ==========================================
@Composable
fun CitizenHistoryView(
    userReports: List<Report>,
    currentUser: User?,
    viewModel: BacheViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Column {
                        Text(
                            text = currentUser?.fullName ?: "Ciudadano",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Correo: ${currentUser?.email ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Tu Historial Portátil de Reportes",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Revisa el estado de la pavimentación de tus incidentes de baches levantados.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        if (userReports.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ContentPasteSearch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aún no has enviado reportes desde este perfil.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(userReports, key = { it.id }) { report ->
                ReportItemCard(
                    report = report,
                    onClick = { viewModel.selectReport(report) },
                    onUpvote = { viewModel.upvoteReport(report.id) },
                    modifier = Modifier.testTag("user_history_report_${report.id}")
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ==========================================
// COMPONENT: REPORT LIST ITEM CARD
// ==========================================
@Composable
fun ReportItemCard(
    report: Report,
    onClick: () -> Unit,
    onUpvote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (report.status) {
        "Reportado" -> StatusReportado
        "En Revisión" -> StatusRevision
        "Programado" -> StatusProgramado
        "Reparado" -> StatusReparado
        else -> UrbanSecondary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("report_card_${report.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Visual Photo Evidence Thumbnail
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                ImagePlaceholderGenerator(
                    presetId = report.photoPath,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Report brief description details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestChipBadge(label = report.status, color = statusColor)
                    
                    // Priority tag
                    val priority = when (report.rating) {
                        1 -> "Baja"
                        2 -> "Media"
                        else -> "Crítica"
                    }
                    Text(
                        text = "Severidad: $priority",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (report.rating == 3) StatusReportado else MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = report.locationDescription,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = report.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDate(report.timestamp),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    // Citizen confirmations (upvotes)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                CardDefaults.shape
                            )
                            .clickable { onUpvote() }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Confirmar",
                            tint = UrbanPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${report.upvotes} confirmaciones",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: DETAILED SLIDE-UP SHEET
// ==========================================
@Composable
fun ReportDetailSheet(
    report: Report,
    onClose: () -> Unit,
    onUpvote: () -> Unit
) {
    val statusColor = when (report.status) {
        "Reportado" -> StatusReportado
        "En Revisión" -> StatusRevision
        "Programado" -> StatusProgramado
        "Reparado" -> StatusReparado
        else -> UrbanSecondary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_detail_sheet"),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 16.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Drag handle selector
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(40.dp, 4.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), shape = CircleShape)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detalle del Reporte de Bache",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar Panel"
                    )
                }
            }

            // Big Photo Evidence panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                ImagePlaceholderGenerator(
                    presetId = report.photoPath,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Status indicator banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.size(10.dp).background(statusColor, CircleShape))
                    Text(
                        text = "Estado: ${report.status}",
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = formatDate(report.timestamp),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Details body
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = report.locationDescription,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Reportado por: @${report.username}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = report.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            // Official feedback node note (Translating municipal responses)
            if (report.adminNote.isNotBlank()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Respuesta del Departamento de Bacheo",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = report.adminNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f)
                        )
                    }
                }
            } else {
                Text(
                    text = "✓ Tu reporte se encuentra en etapa de validación de folio municipal.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Action verify button
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onUpvote()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.ThumbUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirmar Bache")
                }

                Button(
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: NOTIFICATION CENTER DRAWER
// ==========================================
@Composable
fun NotificationCenterDrawer(
    alerts: List<NotificationAlert>,
    onClose: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("notification_drawer"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = UrbanPrimary
                    )
                    Text(
                        text = "Centro de Alertas de Baches",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar Alertas")
                }
            }

            HorizontalDivider()

            if (alerts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Sin nuevas notificaciones viales.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Los cambios sobre el estado de reparación de tus reportes aparecerán aquí pronto.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onClear) {
                        Text("Limpiar todo", color = StatusReportado, fontWeight = FontWeight.Bold)
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alerts) { alert ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (alert.isRead) MaterialTheme.colorScheme.surface
                                               else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = alert.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (!alert.isRead) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(UrbanPrimary, CircleShape)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = alert.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = formatDate(alert.timestamp),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cerrar Centro de Alertas")
            }
        }
    }
}

// Helper badge chip
@Composable
fun SuggestChipBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// Elegant, beautiful custom placeholder generator drawing high-fidelity vector backgrounds
// in case there are no real baches photos loaded locally yet.
@Composable
fun ImagePlaceholderGenerator(
    presetId: String,
    modifier: Modifier = Modifier
) {
    val color1 = Color(0xFF37474F)
    val color2 = Color(0xFF263238)

    Box(
        modifier = modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(color1, color2)
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(6.dp)
        ) {
            val (icon, title) = when (presetId) {
                "preset_pothole_1" -> Pair(Icons.Default.Warning, "Bache Profundo")
                "preset_pothole_2" -> Pair(Icons.Default.Report, "Coladera Rota")
                "preset_pothole_3" -> Pair(Icons.Default.Dangerous, "Socavón Grave")
                "preset_pothole_4" -> Pair(Icons.Default.Warning, "Grietas Pav.")
                else -> Pair(Icons.Default.ReportProblem, "Hundimiento")
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SafetyOrange.copy(alpha = 0.9f),
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Text(
                text = "EVIDENCIA FOTOGRÁFICA",
                fontSize = 8.sp,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// Simple date parser
fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MMM/yyyy, hh:mm a", Locale("es", "MX"))
    return sdf.format(Date(timestamp))
}
