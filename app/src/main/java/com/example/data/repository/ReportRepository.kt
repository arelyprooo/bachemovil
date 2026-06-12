package com.example.data.repository

import com.example.data.local.ReportDao
import com.example.data.model.Report
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ReportRepository(private val reportDao: ReportDao) {

    val allReports: Flow<List<Report>> = reportDao.getAllReports()

    fun getReportsByUser(userId: Int): Flow<List<Report>> {
        return reportDao.getReportsByUser(userId)
    }

    suspend fun insertReport(report: Report): Long {
        return reportDao.insertReport(report)
    }

    suspend fun updateReport(report: Report) {
        reportDao.updateReport(report)
    }

    suspend fun updateStatus(reportId: Int, status: String, adminNote: String) {
        reportDao.updateReportStatus(reportId, status, adminNote)
    }

    suspend fun upvoteReport(reportId: Int) {
        reportDao.upvoteReport(reportId)
    }

    fun getReportById(reportId: Int): Flow<Report?> {
        return reportDao.getReportById(reportId)
    }

    suspend fun populateMockDataIfEmpty() {
        // Double check if database table is empty
        val currentReports = reportDao.getAllReports().first()
        if (currentReports.isEmpty()) {
            val mockReports = listOf(
                Report(
                    userId = 9991,
                    username = "mariag77",
                    description = "Bache muy profundo en el carril central de la avenida. Ya ha ponchado dos llantas el día de hoy.",
                    photoPath = "preset_pothole_1",
                    latitude = 19.432608,
                    longitude = -99.133208,
                    locationDescription = "Av. Madero #10, Centro Histórico, CDMX",
                    status = "Reportado",
                    adminNote = "",
                    rating = 3, // Alta incidencia / severidad alta
                    upvotes = 34
                ),
                Report(
                    userId = 9992,
                    username = "carlos_road",
                    description = "Hoyo peligroso cerca de la coladera. Esquina complicada con mucho tránsito escolar.",
                    photoPath = "preset_pothole_2",
                    latitude = 19.414321,
                    longitude = -99.162232,
                    locationDescription = "Calle Álvaro Obregón / Córdoba, Roma Norte",
                    status = "En Revisión",
                    adminNote = "Se ha asignado brigada de inspección urbana.",
                    rating = 2,
                    upvotes = 18
                ),
                Report(
                    userId = 9993,
                    username = "sofia_an",
                    description = "Vatios baches continuos que parecen cráteres. Es imposible avanzar en línea recta.",
                    photoPath = "preset_pothole_3",
                    latitude = 19.412154,
                    longitude = -99.171923,
                    locationDescription = "Michoacán #34, Col. Condesa",
                    status = "Programado",
                    adminNote = "Agendado para reencarpetamiento el día 25 de Junio.",
                    rating = 3,
                    upvotes = 45
                ),
                Report(
                    userId = 9994,
                    username = "bache_hunter",
                    description = "Bache pequeño pero con filo cortante en el pavimento.",
                    photoPath = "preset_pothole_4",
                    latitude = 19.431211,
                    longitude = -99.191544,
                    locationDescription = "Campos Elíseos #210, Polanco",
                    status = "Reparado",
                    adminNote = "Reparación concluida por la brigada nocturna de bacheo.",
                    rating = 1,
                    upvotes = 5
                ),
                Report(
                    userId = 9995,
                    username = "vecino_coy",
                    description = "Hundimiento notable frente al parque escolar. Urge tapar antes de temporada de lluvias.",
                    photoPath = "preset_pothole_5",
                    latitude = 19.349712,
                    longitude = -99.162021,
                    locationDescription = "Felipe Carrillo Puerto #20, Coyoacán Centro",
                    status = "Reportado",
                    adminNote = "",
                    rating = 2,
                    upvotes = 12
                ),
                Report(
                    userId = 9991,
                    username = "mariag77",
                    description = "Bache gigante que cubre casi la mitad de la calle secundaria.",
                    photoPath = "preset_pothole_2",
                    latitude = 19.435012,
                    longitude = -99.130521,
                    locationDescription = "Calle Donceles #45, Centro",
                    status = "Reparado",
                    adminNote = "Solucionado por pavimentadores gubernamentales.",
                    rating = 3,
                    upvotes = 19
                )
            )

            for (report in mockReports) {
                reportDao.insertReport(report)
            }
        }
    }
}
