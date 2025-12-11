package org.example.project.data.maintenance

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.project.data.model.YearMonth
import kotlin.math.max

private fun today(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

enum class ExtinguisherStatus {
    AVAILABLE,
    IN_WORKSHOP,
    IN_FIELD_SERVICE,
    ON_LOAN,
    OUT_OF_SERVICE
}

enum class MaintenanceType {
    WORKSHOP,
    FIELD
}

enum class MaintenanceStatus {
    REGISTERED,
    CHECK_IN,
    WAITING_PARTS,
    IN_PROGRESS,
    ON_LOAN,
    COMPLETED,
    CANCELLED
}

enum class LoanStatus {
    PREPARING,
    ACTIVE,
    RETURNED,
    CANCELLED
}

enum class StockMovementType {
    INBOUND,
    OUTBOUND,
    ADJUSTMENT
}

enum class PartType {
    VALVE,
    HOSE,
    LIQUID
}

data class QrInfo(
    val code: String,
    val payload: String,
    val lastGeneratedOn: LocalDate = today(),
    val reprintHistory: List<QrReprintLog> = emptyList()
)

data class QrReprintLog(
    val timestamp: LocalDate,
    val requestedBy: String,
    val reason: String
)

data class MaintenanceHistoryEntry(
    val id: String,
    val date: LocalDate,
    val action: String,
    val actor: String,
    val notes: String? = null
)

data class ExtinguisherAsset(
    val id: Int? = null,
    val code: String,
    val serialNumber: String? = null,
    val owner: String,
    val location: String? = null,
    val intakeDate: LocalDate,
    val lastMaintenanceDate: LocalDate? = null,
    val status: ExtinguisherStatus = ExtinguisherStatus.AVAILABLE,
    val qrInfo: QrInfo,
    val history: List<MaintenanceHistoryEntry> = emptyList()
)

data class MaintenanceRecord(
    val id: String,
    val extinguisherCode: String,
    val type: MaintenanceType,
    val status: MaintenanceStatus,
    val registeredOn: LocalDate,
    val expectedDelivery: LocalDate? = null,
    val technician: String? = null,
    val client: String? = null,
    val location: String? = null,
    val partsUsed: List<PartUsage> = emptyList(),
    val loanId: String? = null,
    val notes: String? = null,
    val history: List<MaintenanceHistoryEntry> = emptyList()
)

data class PartUsage(
    val partId: String,
    val quantity: Int
)

data class LoanRecord(
    val id: String,
    val client: String,
    val technician: String,
    val scheduledAt: LocalDate,
    val status: LoanStatus,
    val loanExtinguishers: List<LoanExtinguisher> = emptyList(),
    val originalExtinguishers: List<String> = emptyList(),
    val expectedReturnDate: LocalDate? = null,
    val notes: String? = null,
    val history: List<MaintenanceHistoryEntry> = emptyList()
)

data class LoanExtinguisher(
    val code: String,
    val qrPayload: String,
    val returned: Boolean = false,
    val returnedOn: LocalDate? = null,
    val approxLocation: String? = null
)

data class PartInventoryItem(
    val id: String,
    val name: String,
    val type: PartType,
    val unit: String,
    val stock: Int,
    val minimumStock: Int,
    val monthlyConsumption: List<MonthlyConsumption> = emptyList(),
    val movements: List<StockMovement> = emptyList()
) {
    val isBelowMinimum: Boolean get() = stock <= minimumStock
}

data class StockMovement(
    val id: String,
    val date: LocalDate,
    val quantity: Int,
    val type: StockMovementType,
    val relatedMaintenanceId: String? = null,
    val notes: String? = null
)

data class MonthlyConsumption(
    val month: YearMonth,
    val quantity: Int
)

data class StockAlert(
    val partId: String,
    val partName: String,
    val currentStock: Int,
    val minimumStock: Int,
    val generatedOn: LocalDate = today()
)

data class PurchaseSuggestion(
    val partId: String,
    val partName: String,
    val suggestedQuantity: Int,
    val rationale: String
)

fun generateExtinguisherCode(index: Int): String = "E${max(1, index)}"


data class MonthlyPartReport(
    val partId: String,
    val partName: String,
    val month: YearMonth,
    val consumedQuantity: Int
)
