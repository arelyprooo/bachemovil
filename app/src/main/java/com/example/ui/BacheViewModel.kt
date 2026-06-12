package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Report
import com.example.data.model.User
import com.example.data.repository.ReportRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Sealed class for UI screen routing
sealed class Screen {
    object Login : Screen()
    object Register : Screen()
    object Dashboard : Screen()
}

// Data class for representing alert notifications
data class NotificationAlert(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val message: String,
    val reportId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

// Data class for high incidence zones
data class PotholeZone(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val reportCount: Int,
    val averageSeverity: Float,
    val zoneColor: String // "Alta", "Media", "Normal"
)

class BacheViewModel(
    application: Application,
    private val userRepository: UserRepository,
    private val reportRepository: ReportRepository
) : AndroidViewModel(application) {

    // Current logged-in user state
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Current Screen Routing
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Login)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Public reports list
    val allReports: StateFlow<List<Report>> = reportRepository.allReports
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // User's private report history
    val userReports: StateFlow<List<Report>> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                reportRepository.getReportsByUser(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // In-app Notification Alerts list
    private val _notifications = MutableStateFlow<List<NotificationAlert>>(emptyList())
    val notifications: StateFlow<List<NotificationAlert>> = _notifications.asStateFlow()

    // Selected report details for sheet view
    private val _selectedReport = MutableStateFlow<Report?>(null)
    val selectedReport: StateFlow<Report?> = _selectedReport.asStateFlow()

    // Current simulated or actual location
    private val _currentLocation = MutableStateFlow<Pair<Double, Double>>(Pair(19.432608, -99.133208)) // Default CDMX
    val currentLocation: StateFlow<Pair<Double, Double>> = _currentLocation.asStateFlow()

    // Status / Message fields for feedback
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authSuccess = MutableStateFlow<Boolean>(false)
    val authSuccess: StateFlow<Boolean> = _authSuccess.asStateFlow()

    init {
        // Populates database with structured mockup baches on startup so the app is demonstrably warm
        viewModelScope.launch {
            reportRepository.populateMockDataIfEmpty()
        }

        // Start municipal auto-responder routine to simulate citizen notifications on report updates!
        startMunicipalSimulation()
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        _authError.value = null
    }

    // Set map center or custom coordinates
    fun setLocation(lat: Double, lng: Double) {
        _currentLocation.value = Pair(lat, lng)
    }

    fun selectReport(report: Report?) {
        _selectedReport.value = report
    }

    // Citizen Account Login
    fun login(username: String, emailOrUsername: String) {
        _authError.value = null
        if (username.isBlank() || emailOrUsername.isBlank()) {
            _authError.value = "Por favor, complete todos los campos."
            return
        }

        viewModelScope.launch {
            val user = userRepository.getUserByUsername(username)
            if (user != null && user.passwordHash == emailOrUsername) {
                _currentUser.value = user
                _currentScreen.value = Screen.Dashboard
            } else {
                _authError.value = "Credenciales incorrectas de ingreso."
            }
        }
    }

    // Citizen Account Registration
    fun register(fullName: String, email: String, username: String, passHex: String) {
        _authError.value = null
        if (fullName.isBlank() || email.isBlank() || username.isBlank() || passHex.isBlank()) {
            _authError.value = "Todos los campos de Registro son requeridos."
            return
        }

        viewModelScope.launch {
            val user = User(
                username = username.trim(),
                email = email.trim(),
                fullName = fullName.trim(),
                passwordHash = passHex // Simple credential check
            )
            val result = userRepository.registerUser(user)
            if (result.isSuccess) {
                _authSuccess.value = true
                _authError.value = null
                // Auto login user after registration
                val newlyCreatedUser = userRepository.getUserByUsername(user.username)
                if (newlyCreatedUser != null) {
                    _currentUser.value = newlyCreatedUser
                    _currentScreen.value = Screen.Dashboard
                }
            } else {
                _authError.value = result.exceptionOrNull()?.message ?: "Error al registrar ciudadano."
            }
        }
    }

    fun clearAuthStatus() {
        _authError.value = null
        _authSuccess.value = false
    }

    fun logout() {
        _currentUser.value = null
        _currentScreen.value = Screen.Login
        _notifications.value = emptyList()
    }

    // Submit citizen report
    fun submitReport(
        description: String,
        photoPath: String,
        latitude: Double,
        longitude: Double,
        locationDescription: String,
        rating: Int
    ): Boolean {
        val user = _currentUser.value ?: return false
        if (description.isBlank() || locationDescription.isBlank()) {
            return false
        }

        viewModelScope.launch {
            val report = Report(
                userId = user.id,
                username = user.fullName,
                description = description,
                photoPath = photoPath,
                latitude = latitude,
                longitude = longitude,
                locationDescription = locationDescription,
                rating = rating,
                status = "Reportado",
                upvotes = 1
            )
            reportRepository.insertReport(report)

            // Trigger instantaneous notification confirming receipt of report
            addNotification(
                title = "Reporte Recibido 📥",
                message = "Tu bache en '${locationDescription}' fue registrado. Folio generado exitosamente.",
                reportId = 0
            )
        }
        return true
    }

    fun upvoteReport(reportId: Int) {
        viewModelScope.launch {
            reportRepository.upvoteReport(reportId)
        }
    }

    // Helper notification generator
    fun addNotification(title: String, message: String, reportId: Int) {
        val list = _notifications.value.toMutableList()
        list.add(0, NotificationAlert(title = title, message = message, reportId = reportId))
        _notifications.value = list
    }

    fun markNotificationsRead() {
        val list = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = list
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    // Calculates and groups reports into heatzones by proximity (within ~600m)
    // This allows identifying highest incidence roads instantly
    // Output: PotholeZone
    val highIncidenceZones: StateFlow<List<PotholeZone>> = allReports
        .map { reports ->
            if (reports.isEmpty()) return@map emptyList()

            val zones = mutableListOf<PotholeZone>()
            for (report in reports) {
                // Check if this matches close to an existing zone center
                val matchedZoneIdx = zones.indexOfFirst {
                    calculateDistanceInMeters(it.latitude, it.longitude, report.latitude, report.longitude) < 600.0
                }

                if (matchedZoneIdx != -1) {
                    val existing = zones[matchedZoneIdx]
                    val updatedCount = existing.reportCount + 1
                    val updatedAvgSeverity = (existing.averageSeverity * existing.reportCount + report.rating.toFloat()) / updatedCount
                    val riskLevel = when {
                        updatedCount >= 3 -> "Crítica"
                        updatedCount >= 2 -> "Moderada"
                        else -> "Baja"
                    }
                    zones[matchedZoneIdx] = existing.copy(
                        reportCount = updatedCount,
                        averageSeverity = updatedAvgSeverity,
                        zoneColor = riskLevel
                    )
                } else {
                    // Create a new cluster zone
                    val prefix = when {
                        report.locationDescription.contains(",") -> report.locationDescription.split(",").first()
                        else -> report.locationDescription
                    }
                    zones.add(
                        PotholeZone(
                            name = "Zona $prefix",
                            latitude = report.latitude,
                            longitude = report.longitude,
                            reportCount = 1,
                            averageSeverity = report.rating.toFloat(),
                            zoneColor = "Baja"
                        )
                    )
                }
            }
            zones.sortedByDescending { it.reportCount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Haversine formula to compute metric distance on spheres
    private fun calculateDistanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3 // Earth radius
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2) * sin(deltaLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c
    }

    // Simulated governmental periodic status updater
    // Automatically takes random active non-resolved report and advances status,
    // sending matching Notification Updates to the User!
    private fun startMunicipalSimulation() {
        viewModelScope.launch {
            while (true) {
                delay(40000) // update every 40s to keep preview active but not spamming
                val reports = allReports.value
                val user = currentUser.value
                if (reports.isNotEmpty() && user != null) {
                    // Select a random user report OR general active report
                    val targetReports = reports.filter { it.status != "Reparado" }
                    if (targetReports.isNotEmpty()) {
                        val chosenReport = targetReports.random()
                        val currentStatus = chosenReport.status

                        val (nextStatus, note) = when (currentStatus) {
                            "Reportado" -> Pair("En Revisión", "Brigada de inspección urbana de BacheReport se dirige a evaluar la zona.")
                            "En Revisión" -> Pair("Programado", "Bache validado. Se agendó con el departamento de Pavimentación el parcheado.")
                            "Programado" -> Pair("Reparado", "¡Excelente noticia! Brigada municipal ha concluido los trabajos de bacheo.")
                            else -> Pair("Reparado", "Cuadrilla reporta reparado.")
                        }

                        // Save update back to DB
                        reportRepository.updateStatus(chosenReport.id, nextStatus, note)

                        // If user is owner of the report, notify them!
                        if (chosenReport.userId == user.id) {
                            addNotification(
                                title = "¡Actualización de tu Reporte! 🚧",
                                message = "Tu reporte en '${chosenReport.locationDescription}' cambió a estado: *${nextStatus}*. Nota: $note",
                                reportId = chosenReport.id
                            )
                        } else {
                            // If not the owner, citizen gets notified of general progress in CDMX of report they may have upvoted
                            if (chosenReport.upvotes > 2) {
                                addNotification(
                                    title = "Avance en Zona de Alta Incidencia 📍",
                                    message = "El bache de alta prioridad reportado en '${chosenReport.locationDescription}' ahora está *${nextStatus}*.",
                                    reportId = chosenReport.id
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ViewModel Factory
class ViewModelFactory(
    private val application: Application,
    private val userRepository: UserRepository,
    private val reportRepository: ReportRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BacheViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BacheViewModel(application, userRepository, reportRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
